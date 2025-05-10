package com.example.financetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings LIMIT 1")
    fun getUserSettings(): Flow<UserSettings?>
    
    @Query("SELECT * FROM user_settings LIMIT 1")
    fun getUserSettingsLive(): LiveData<UserSettings>
    
    @Query("SELECT * FROM user_settings LIMIT 1")
    suspend fun getUserSettingsSync(): UserSettings?
    
    @Query("SELECT COUNT(*) FROM user_settings")
    suspend fun getUserSettingsCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(userSettings: UserSettings)
    
    @Update
    suspend fun updateUserSettings(userSettings: UserSettings)
    
    // Specific fields as Flows
    @Query("SELECT monthly_salary FROM user_settings LIMIT 1")
    fun getMonthlySalary(): Flow<Double>
    
    @Query("SELECT monthly_budget FROM user_settings LIMIT 1")
    fun getMonthlyBudget(): Flow<Double>
    
    @Query("SELECT currency FROM user_settings LIMIT 1")
    fun getCurrency(): Flow<String>
    
    @Query("SELECT budget_alerts_enabled FROM user_settings LIMIT 1")
    fun getBudgetAlertsEnabled(): Flow<Boolean>
    
    // Update individual fields
    @Query("UPDATE user_settings SET monthly_salary = :salary")
    suspend fun updateMonthlySalary(salary: Double)
    
    @Query("UPDATE user_settings SET monthly_budget = :budget")
    suspend fun updateMonthlyBudget(budget: Double)
    
    @Query("UPDATE user_settings SET currency = :currency")
    suspend fun updateCurrency(currency: String)
    
    @Query("UPDATE user_settings SET budget_alerts_enabled = :enabled")
    suspend fun updateBudgetAlertsEnabled(enabled: Boolean)
} 