package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_splash)
            
            // Set up the toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            
            // Initialize your preferences or other components safely
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    navigateToHome()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to home", e)
                    Toast.makeText(this, "Error starting the app", Toast.LENGTH_SHORT).show()
                }
            }, 2000) // 2 seconds delay
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            // Even in case of error, try to navigate to home
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    navigateToHome()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to home after onCreate failure", e)
                    // If we can't even navigate to home, just finish this activity
                    finish()
                }
            }, 1000)
        }
    }

    private fun navigateToHome() {
        try {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to HomeActivity", e)
            Toast.makeText(this, "Unable to start the app", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                try {
                    navigateToHome()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to home from back button", e)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 