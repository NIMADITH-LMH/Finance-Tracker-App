package com.example.financetracker.model

import java.util.Date

data class Transaction(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var amount: Double,
    var category: String,
    var date: Date,
    var type: TransactionType
)

enum class TransactionType {
    INCOME, EXPENSE
} 