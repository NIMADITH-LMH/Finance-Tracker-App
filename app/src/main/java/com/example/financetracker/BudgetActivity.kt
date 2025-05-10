package com.example.financetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.financetracker.data.UserSettings
import com.example.financetracker.databinding.ActivityBudgetBinding
import com.example.financetracker.viewmodel.BudgetViewModel
import com.example.financetracker.viewmodel.TransactionViewModel
import com.example.financetracker.viewmodel.UserSettingsViewModel
import java.text.NumberFormat
import java.util.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class BudgetActivity : AppCompatActivity() {
    private val TAG = "BudgetActivity"
    
    private var _binding: ActivityBudgetBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userSettingsViewModel: UserSettingsViewModel
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var viewModel: BudgetViewModel
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "BudgetActivity onCreate started")
            
            // Initialize view binding
            _binding = ActivityBudgetBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Set up the toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Budget & Income"
            
            // Initialize components WITHOUT bottom navigation
            initializeComponents()
            
            // Setup custom bottom navigation at the very end of initialization
            setupCustomNavigation()
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            showErrorDialog("Error initializing budget screen: ${e.message}")
        }
    }

    private fun initializeComponents() {
        // Initialize ViewModels
        try {
            userSettingsViewModel = ViewModelProvider(this)[UserSettingsViewModel::class.java]
            transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
            viewModel = ViewModelProvider(this)[BudgetViewModel::class.java]
            
            // Set up UI components
            setupBudgetButtons()
            
            // Load user settings
            observeUserSettings()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
            showErrorDialog("Error setting up budget components: ${e.message}")
        }
    }

    private fun setupBudgetButtons() {
        // Set up update buttons
        binding.updateSalaryButton.setOnClickListener {
            saveSalary()
        }
        
        binding.updateBudgetButton.setOnClickListener {
            saveBudget()
        }
    }
    
    private fun observeUserSettings() {
        try {
            Log.d(TAG, "Starting to observe user settings")
            userSettingsViewModel.userSettings.observe(this) { settings ->
                try {
                    if (settings != null) {
                        Log.d(TAG, "Observed UserSettings: $settings")
                        // Populate UI with settings values
                        binding.salaryInput.setText(settings.monthlySalary.toString())
                        binding.budgetLimitInput.setText(settings.monthlyBudget.toString())
                        
                        // Update budget display
                        val formatter = NumberFormat.getCurrencyInstance().apply {
                            currency = Currency.getInstance("LKR")
                        }
                        binding.monthlyBudgetDisplay.text = formatter.format(settings.monthlyBudget)
                    } else {
                        Log.d(TAG, "No UserSettings found, initializing with defaults")
                        // Set default values if no settings exist
                        binding.salaryInput.setText("0.0")
                        binding.budgetLimitInput.setText("0.0")
                        binding.monthlyBudgetDisplay.text = "LKR 0.00"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating UI with settings", e)
                    showToast("Error displaying settings: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing user settings", e)
            showToast("Error loading settings: ${e.message}")
        }
    }
    
    private fun setupCustomNavigation() {
        try {
            // Disable standard menu selection behavior completely
            binding.bottomNavigation.itemIconTintList = null
            binding.bottomNavigation.itemTextColor = null
            binding.bottomNavigation.setOnItemSelectedListener(null)
            binding.bottomNavigation.setOnItemReselectedListener(null)
            
            // Manually highlight the budget item
            val menu = binding.bottomNavigation.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                // Reset all items to unchecked
                item.isChecked = false
            }
            
            // Manually force the budget item to appear selected
            val budgetItem = menu.findItem(R.id.nav_budget)
            if (budgetItem != null) {
                budgetItem.isChecked = true
            }
            
            // Add direct click listeners to the BottomNavigationView using a custom approach
            binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
                if (menuItem.itemId == R.id.nav_budget) {
                    // Already on this screen, do nothing
                    return@setOnItemSelectedListener true
                }
                
                when (menuItem.itemId) {
                    R.id.nav_home -> {
                        navigateTo(MainActivity::class.java)
                    }
                    R.id.nav_expense_analysis -> {
                        navigateTo(CategoryAnalysisActivity::class.java)
                    }
                    R.id.nav_income_analysis -> {
                        navigateTo(IncomeAnalysisActivity::class.java)
                    }
                    R.id.nav_settings -> {
                        navigateTo(SettingsActivity::class.java)
                    }
                }
                
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up custom navigation", e)
            showToast("Error setting up navigation")
        }
    }

    private fun <T> navigateTo(activityClass: Class<T>) {
        try {
            if (isNavigating) return
            isNavigating = true
            
            Log.d(TAG, "Navigating to ${activityClass.simpleName}")
            val intent = Intent(this, activityClass)
            startActivity(intent)
            
            // Optional: Add transition animation
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error to ${activityClass.simpleName}", e)
            showToast("Navigation failed")
            isNavigating = false
        }
    }
    
    private fun saveSalary() {
        try {
            val monthlySalaryText = binding.salaryInput.text.toString()
            if (monthlySalaryText.isEmpty()) {
                showToast("Please enter a salary amount")
                return
            }
            
            val monthlySalary = monthlySalaryText.toDoubleOrNull()
            if (monthlySalary == null) {
                showToast("Please enter a valid number")
                return
            }

            Log.d(TAG, "Saving salary: $monthlySalary")

            // Get current settings first to preserve other values
            userSettingsViewModel.userSettings.value?.let { currentSettings ->
                // Create new settings object with updated salary
                val settings = UserSettings(
                    id = 1, // Always use ID 1 for single user settings
                    monthlySalary = monthlySalary,
                    monthlyBudget = currentSettings.monthlyBudget,
                    currency = currentSettings.currency,
                    budgetAlertsEnabled = currentSettings.budgetAlertsEnabled
                )
                
                userSettingsViewModel.saveUserSettings(settings)
                showToast("Salary updated successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving salary", e)
            showToast("Error updating salary: ${e.message}")
        }
    }
    
    private fun saveBudget() {
        try {
            val budgetLimitText = binding.budgetLimitInput.text.toString()
            if (budgetLimitText.isEmpty()) {
                showToast("Please enter a budget amount")
                return
            }
            
            val budgetLimit = budgetLimitText.toDoubleOrNull()
            if (budgetLimit == null) {
                showToast("Please enter a valid number")
                return
            }

            Log.d(TAG, "Saving budget: $budgetLimit")

            // Get current settings first to preserve other values
            userSettingsViewModel.userSettings.value?.let { currentSettings ->
                // Create new settings object with updated budget
                val settings = UserSettings(
                    id = 1, // Always use ID 1 for single user settings
                    monthlySalary = currentSettings.monthlySalary,
                    monthlyBudget = budgetLimit,
                    currency = currentSettings.currency,
                    budgetAlertsEnabled = currentSettings.budgetAlertsEnabled
                )
                
                userSettingsViewModel.saveUserSettings(settings)
                showToast("Budget updated successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving budget", e)
            showToast("Error updating budget: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorDialog(message: String) {
        try {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ -> 
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
            } else {
                showToast(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog", e)
            showToast(message)
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onBackPressed() {
        Log.d(TAG, "Back button pressed, returning to previous screen")
        finish() // This will properly return to the previous activity in the stack
    }

    override fun onDestroy() {
        Log.d(TAG, "BudgetActivity onDestroy called")
        super.onDestroy()
        _binding = null
    }
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, BudgetActivity::class.java)
        }
    }
} 