package com.example.financetracker.data

import com.example.financetracker.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.*

class TransactionRepository(private val transactionDao: TransactionDao) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type.name)

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate)

    suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun getTotalIncomeBetweenDates(startDate: Date, endDate: Date): Double =
        transactionDao.getTotalIncomeBetweenDates(startDate, endDate)

    suspend fun getTotalExpensesBetweenDates(startDate: Date, endDate: Date): Double =
        transactionDao.getTotalExpensesBetweenDates(startDate, endDate)

    fun getTopExpenseCategories(startDate: Date, endDate: Date): Flow<List<CategoryTotal>> =
        transactionDao.getTopExpenseCategories(startDate, endDate)

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
} 