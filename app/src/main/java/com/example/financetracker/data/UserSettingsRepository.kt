package com.example.financetracker.data

import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {
    
    fun getUserSettings(): Flow<UserSettings?> = userSettingsDao.getUserSettings()
    
    suspend fun updateMonthlySalary(salary: Double) = userSettingsDao.updateMonthlySalary(salary)
    
    suspend fun updateMonthlyBudget(budget: Double) = userSettingsDao.updateMonthlyBudget(budget)
    
    suspend fun updateBudgetAlertsEnabled(enabled: Boolean) = userSettingsDao.updateBudgetAlertsEnabled(enabled)
    
    suspend fun updateCurrency(currency: String) = userSettingsDao.updateCurrency(currency)
    
    suspend fun updateUserSettings(settings: UserSettings) = userSettingsDao.updateUserSettings(settings)
    
    suspend fun insertUserSettings(settings: UserSettings) = userSettingsDao.insertUserSettings(settings)
} 