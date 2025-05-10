package com.example.financetracker.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, UserSettings::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d("FinanceDatabase", "Database created")
                        verifyDatabaseStructure(db)
                        
                        // Initialize UserSettings in a background thread
                        CoroutineScope(Dispatchers.IO).launch {
                            initializeUserSettings(INSTANCE!!)
                        }
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Log.d("FinanceDatabase", "Database opened")
                        verifyDatabaseStructure(db)
                        
                        // Check and initialize UserSettings if needed on every open
                        CoroutineScope(Dispatchers.IO).launch {
                            initializeUserSettings(INSTANCE!!)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Alias for getDatabase that's used in UserSettingsViewModel
        fun getInstance(context: Context): FinanceDatabase {
            return getDatabase(context)
        }
        
        private suspend fun initializeUserSettings(database: FinanceDatabase) {
            val userSettingsDao = database.userSettingsDao()
            val existingSettings = userSettingsDao.getUserSettingsSync()
            
            if (existingSettings == null) {
                Log.d("FinanceDatabase", "Initializing default UserSettings")
                val defaultSettings = UserSettings(
                    id = 1,
                    monthlySalary = 0.0,
                    monthlyBudget = 0.0,
                    budgetAlertsEnabled = true,
                    currency = "USD"
                )
                userSettingsDao.insertUserSettings(defaultSettings)
            } else {
                Log.d("FinanceDatabase", "UserSettings already exist: $existingSettings")
            }
        }

        private fun verifyDatabaseStructure(db: SupportSQLiteDatabase) {
            try {
                // Verify transactions table structure
                val cursor = db.query("PRAGMA table_info(transactions)")
                val columnNames = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(1)
                    columnNames.add(columnName)
                    Log.d("FinanceDatabase", "Column: $columnName")
                }
                cursor.close()
                
                // Verify user_settings table structure
                val settingsCursor = db.query("PRAGMA table_info(user_settings)")
                val settingsColumns = mutableListOf<String>()
                while (settingsCursor.moveToNext()) {
                    val columnName = settingsCursor.getString(1)
                    settingsColumns.add(columnName)
                    Log.d("FinanceDatabase", "UserSettings Column: $columnName")
                }
                settingsCursor.close()
                
                // Log any indices
                val indicesCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index'")
                while (indicesCursor.moveToNext()) {
                    val indexName = indicesCursor.getString(0)
                    Log.d("FinanceDatabase", "Index: $indexName")
                }
                indicesCursor.close()
            } catch (e: Exception) {
                Log.e("FinanceDatabase", "Error verifying database structure", e)
            }
        }
    }
} 