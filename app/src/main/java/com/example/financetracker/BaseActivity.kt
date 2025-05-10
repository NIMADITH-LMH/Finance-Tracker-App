package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {
    private val TAG = "BaseActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            // Set content view from layout resource ID
            setContentView(getLayoutResourceId())
            
            // Set up common components
            setupToolbar()
            setupBottomNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing activity", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        try {
            // Find toolbar if it exists in the layout
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            } else {
                Log.d(TAG, "No toolbar found in layout")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            // Find bottom navigation if it exists in the layout
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            if (bottomNavigationView != null) {
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    // Only navigate if we're not already on that screen
                    when (item.itemId) {
                        R.id.nav_home -> {
                            if (this !is MainActivity) {
                                safeNavigate { Intent(this, MainActivity::class.java) }
                                return@setOnNavigationItemSelectedListener true
                            }
                        }
                        R.id.nav_expense_analysis -> {
                            if (this !is CategoryAnalysisActivity) {
                                safeNavigate { Intent(this, CategoryAnalysisActivity::class.java) }
                                return@setOnNavigationItemSelectedListener true
                            }
                        }
                        R.id.nav_income_analysis -> {
                            if (this !is IncomeAnalysisActivity) {
                                safeNavigate { Intent(this, IncomeAnalysisActivity::class.java) }
                                return@setOnNavigationItemSelectedListener true
                            }
                        }
                        R.id.nav_settings -> {
                            if (this !is SettingsActivity) {
                                safeNavigate { Intent(this, SettingsActivity::class.java) }
                                return@setOnNavigationItemSelectedListener true
                            }
                        }
                    }
                    true
                }
                
                // Set the selected item
                bottomNavigationView.selectedItemId = getNavigationMenuItemId()
            } else {
                Log.d(TAG, "No bottom navigation found in layout")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation", e)
        }
    }
    
    // Helper method for safe navigation with error handling
    private fun safeNavigate(intentCreator: () -> Intent) {
        try {
            val intent = intentCreator()
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                navigateUp()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        navigateUp()
        return true
    }
    
    private fun navigateUp() {
        try {
            // First try to find the parent activity
            val parentName = NavUtils.getParentActivityName(this)
            
            if (parentName != null) {
                // We have a parent defined in the manifest
                val upIntent = NavUtils.getParentActivityIntent(this)
                if (upIntent != null) {
                    startActivity(upIntent)
                    finish()
                } else {
                    // Fallback
                    goToMainActivity()
                }
            } else if (this !is MainActivity) {
                // No defined parent, go to MainActivity if not already there
                goToMainActivity()
            } else {
                // We're in MainActivity, just finish
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating up", e)
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            // Last resort fallback
            try {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } catch (e2: Exception) {
                Log.e(TAG, "Fatal navigation error", e2)
            }
        }
    }
    
    private fun goToMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error going to MainActivity", e)
        }
    }

    abstract fun getLayoutResourceId(): Int
    abstract fun getNavigationMenuItemId(): Int
} 