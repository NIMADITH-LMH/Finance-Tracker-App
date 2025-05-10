package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import com.example.financetracker.util.PreferencesManager
import com.example.financetracker.viewmodel.TransactionViewModel
import com.example.financetracker.viewmodel.UserSettingsViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class IncomeAnalysisActivity : AppCompatActivity() {
    private val TAG = "IncomeAnalysisActivity"
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var userSettingsViewModel: UserSettingsViewModel
    private var transactions: List<Transaction> = listOf()
    private var monthlySalary: Double = 0.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_income_analysis)
            
            // Set up the toolbar with NoActionBar theme
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Income Analysis"
            }
            
            // Setup bottom navigation
            setupBottomNavigation()
            
            preferencesManager = PreferencesManager(this)
            transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
            userSettingsViewModel = ViewModelProvider(this)[UserSettingsViewModel::class.java]
            
            // Get monthly salary from Room database
            loadUserSettings()
            
            loadTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing income analysis", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadUserSettings() {
        try {
            Log.d(TAG, "Loading user settings from database")
            userSettingsViewModel.userSettings.observe(this) { settings ->
                if (settings != null) {
                    Log.d(TAG, "User settings loaded: $settings")
                    monthlySalary = settings.monthlySalary
                    setupUI() // Update UI when settings change
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user settings", e)
            // Fallback to preferences manager if Room DB fails
            monthlySalary = preferencesManager.getMonthlySalary()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data when activity resumes
        loadUserSettings() // Load settings from database
        loadTransactions()
    }
    
    private fun setupBottomNavigation() {
        try {
            val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            if (bottomNavigation == null) {
                Log.e(TAG, "Bottom navigation view not found")
                return
            }
            
            // Set the income analysis item as selected
            bottomNavigation.selectedItemId = R.id.nav_income_analysis
            
            // Set up navigation item selection listener
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        try {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Main", e)
                            Toast.makeText(this, "Could not navigate to home", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.nav_expense_analysis -> {
                        try {
                            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Expense Analysis", e)
                            Toast.makeText(this, "Could not open expense analysis", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.nav_income_analysis -> {
                        // Already on income analysis screen
                        true
                    }
                    R.id.nav_budget -> {
                        try {
                            startActivity(Intent(this, BudgetActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Budget", e)
                            Toast.makeText(this, "Could not open budget settings", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.nav_settings -> {
                        try {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Settings", e)
                            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation", e)
        }
    }
    
    private fun loadTransactions() {
        try {
            // Get income transactions from Room database
            lifecycleScope.launch {
                try {
                    val transactionEntities = transactionViewModel.getAllTransactions().firstOrNull() ?: emptyList()
                    
                    // Convert to Transaction objects
                    transactions = transactionEntities
                        .filter { it.type == "INCOME" }
                        .map { entity ->
                            Transaction(
                                id = entity.id,
                                title = entity.title,
                                amount = entity.amount,
                                category = entity.category,
                                date = entity.date,
                                type = TransactionType.INCOME
                            )
                        }
                    
                    // Now that data is loaded, set up the UI
                    setupUI()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching transactions from database", e)
                    Toast.makeText(this@IncomeAnalysisActivity, 
                        "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions", e)
            Toast.makeText(this, "Error loading transactions", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupUI() {
        try {
            // Calculate total income
            val totalIncome = transactions.sumOf { it.amount }
            
            // Set total income text
            val totalIncomeTextView = findViewById<TextView>(R.id.totalIncomeTextView)
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
            currencyFormat.currency = Currency.getInstance("LKR")
            totalIncomeTextView?.text = currencyFormat.format(totalIncome)
            
            // Set monthly salary text
            val monthlySalaryTextView = findViewById<TextView>(R.id.monthlySalaryTextView)
            monthlySalaryTextView?.text = "Monthly Salary: ${currencyFormat.format(monthlySalary)}"
            
            // Group transactions by category and calculate percentages
            val categorySums = transactions
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { (_, amount) -> amount }
            
            // Display category breakdown
            setupCategoryBreakdown(categorySums, totalIncome)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up UI", e)
            Toast.makeText(this, "Error displaying income data", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupCategoryBreakdown(categorySums: List<Pair<String, Double>>, totalIncome: Double) {
        try {
            // You can have up to 5 category slots in the UI, so we'll use the top 5
            val categoryViews = listOf(
                findViewById<TextView>(R.id.category1TextView) to findViewById<ProgressBar>(R.id.category1ProgressBar),
                findViewById<TextView>(R.id.category2TextView) to findViewById<ProgressBar>(R.id.category2ProgressBar),
                findViewById<TextView>(R.id.category3TextView) to findViewById<ProgressBar>(R.id.category3ProgressBar),
                findViewById<TextView>(R.id.category4TextView) to findViewById<ProgressBar>(R.id.category4ProgressBar),
                findViewById<TextView>(R.id.category5TextView) to findViewById<ProgressBar>(R.id.category5ProgressBar)
            )
            
            // Clear all views first
            for ((textView, progressBar) in categoryViews) {
                textView?.text = ""
                progressBar?.progress = 0
            }
            
            // Fill in data for available categories
            for (i in categorySums.indices.take(5)) {
                val (category, amount) = categorySums[i]
                val percentage = if (totalIncome > 0) (amount / totalIncome * 100).toInt() else 0
                
                val (textView, progressBar) = categoryViews[i]
                textView?.text = "$category: $percentage%"
                progressBar?.progress = percentage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up category breakdown", e)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.standard_menu, menu)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu", e)
        }
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                try {
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling up navigation", e)
                }
                true
            }
            R.id.action_home -> {
                try {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to home", e)
                }
                true
            }
            R.id.action_expense_analysis -> {
                try {
                    val intent = Intent(this, CategoryAnalysisActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to expense analysis", e)
                }
                true
            }
            R.id.action_settings -> {
                try {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to settings", e)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 