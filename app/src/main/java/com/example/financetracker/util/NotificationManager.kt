package com.example.financetracker.util

import android.content.Context
import com.example.financetracker.util.NotificationHelper

/**
 * A manager class for handling all notification-related operations.
 */
class NotificationManager(private val context: Context) {
    private val notificationHelper = NotificationHelper(context)
    
    /**
     * Sends a budget alert notification when user is exceeding their budget limit
     */
    fun sendBudgetAlert(currentSpending: Double, budgetLimit: Double) {
        notificationHelper.showBudgetWarning(currentSpending, budgetLimit)
    }
    
    /**
     * Schedules a daily reminder to record expenses
     */
    fun enableDailyReminders() {
        notificationHelper.scheduleDailyReminder()
    }
    
    /**
     * Cancels scheduled daily reminders
     */
    fun disableDailyReminders() {
        notificationHelper.cancelDailyReminder()
    }
    
    /**
     * Cancels all active budget notifications
     */
    fun clearBudgetNotifications() {
        notificationHelper.cancelBudgetNotifications()
    }
} 