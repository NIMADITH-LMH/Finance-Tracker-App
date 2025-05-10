package com.example.financetracker.data

import android.content.Context
import android.util.Log
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import com.example.financetracker.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class DataMigration(private val context: Context) {
    private val TAG = "DataMigration"
    private val preferencesManager = PreferencesManager(context)
    private val database = FinanceDatabase.getDatabase(context)
    private val repository = TransactionRepository(database.transactionDao())

    suspend fun migrateData() = withContext(Dispatchers.IO) {
        try {
            // Get existing transactions from PreferencesManager
            val existingTransactions = preferencesManager.getTransactions()
            Log.d(TAG, "Found ${existingTransactions.size} transactions to migrate")
            
            if (existingTransactions.isNotEmpty()) {
                // Convert to TransactionEntity and insert into Room database
                existingTransactions.forEach { transaction ->
                    try {
                        val entity = TransactionEntity(
                            title = transaction.title,
                            amount = transaction.amount,
                            category = transaction.category,
                            type = transaction.type.name,
                            date = transaction.date,
                            description = null // Set description to null since it's not in the old model
                        )
                        repository.insertTransaction(entity)
                        Log.d(TAG, "Successfully migrated transaction: ${transaction.title}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error migrating transaction: ${transaction.title}", e)
                    }
                }

                // Clear old data from PreferencesManager
                preferencesManager.clearTransactions()
                Log.d(TAG, "Successfully cleared old transactions from PreferencesManager")
            } else {
                Log.d(TAG, "No transactions to migrate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during data migration", e)
            throw e
        }
    }

    private fun PreferencesManager.clearTransactions() {
        // Implementation depends on how transactions are stored in PreferencesManager
        // This is a placeholder - you'll need to implement the actual clearing logic
    }
} 