package com.example.financetracker.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import java.util.Currency

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val listener: TransactionClickListener
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    interface TransactionClickListener {
        fun onTransactionClick(transaction: Transaction)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.transactionTitle)
        val amountText: TextView = view.findViewById(R.id.transactionAmount)
        val categoryText: TextView = view.findViewById(R.id.transactionCategory)
        val dateText: TextView = view.findViewById(R.id.transactionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("LKR")
        }

        holder.titleText.text = transaction.title
        holder.amountText.text = String.format(
            "%s%s",
            if (transaction.type == TransactionType.EXPENSE) "-" else "+",
            currencyFormat.format(transaction.amount).replace("LKR", "").trim()
        )
        holder.categoryText.text = transaction.category
        holder.dateText.text = dateFormat.format(transaction.date)

        // Set text color based on transaction type
        val textColor = if (transaction.type == TransactionType.INCOME) 
            holder.itemView.context.getColor(android.R.color.holo_green_dark)
        else 
            holder.itemView.context.getColor(android.R.color.holo_red_dark)
        
        holder.amountText.setTextColor(textColor)

        // Set click listener with error handling
        holder.itemView.setOnClickListener { view ->
            try {
                // Disable the view temporarily to prevent multiple clicks
                view.isEnabled = false
                
                // Notify the listener about the click
                listener.onTransactionClick(transaction)
                
                // Re-enable the view after a short delay
                view.postDelayed({ view.isEnabled = true }, 300)
            } catch (e: Exception) {
                // Log the error and re-enable the view
                Log.e("TransactionAdapter", "Error handling item click", e)
                view.isEnabled = true
            }
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions.sortedByDescending { it.date }
        notifyDataSetChanged()
    }
} 