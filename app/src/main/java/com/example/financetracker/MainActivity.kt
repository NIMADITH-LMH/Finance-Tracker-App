package com.example.financetracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.widget.Toolbar
import com.example.financetracker.adapter.TransactionAdapter
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import com.example.financetracker.util.NotificationHelper
import com.example.financetracker.util.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.financetracker.data.DataMigration
import com.example.financetracker.data.FinanceDatabase
import com.example.financetracker.viewmodel.TransactionViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.financetracker.data.TransactionEntity
import com.example.financetracker.databinding.ActivityMainBinding
import com.example.financetracker.viewmodel.UserSettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter

class MainActivity : AppCompatActivity(), TransactionAdapter.TransactionClickListener {
    private val TAG = "MainActivity"
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var userSettingsViewModel: UserSettingsViewModel
    
    // Budget views
    private lateinit var budgetAmount: TextView
    private lateinit var budgetProgress: ProgressBar
    private lateinit var remainingBalance: TextView
    
    // Income views
    private lateinit var monthlySalaryAmount: TextView
    private lateinit var additionalIncomeAmount: TextView
    private lateinit var totalMoneyToSpend: TextView
    
    private var monthlyBudget: Double = 0.0
    private var currentSpending: Double = 0.0
    private var monthlySalary: Double = 0.0
    private var additionalIncome: Double = 0.0

    private val defaultCategories = listOf(
        "Food", "Transport", "Bills", "Entertainment", "Shopping", 
        "Health", "Education", "Salary", "Investment", "Other"
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showPermissionRationale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "Starting MainActivity onCreate")
            super.onCreate(savedInstanceState)
            
