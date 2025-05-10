package com.example.financetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.model.Category
import java.text.NumberFormat

class CategoryAdapter(
    private val categories: List<Category>,
    private val currencyFormat: NumberFormat
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryNameTextView: TextView = view.findViewById(R.id.categoryNameTextView)
        val categoryAmountTextView: TextView = view.findViewById(R.id.categoryAmountTextView)
        val categoryPercentageTextView: TextView = view.findViewById(R.id.categoryPercentageTextView)
        val categoryProgressBar: ProgressBar = view.findViewById(R.id.categoryProgressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        
        holder.categoryNameTextView.text = category.name
        holder.categoryAmountTextView.text = currencyFormat.format(category.amount)
        holder.categoryPercentageTextView.text = "${category.percentage}%"
        holder.categoryProgressBar.progress = category.percentage
    }

    override fun getItemCount() = categories.size
} 