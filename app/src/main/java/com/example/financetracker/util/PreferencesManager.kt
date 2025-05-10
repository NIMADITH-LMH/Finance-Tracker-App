package com.example.financetracker.util

import android.content.Context
import android.content.SharedPreferences
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("FinanceTracker", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_MONTHLY_SALARY = "monthly_salary"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_BUDGET_ALERTS = "budget_alerts_enabled"
        private const val KEY_DAILY_REMINDERS = "daily_reminders_enabled"
    }

    fun saveMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }

    fun setMonthlyBudget(budget: Double) {
        saveMonthlyBudget(budget)
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun setMonthlySalary(salary: Double) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_SALARY, salary.toFloat()).apply()
    }

    fun getMonthlySalary(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_SALARY, 0f).toDouble()
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BUDGET_ALERTS, enabled).apply()
    }

    fun getBudgetAlertsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BUDGET_ALERTS, true)
    }

    fun setDailyRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DAILY_REMINDERS, enabled).apply()
    }

    fun getDailyRemindersEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DAILY_REMINDERS, false)
    }

    fun setBudgetAlertEnabled(enabled: Boolean) {
        setBudgetAlertsEnabled(enabled)
    }

    fun getBudgetAlertEnabled(): Boolean {
        return getBudgetAlertsEnabled()
    }

    fun setBudgetAlertEnable(isChecked: Boolean) {
        setBudgetAlertsEnabled(isChecked)
    }

    fun saveTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            transactions[index] = updatedTransaction
            saveTransactions(transactions)
        }
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]") ?: "[]"
        val transactions = mutableListOf<Transaction>()
        
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                transactions.add(
                    Transaction(
                        id = jsonObject.getLong("id"),
                        title = jsonObject.getString("title"),
                        amount = jsonObject.getDouble("amount"),
                        category = jsonObject.getString("category"),
                        date = Date(jsonObject.getLong("date")),
                        type = TransactionType.valueOf(jsonObject.getString("type"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return transactions
    }

    fun getTransactionsByCategory(): Map<String, List<Transaction>> {
        return getTransactions().groupBy { it.category }
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val jsonArray = JSONArray()
        
        transactions.forEach { transaction ->
            val jsonObject = JSONObject().apply {
                put("id", transaction.id)
                put("title", transaction.title)
                put("amount", transaction.amount)
                put("category", transaction.category)
                put("date", transaction.date.time)
                put("type", transaction.type.name)
            }
            jsonArray.put(jsonObject)
        }
        
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
    }

    fun deleteTransaction(transactionId: Long) {
        try {
            // Get current transactions
            val transactions = getTransactions().toMutableList()
            
            // Log before deletion
            val initialCount = transactions.size
            val transactionToDelete = transactions.find { it.id == transactionId }
            
            // Remove the transaction with the given ID
            transactions.removeAll { it.id == transactionId }
            
            // Log after deletion
            val finalCount = transactions.size
            
            // Save the updated list
            saveTransactions(transactions)
            
            // Verify deletion
            if (initialCount == finalCount) {
                android.util.Log.w("PreferencesManager", "Transaction with ID $transactionId was not found or deleted")
            } else {
                android.util.Log.d("PreferencesManager", 
                    "Transaction deleted: ID=$transactionId, Title=${transactionToDelete?.title}, " +
                    "Before=$initialCount, After=$finalCount")
            }
        } catch (e: Exception) {
            android.util.Log.e("PreferencesManager", "Error deleting transaction: $transactionId", e)
            // Re-throw to allow handling in the UI
            throw e
        }
    }

    fun setCurrency(currency: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "LKR") ?: "LKR"
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }

    fun exportTransactions(): String {
        return sharedPreferences.getString(KEY_TRANSACTIONS, "[]") ?: "[]"
    }

    fun importTransactions(json: String) {
        try {
            // Validate JSON format
            JSONArray(json)
            sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup data format")
        }
    }

    fun clearTransactions() {
        sharedPreferences.edit().remove(KEY_TRANSACTIONS).apply()
    }
} 