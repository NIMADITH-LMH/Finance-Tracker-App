package com.example.financetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.financetracker.util.PreferencesManager
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.content.ContextCompat

class NotificationSettingsActivity : AppCompatActivity() {
    private val TAG = "NotificationSettings"
    
    private lateinit var preferencesManager: PreferencesManager
    
    // UI elements as nullable
    private var budgetAlertSwitch: SwitchMaterial? = null
    private var dailyReminderSwitch: SwitchMaterial? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)
        
        try {
            // Set up the toolbar manually
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            // Set navigation icon color to white
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
            supportActionBar?.title = "Notification Settings"
            
            // Initialize preference manager
            preferencesManager = PreferencesManager(this)
            
            // Safely initialize views
            initializeViews()
            
            // Load current settings
            loadSettings()
            
            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Could not initialize settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initializeViews() {
        try {
            budgetAlertSwitch = findViewById(R.id.budgetAlertSwitch)
            dailyReminderSwitch = findViewById(R.id.dailyReminderSwitch)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
        }
    }
    
    private fun loadSettings() {
        try {
            // Get current settings
            val budgetAlertsEnabled = preferencesManager.getBudgetAlertsEnabled()
            val dailyRemindersEnabled = preferencesManager.getDailyRemindersEnabled()
            
            // Set current values to UI with null checks
            budgetAlertSwitch?.isChecked = budgetAlertsEnabled
            dailyReminderSwitch?.isChecked = dailyRemindersEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings", e)
        }
    }
    
    private fun setupListeners() {
        try {
            budgetAlertSwitch?.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.setBudgetAlertEnabled(isChecked)
                Toast.makeText(this, 
                    if (isChecked) "Budget alerts enabled" else "Budget alerts disabled", 
                    Toast.LENGTH_SHORT).show()
            }
            
            dailyReminderSwitch?.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.setDailyRemindersEnabled(isChecked)
                Toast.makeText(this, 
                    if (isChecked) "Daily reminders enabled" else "Daily reminders disabled", 
                    Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listeners", e)
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Navigate back to settings activity safely
            try {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating up", e)
                // Just finish if we can't navigate
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationSettingsActivity::class.java)
        }
    }
} 