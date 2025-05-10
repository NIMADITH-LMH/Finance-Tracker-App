package com.example.financetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _budgetStatus = MutableLiveData<Double>()
    val budgetStatus: LiveData<Double> = _budgetStatus
    
    private val _remainingBudget = MutableLiveData<Double>()
    val remainingBudget: LiveData<Double> = _remainingBudget
    
    // Initialize with default values
    init {
        _budgetStatus.value = 0.0
        _remainingBudget.value = 0.0
    }
    
    fun calculateBudgetStatus(currentSpending: Double, budgetLimit: Double) {
        viewModelScope.launch {
            val status = if (budgetLimit > 0) {
                (currentSpending / budgetLimit) * 100
            } else {
                0.0
            }
            _budgetStatus.postValue(status)
        }
    }
    
    fun calculateRemainingBudget(income: Double, spending: Double) {
        viewModelScope.launch {
            val remaining = income - spending
            _remainingBudget.postValue(remaining)
        }
    }
} 