package com.example.financetracker

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.financetracker.data.TransactionEntity
import com.example.financetracker.viewmodel.TransactionViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var viewModel: TransactionViewModel
    private lateinit var titleInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var categoryInput: AutoCompleteTextView
    private lateinit var dateInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        // Initialize views
        titleInput = findViewById(R.id.titleInput)
        amountInput = findViewById(R.id.amountInput)
        categoryInput = findViewById(R.id.categoryInput)
        dateInput = findViewById(R.id.dateInput)
        descriptionInput = findViewById(R.id.descriptionInput)

        // Set up category dropdown
        val categories = arrayOf("Food", "Transport", "Entertainment", "Bills", "Shopping", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (categoryInput as? AutoCompleteTextView)?.setAdapter(adapter)

        // Set current date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateInput.setText(dateFormat.format(Date()))

        // Set up save button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveTransaction()
        }

        // Set up cancel button
        findViewById<Button>(R.id.cancelButton).setOnClickListener {
            finish()
        }
    }

    private fun saveTransaction() {
        val title = titleInput.text.toString()
        val amount = amountInput.text.toString().toDoubleOrNull()
        val category = categoryInput.text.toString()
        val dateStr = dateInput.text.toString()
        val description = descriptionInput.text.toString()
        val type = if (findViewById<RadioButton>(R.id.expenseRadio).isChecked) "EXPENSE" else "INCOME"

        if (title.isEmpty() || amount == null || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            dateStr.takeIf { it.isNotEmpty() }?.let { dateFormat.parse(it) } ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val transaction = TransactionEntity(
            title = title,
            amount = amount,
            category = category,
            type = type,
            date = date,
            description = description
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                viewModel.insertTransaction(transaction)
                Toast.makeText(this@AddTransactionActivity, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddTransactionActivity, "Error saving transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 