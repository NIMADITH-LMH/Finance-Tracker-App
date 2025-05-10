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
import com.example.financetracker.data.TransactionEntity
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import com.example.financetracker.util.PreferencesManager
import com.example.financetracker.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class CategoryAnalysisActivity : AppCompatActivity() {
    private val TAG = "CategoryAnalysisActivity"
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var transactionViewModel: TransactionViewModel
    private var transactions: List<Transaction> = listOf()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_category_analysis)
            
            // Set up the toolbar with NoActionBar theme
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Expense Analysis"
            }
            
            // Setup bottom navigation
            setupBottomNavigation()
            
            preferencesManager = PreferencesManager(this)
            transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
            loadTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing expense analysis", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadTransactions() {
        try {
            // Get expense transactions from Room database
            lifecycleScope.launch {
                try {
                    val transactionEntities = transactionViewModel.getAllTransactions().firstOrNull() ?: emptyList()
                    
                    // Convert to Transaction objects
                    transactions = transactionEntities
                        .filter { it.type == "EXPENSE" }
                        .map { entity ->
                            Transaction(
                                id = entity.id,
                                title = entity.title,
                                amount = entity.amount,
                                category = entity.category,
                                date = entity.date,
                                type = TransactionType.EXPENSE
                            )
                        }
                    
                    // Now that data is loaded, set up the UI
                    setupUI()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching transactions from database", e)
                    Toast.makeText(this@CategoryAnalysisActivity, 
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
            // Calculate total expenses
            val totalExpense = transactions.sumOf { it.amount }
            
            // Get current budget
            val currentBudget = preferencesManager.getMonthlyBudget()
            
            // Set up currency formatter
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
            currencyFormat.currency = Currency.getInstance("LKR")
            
            // Set total expense text
            val totalExpenseTextView = findViewById<TextView>(R.id.totalExpenseTextView)
            totalExpenseTextView?.text = currencyFormat.format(totalExpense)
            
            // Set budget text
            val budgetStatusTextView = findViewById<TextView>(R.id.budgetStatusTextView)
            budgetStatusTextView?.text = "Budget: ${currencyFormat.format(currentBudget)}"
            
            // Update budget progress bar
            val budgetProgressBar = findViewById<ProgressBar>(R.id.budgetProgressBar)
            if (currentBudget > 0) {
                val progress = ((totalExpense / currentBudget) * 100).toInt().coerceIn(0, 100)
                budgetProgressBar?.progress = progress
            } else {
                budgetProgressBar?.progress = 0
            }
            
            // Group transactions by category and calculate percentages
            val categorySums = transactions
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { (_, amount) -> amount }
            
            // Display category breakdown
            setupCategoryBreakdown(categorySums, totalExpense)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up UI", e)
            Toast.makeText(this, "Error displaying expense data", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupCategoryBreakdown(categorySums: List<Pair<String, Double>>, totalExpense: Double) {
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
                val percentage = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0
                
                val (textView, progressBar) = categoryViews[i]
                textView?.text = "$category: $percentage%"
                progressBar?.progress = percentage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up category breakdown", e)
        }
    }
    
    private fun setupBottomNavigation() {
        try {
            val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            if (bottomNavigation == null) {
                Log.e(TAG, "Bottom navigation view not found")
                return
            }
            
            // Set the expense analysis item as selected
            bottomNavigation.selectedItemId = R.id.nav_expense_analysis
            
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
                        // Already on expense analysis screen
                        true
                    }
                    R.id.nav_income_analysis -> {
                        try {
                            startActivity(Intent(this, IncomeAnalysisActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Income Analysis", e)
                            Toast.makeText(this, "Could not open income analysis", Toast.LENGTH_SHORT).show()
                        }
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
                            Log.e(TAG, "Error navigating to settings", e)
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
            R.id.action_income_analysis -> {
                try {
                    val intent = Intent(this, IncomeAnalysisActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to income analysis", e)
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

    override fun onResume() {
        super.onResume()
        // Reload data when activity resumes
        loadTransactions()
    }
} 