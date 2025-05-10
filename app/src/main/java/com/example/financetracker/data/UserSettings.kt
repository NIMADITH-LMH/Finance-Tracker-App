package com.example.financetracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user settings like monthly salary and budget limits
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "monthly_salary")
    val monthlySalary: Double = 0.0,
    
    @ColumnInfo(name = "monthly_budget")
    val monthlyBudget: Double = 0.0,
    
    @ColumnInfo(name = "currency")
    val currency: String = "$",
    
    @ColumnInfo(name = "budget_alerts_enabled")
    val budgetAlertsEnabled: Boolean = true,
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
) 