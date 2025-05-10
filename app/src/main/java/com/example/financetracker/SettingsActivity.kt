package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.example.financetracker.util.PreferencesManager
import androidx.core.content.ContextCompat

/**
 * Settings activity that primarily serves as a navigation hub to more specific settings
 */
class SettingsActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Create a simple settings layout programmatically
            val rootLayout = LinearLayout(this)
            rootLayout.orientation = LinearLayout.VERTICAL
            
            // Create and setup toolbar
            val toolbar = Toolbar(this)
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.title = "Settings"
            rootLayout.addView(toolbar)
            
            // Content container
            val contentLayout = LinearLayout(this)
            contentLayout.orientation = LinearLayout.VERTICAL
            contentLayout.setPadding(30, 30, 30, 30)
            
            // Notification Settings Card
            val notificationCard = createSettingsCard("Notification Settings", 
                "Configure alerts and reminders")
            notificationCard.setOnClickListener {
                startActivity(Intent(this, NotificationSettingsActivity::class.java))
            }
            contentLayout.addView(notificationCard)
            
            // About Card
            val aboutCard = createSettingsCard("About", 
                "Finance Tracker v1.0.0\n© 2023 Finance Tracker. All rights reserved.")
            contentLayout.addView(aboutCard)
            
            // Add content layout to root
            rootLayout.addView(contentLayout)
            
            // Set the root layout as content view
            setContentView(rootLayout)
            
            // Initialize preference manager
            preferencesManager = PreferencesManager(this)
            
            Log.d(TAG, "Settings activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun createSettingsCard(title: String, description: String): CardView {
        // Create card
        val card = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 30)
        card.layoutParams = layoutParams
        card.radius = 12f
        card.cardElevation = 8f
        
        // Create content layout
        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.setPadding(30, 30, 30, 30)
        
        // Add title
        val titleView = TextView(this)
        titleView.text = title
        titleView.textSize = 18f
        contentLayout.addView(titleView)
        
        // Add description
        val descriptionView = TextView(this)
        descriptionView.text = description
        descriptionView.textSize = 14f
        descriptionView.setPadding(0, 10, 0, 0)
        contentLayout.addView(descriptionView)
        
        // Add content to card
        card.addView(contentLayout)
        
        return card
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 