            // Initialize view binding first
            try {
                Log.d(TAG, "Attempting to initialize view binding")
                binding = ActivityMainBinding.inflate(layoutInflater)
                val rootView = binding.root
                setContentView(rootView)
                Log.d(TAG, "View binding initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize view binding: ${e.javaClass.simpleName}", e)
                Toast.makeText(this, "Error initializing app layout: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Initialize core components with proper error handling
            try {
                Log.d(TAG, "Initializing core components")
                preferencesManager = PreferencesManager(this)
                notificationHelper = NotificationHelper(this)
                
                Log.d(TAG, "Initializing ViewModels")
                transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
                userSettingsViewModel = ViewModelProvider(this)[UserSettingsViewModel::class.java]
                
                Log.d(TAG, "Setting up toolbar")
                val toolbar = binding.toolbar
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                
                Log.d(TAG, "Core components initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize core components: ${e.javaClass.simpleName}", e)
                showErrorAndFinish("Failed to initialize app components: ${e.message}")
                return
            }

            // Set up UI components
            try {
                Log.d(TAG, "Initializing UI components")
                initializeViews()
                
                // Setup the RecyclerView before attempting to load any data
                setupRecyclerView()
                
                Log.d(TAG, "UI components initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize UI components: ${e.javaClass.simpleName}", e)
                showErrorAndFinish("Failed to initialize app interface: ${e.message}")
                return
            }

            // Initialize database and migrate data
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Starting database initialization")
                    initializeDatabase()
                    Log.d(TAG, "Database initialization completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Database initialization failed: ${e.javaClass.simpleName}", e)
                    runOnUiThread {
                        showErrorAndFinish("Database error: ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.javaClass.simpleName}", e)
            Toast.makeText(this, "Critical error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeCoreComponents() {
        Log.d(TAG, "Initializing core components...")
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        userSettingsViewModel = ViewModelProvider(this)[UserSettingsViewModel::class.java]
        
        // Set up toolbar
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private suspend fun initializeDatabase() {
        // Initialize database
        val database = FinanceDatabase.getDatabase(applicationContext)
        
        // Test database functionality
        testDatabaseFunctionality()
        
        // Clean up any test transactions
        removeTestTransactions()
        
        // Migrate data
        val dataMigration = DataMigration(applicationContext)
        dataMigration.migrateData()
        
        // Load initial data
        loadTransactions()
    }

    private fun showErrorAndFinish(message: String) {
        try {
            if (!isFinishing) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .show()
                }
            } else {
                Log.w(TAG, "Activity finishing, showing Toast instead of Dialog")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error dialog", e)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            // Budget views
            transactionsRecyclerView = binding.transactionsRecyclerView
            budgetAmount = binding.budgetAmount
            budgetProgress = binding.budgetProgress
            remainingBalance = binding.remainingBalance
            
            // Income views
            monthlySalaryAmount = binding.monthlySalaryAmount
            additionalIncomeAmount = binding.additionalIncomeAmount
            totalMoneyToSpend = binding.totalMoneyToSpend
            
            // Setup the settings button if it exists
            setupSettingsButton()
            
            // Setup bottom navigation
            setupBottomNavigation()
            
            // Setup FAB with improved click handling
            val fab = binding.addTransactionFab
            fab.setOnClickListener { view ->
                // Prevent multiple rapid clicks
                view.isEnabled = false
                try {
                    showTransactionDialog()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing transaction dialog", e)
                    showSnackbar("Could not open transaction dialog")
                } finally {
                    // Re-enable the button after a short delay
                    view.postDelayed({ view.isEnabled = true }, 300)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            showErrorAndFinish("Failed to initialize app interface: ${e.message}")
        }
    }
    
    private fun setupSettingsButton() {
        try {
            // The settings button might not exist in the layout
            // Just a placeholder method for now - settings handled by bottom nav
            Log.d(TAG, "Settings button setup (using bottom navigation instead)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up settings button", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavigation = binding.bottomNavigation
            
            // Set the home item as selected
            bottomNavigation.selectedItemId = R.id.nav_home
            
            // Set up navigation item selection listener
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home screen
                        true
                    }
                    R.id.nav_expense_analysis -> {
                        try {
                            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Expense Analysis", e)
                            showSnackbar("Could not open expense analysis")
                        }
                        true
                    }
                    R.id.nav_income_analysis -> {
                        try {
                            startActivity(Intent(this, IncomeAnalysisActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Income Analysis", e)
                            showSnackbar("Could not open income analysis")
                        }
                        true
                    }
                    R.id.nav_budget -> {
                        try {
                            startActivity(Intent(this, BudgetActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Budget", e)
                            showSnackbar("Could not open budget settings")
                        }
                        true
                    }
                    R.id.nav_settings -> {
                        // Use the direct method 
                        openSettingsDirectly()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d(TAG, "Setting up RecyclerView")
            if (!::transactionsRecyclerView.isInitialized) {
                transactionsRecyclerView = binding.transactionsRecyclerView
            }
            
            // Initialize adapter if needed
            if (!::transactionAdapter.isInitialized) {
                transactionAdapter = TransactionAdapter(ArrayList(), this)
            }
            
            // Configure the RecyclerView
            transactionsRecyclerView.let { recyclerView ->
                if (recyclerView.layoutManager == null) {
                    recyclerView.layoutManager = LinearLayoutManager(this)
                }
                if (recyclerView.adapter == null) {
                    recyclerView.adapter = transactionAdapter
                }
                recyclerView.setHasFixedSize(true)
            }
            
            Log.d(TAG, "RecyclerView setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationale()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            "Notifications are required for budget alerts and reminders",
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Don't navigate to HomeActivity from MainActivity on up button
                // This fixes the issue with unwanted navigation
                finish()
                true
            }
            R.id.action_category_analysis -> {
                try {
                    startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Expense Analysis", e)
                    showSnackbar("Could not open expense analysis")
                }
                true
            }
            R.id.action_income_analysis -> {
                try {
                    startActivity(Intent(this, IncomeAnalysisActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Income Analysis", e)
                    showSnackbar("Could not open income analysis")
                }
                true
            }
            R.id.action_monthly_report -> {
                try {
                    startActivity(Intent(this, MonthlyReportActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Monthly Report", e)
                    showSnackbar("Could not open monthly report")
                }
                true
            }
            R.id.action_settings -> {
                openSettingsDirectly()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun safelyCreateDialog(): AlertDialog.Builder {
        return try {
            MaterialAlertDialogBuilder(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MaterialAlertDialogBuilder", e)
            AlertDialog.Builder(this)
        }
    }

    private fun showTransactionDialog(transaction: Transaction? = null) {
        try {
            Log.d(TAG, "Opening transaction dialog: ${transaction?.title ?: "New"}")
            
            // Check if activity is finishing
            if (isFinishing) {
                Log.e(TAG, "Activity is finishing, cannot show dialog")
                return
            }
            
            // Safely inflate the dialog layout
            val dialogView = try {
                layoutInflater.inflate(R.layout.dialog_transaction, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inflate dialog layout", e)
                showSnackbar("Error creating transaction form")
                return
            }
            
            if (dialogView == null) {
                Log.e(TAG, "Dialog view is null after inflation")
                showSnackbar("Error creating transaction form")
                return
            }
            
            // Get views with comprehensive error handling
            val titleInput = try {
                dialogView.findViewById<TextInputEditText>(R.id.titleInput)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding titleInput", e)
                showSnackbar("Error setting up transaction form")
                return
            }
            
            val amountInput = try {
                dialogView.findViewById<TextInputEditText>(R.id.amountInput)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding amountInput", e)
                showSnackbar("Error setting up transaction form")
                return
            }
            
            val categoryInput = try {
                dialogView.findViewById<AutoCompleteTextView>(R.id.categoryInput)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding categoryInput", e)
                showSnackbar("Error setting up transaction form")
                return
            }
            
            val typeGroup = try {
                dialogView.findViewById<RadioGroup>(R.id.transactionTypeGroup)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding typeGroup", e)
                showSnackbar("Error setting up transaction form")
                return
            }
            
            val expenseRadio = try {
                dialogView.findViewById<RadioButton>(R.id.expenseRadio)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding expenseRadio", e)
                showSnackbar("Error setting up transaction form")
                return
            }
            
            val incomeRadio = try {
                dialogView.findViewById<RadioButton>(R.id.incomeRadio)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding incomeRadio", e)
                showSnackbar("Error setting up transaction form")
                return
            }
            
            // Setup category autocomplete with error handling
            try {
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defaultCategories)
                categoryInput.setAdapter(adapter)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up category adapter", e)
                // Continue without autocomplete
            }
            
            // Populate fields if editing an existing transaction
            try {
                transaction?.let {
                    titleInput.setText(it.title)
                    amountInput.setText(String.format(Locale.US, "%.2f", it.amount))
                    categoryInput.setText(it.category)
                    
                    if (it.type == TransactionType.INCOME) {
                        incomeRadio.isChecked = true
                    } else {
                        expenseRadio.isChecked = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error populating transaction fields", e)
                // Continue with empty fields
            }
            
            // Build dialog safely
            val builder = try {
                AlertDialog.Builder(this)
                    .setTitle(if (transaction == null) "Add Transaction" else "Edit Transaction")
                    .setView(dialogView)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating dialog", e)
                showSnackbar("Error creating transaction dialog")
                return
            }
            
            // Add Save button with comprehensive error handling
            builder.setPositiveButton("Save") { _, _ ->
                try {
                    // Get and validate input values
                    val title = titleInput.text?.toString()?.trim() ?: ""
                    val amountText = amountInput.text?.toString()?.replace(",", "") ?: ""
                    val category = categoryInput.text?.toString()?.trim() ?: ""
                    val type = if (expenseRadio.isChecked) "EXPENSE" else "INCOME"
                    
                    if (validateInput(title, amountText, category)) {
                        try {
                            // Parse amount carefully
                            val amount = try {
                                amountText.toDouble()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing amount: $amountText", e)
                                showSnackbar("Please enter a valid number")
                                return@setPositiveButton
                            }
                            
                            // Create transaction entity
                            val transactionEntity = TransactionEntity(
                                id = transaction?.id ?: 0,
                                title = title,
                                amount = amount,
                                category = category,
                                type = type,
                                date = transaction?.date ?: Date(),
                                description = null
                            )
                            
                            // Save to database
                            saveTransaction(transactionEntity, transaction == null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving transaction", e)
                            showSnackbar("Error saving: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in save button handler", e)
                    showSnackbar("Error processing transaction")
                }
            }
            
            // Add Cancel button
            builder.setNegativeButton("Cancel", null)
            
            // Add Delete button if editing
            if (transaction != null) {
                builder.setNeutralButton("Delete") { _, _ ->
                    deleteTransaction(transaction)
                }
            }
            
            // Show dialog safely
            try {
                val dialog = builder.create()
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog", e)
                showSnackbar("Could not show transaction form")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in showTransactionDialog", e)
            showSnackbar("Error showing transaction form")
        }
    }

    private fun saveTransaction(transaction: TransactionEntity, isNew: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isNew) {
                    transactionViewModel.insertTransaction(transaction)
                    runOnUiThread {
                        showSnackbar("Transaction added successfully")
                    }
                } else {
                    transactionViewModel.updateTransaction(transaction)
                    runOnUiThread {
                        showSnackbar("Transaction updated successfully")
                    }
                }
                loadTransactions()
            } catch (e: Exception) {
                Log.e(TAG, "Database error saving transaction", e)
                runOnUiThread {
                    showSnackbar("Database error: ${e.message}")
                }
            }
        }
    }

    private fun validateInput(title: String, amount: String, category: String): Boolean {
        try {
            when {
                title.isEmpty() -> {
                    showSnackbar("Please enter a title")
                    return false
                }
                amount.isEmpty() -> {
                    showSnackbar("Please enter an amount")
                    return false
                }
                amount.toDoubleOrNull() == null -> {
                    showSnackbar("Please enter a valid amount number")
                    return false
                }
                amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! <= 0 -> {
                    showSnackbar("Amount must be greater than zero")
                    return false
                }
                category.isEmpty() -> {
                    showSnackbar("Please select a category")
                    return false
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating input", e)
            showSnackbar("Error validating input fields")
            return false
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        try {
            Log.d(TAG, "Preparing to delete transaction: ${transaction.id} - ${transaction.title}")
            
            // Use the safelyCreateDialog method for consistency
            val builder = safelyCreateDialog()
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                
            builder.setPositiveButton("Delete") { dialog, _ ->
                try {
                    // Delete the transaction using Room
                    Log.d(TAG, "User confirmed delete for transaction: ${transaction.id}")
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val transactionEntity = TransactionEntity(
                                id = transaction.id,
                                title = transaction.title,
                                amount = transaction.amount,
                                category = transaction.category,
                                type = if (transaction.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
                                date = transaction.date,
                                description = null
                            )
                            
                            Log.d(TAG, "Deleting transaction from database: ${transactionEntity.id}")
                            transactionViewModel.deleteTransaction(transactionEntity)
                            
                            runOnUiThread {
                                showSnackbar("Transaction deleted")
                                loadTransactions()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting transaction from database", e)
                            runOnUiThread {
                                showSnackbar("Error: Could not delete transaction")
                            }
                        }
                    }
                    // Dismiss the dialog to prevent issues
                    try {
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing delete dialog", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in delete confirmation", e)
                    showSnackbar("Error: Could not delete transaction")
                    try {
                        dialog.dismiss()
                    } catch (ignored: Exception) {
                        // Already trying to handle an error, ignore this one
                    }
                }
            }
            
            builder.setNegativeButton("Cancel") { dialog, _ -> 
                // Explicitly dismiss dialog when canceled
                try {
                    dialog.dismiss() 
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing delete dialog on cancel", e)
                }
            }
            
            try {
                val dialog = builder.create()
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing delete confirmation dialog", e)
                showSnackbar("Could not show delete confirmation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete confirmation", e)
            showSnackbar("Could not open delete confirmation")
        }
    }

    private fun updateBudgetDisplay() {
        val numberFormat = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("LKR")
        }

        // Update budget information
        budgetAmount.text = String.format("%s / %s", 
            numberFormat.format(currentSpending),
            numberFormat.format(monthlyBudget)
        )

        // Update monthly salary and income information
        monthlySalaryAmount.text = numberFormat.format(monthlySalary)
        additionalIncomeAmount.text = numberFormat.format(additionalIncome)
        
        // Calculate total money to spend and remaining balance
        val totalToSpend = monthlySalary + additionalIncome
        val remaining = totalToSpend - currentSpending
        
        totalMoneyToSpend.text = numberFormat.format(totalToSpend)
        remainingBalance.text = numberFormat.format(remaining)
        
        // Update progress bar
        if (monthlyBudget > 0) {
            val progress = ((currentSpending / monthlyBudget) * 100).toInt().coerceIn(0, 100)
            budgetProgress.progress = progress
            
            // Check if we need to show a warning
            if (currentSpending >= monthlyBudget * 0.9) {
                showBudgetWarning()
            }
        }
    }

    private fun showBudgetWarning() {
        userSettingsViewModel.userSettings.value?.let { settings ->
            if (settings.budgetAlertsEnabled) {
                notificationHelper.showBudgetWarning(currentSpending, monthlyBudget)
            }
        }
    }

    private fun loadTransactions() {
        try {
            Log.d(TAG, "Starting to load transactions")
            
            // Ensure RecyclerView setup
            if (!::transactionAdapter.isInitialized) {
                Log.w(TAG, "Transaction adapter not initialized, setting it up now")
                setupRecyclerView()
            }
            
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Collecting transaction flow")
                    transactionViewModel.getAllTransactions().collectLatest { transactions ->
                        try {
                            Log.d(TAG, "Received ${transactions.size} transactions")
                            
                            // Map and prepare transaction list
                            val transactionList = ArrayList<Transaction>()
                            transactions.forEach { entity ->
                                try {
                                    val transaction = Transaction(
                                        id = entity.id,
                                        title = entity.title,
                                        amount = entity.amount,
                                        category = entity.category,
                                        date = entity.date,
                                        type = if (entity.type == "EXPENSE") TransactionType.EXPENSE else TransactionType.INCOME
                                    )
                                    transactionList.add(transaction)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing transaction: ${entity.id}", e)
                                }
                            }
                            
                            // Update UI on main thread
                            runOnUiThread {
                                try {
                                    if (::transactionAdapter.isInitialized) {
                                        Log.d(TAG, "Updating adapter with ${transactionList.size} transactions")
                                        transactionAdapter.updateTransactions(transactionList)
                                    }
                                    
                                    // Update budget info
                                    try {
                                        currentSpending = transactions
                                            .filter { it.type == "EXPENSE" }
                                            .sumOf { it.amount }
                                        
                                        additionalIncome = transactions
                                            .filter { it.type == "INCOME" }
                                            .sumOf { it.amount }
                                        
                                        updateBudgetDisplay()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error calculating transaction totals", e)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error updating UI with transactions", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing transactions data", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting transactions flow", e)
                    runOnUiThread {
                        showSnackbar("Error loading transactions")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadTransactions", e)
            showSnackbar("Could not load transactions")
        }
    }

    override fun onTransactionClick(transaction: Transaction) {
        try {
            // Show transaction dialog with the selected transaction
            showTransactionDialog(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling transaction click", e)
            showSnackbar("Could not open transaction details")
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload transactions
        loadTransactions()
        
        // Load latest user settings
        loadUserSettings()
    }

    private fun loadUserSettings() {
        try {
            Log.d(TAG, "Loading latest user settings")
            userSettingsViewModel.userSettings.observe(this) { settings ->
                if (settings != null) {
                    Log.d(TAG, "User settings loaded: $settings")
                    // Update the UI values
                    monthlySalary = settings.monthlySalary
                    monthlyBudget = settings.monthlyBudget
                    
                    // Update the UI
                    updateBudgetDisplay()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user settings", e)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("monthlyBudget", monthlyBudget)
        outState.putDouble("currentSpending", currentSpending)
        outState.putDouble("monthlySalary", monthlySalary)
        outState.putDouble("additionalIncome", additionalIncome)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        monthlyBudget = savedInstanceState.getDouble("monthlyBudget")
        currentSpending = savedInstanceState.getDouble("currentSpending")
        monthlySalary = savedInstanceState.getDouble("monthlySalary")
        additionalIncome = savedInstanceState.getDouble("additionalIncome")
        updateBudgetDisplay()
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            moveTaskToBack(false)
        } else {
            super.onBackPressed()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setIcon(android.R.drawable.ic_dialog_alert)
            
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            // If even showing the dialog fails, fall back to Toast
            Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Failed to show error dialog", e)
        }
    }

    private fun launchSettingsActivity() {
        try {
            startActivity(Intent(this, SettingsActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Settings: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettingsDirectly() {
        try {
            // Open the actual SettingsActivity
            startActivity(Intent(this, SettingsActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings: ${e.message}")
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testDatabaseFunctionality() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Only check if database is accessible, don't add test transactions
                try {
                    val transactions = transactionViewModel.getAllTransactions().firstOrNull() ?: emptyList()
                    Log.d(TAG, "Database check: Found ${transactions.size} transactions in database")
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing database", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database test failed", e)
            }
        }
    }

    private fun removeTestTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Find and delete test transactions
                val transactions = transactionViewModel.getAllTransactions().firstOrNull() ?: emptyList()
                for (transaction in transactions) {
                    if (transaction.title == "Test Transaction" && transaction.category == "Test Category") {
                        transactionViewModel.deleteTransaction(transaction)
                        Log.d(TAG, "Deleted test transaction: ${transaction.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing test transactions", e)
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}