package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class HomeActivity : AppCompatActivity() {
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "Starting HomeActivity onCreate")
            setContentView(R.layout.activity_home)
            
            // Set up the toolbar with NoActionBar theme
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(false) // No back button on home
            } else {
                Log.w(TAG, "Toolbar not found in layout")
            }
            
            // Set up the Get Started button
            val getStartedButton = findViewById<Button>(R.id.getStartedButton)
            if (getStartedButton == null) {
                Log.e(TAG, "Get Started button not found in layout")
                showErrorDialog("Initialization Error", "Could not find the Get Started button")
                return
            }
            
            getStartedButton.setOnClickListener {
                try {
                    Log.d(TAG, "Get Started button clicked, preparing to start MainActivity")
                    // Add some basic checks before starting MainActivity
                    if (!isFinishing && !isDestroyed) {
                        val intent = Intent(this, MainActivity::class.java)
                        // Add flags to create a clean start
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        Log.d(TAG, "Starting MainActivity...")
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "Activity is finishing or destroyed, cannot start MainActivity")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start MainActivity", e)
                    val errorMessage = when {
                        e is ClassNotFoundException -> "MainActivity not found. Please check your app configuration."
                        e is SecurityException -> "Security error while starting MainActivity."
                        e.message?.contains("layout") == true -> "Layout error in MainActivity."
                        else -> "Unable to start the main app: ${e.message}"
                    }
                    showErrorDialog("Navigation Error", errorMessage)
                }
            }
            Log.d(TAG, "HomeActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            showErrorDialog("Initialization Error", "Error initializing home screen: ${e.message}")
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        try {
            if (isFinishing || isDestroyed) {
                Log.w(TAG, "Activity is finishing/destroyed, showing Toast instead of Dialog")
                Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
                return
            }
            
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setIcon(android.R.drawable.ic_dialog_alert)
            
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            // If even showing the dialog fails, fall back to Toast
            Log.e(TAG, "Error showing dialog, falling back to Toast", e)
            Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.home_menu, menu)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu", e)
            showErrorDialog("Menu Error", "Error creating options menu: ${e.message}")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                try {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Settings", e)
                    showErrorDialog("Navigation Error", "Could not open settings: ${e.message}")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 