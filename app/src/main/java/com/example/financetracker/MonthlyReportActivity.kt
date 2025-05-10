package com.example.financetracker

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.financetracker.data.CategoryTotal
import com.example.financetracker.data.FinanceDatabase
import com.example.financetracker.data.TransactionRepository
import com.example.financetracker.databinding.ActivityMonthlyReportBinding
import com.example.financetracker.model.TransactionType
import com.example.financetracker.util.PreferencesManager
import com.example.financetracker.viewmodel.TransactionViewModel
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.first

class MonthlyReportActivity : AppCompatActivity() {
    private val TAG = "MonthlyReportActivity"
    private var _binding: ActivityMonthlyReportBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var transactionViewModel: TransactionViewModel
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK")).apply {
        currency = Currency.getInstance("LKR")
    }
    private val dateFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val fileDateFormatter = SimpleDateFormat("yyyyMM", Locale.getDefault())

    companion object {
        private const val WRITE_EXTERNAL_STORAGE_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            _binding = ActivityMonthlyReportBinding.inflate(layoutInflater)
            setContentView(binding.root)

            preferencesManager = PreferencesManager(this)
            transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

            setupToolbar()
            setupAppBarBehavior()
            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing report screen: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.monthly_report)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar", e)
            Toast.makeText(this, "Error setting up toolbar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAppBarBehavior() {
        try {
            binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, _ ->
                // Handle app bar offset changes if needed
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up app bar behavior", e)
        }
    }

    private fun setupClickListeners() {
        try {
            binding.btnGenerateReport.setOnClickListener {
                binding.btnGenerateReport.isEnabled = false // Disable button while generating
                generateReport()
            }

            binding.btnDownloadReport.setOnClickListener {
                if (checkStoragePermission()) {
                    downloadReport()
                } else {
                    requestStoragePermission()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners", e)
            Toast.makeText(this, "Error setting up buttons: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateReport() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting report generation")
                binding.btnGenerateReport.isEnabled = false
                
                val currentMonth = Calendar.getInstance()
                val firstDayOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val lastDayOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                Log.d(TAG, "Date range: ${firstDayOfMonth.time} to ${lastDayOfMonth.time}")

                // Use withContext to switch to IO dispatcher for database operations
                val totalIncome = withContext(Dispatchers.IO) {
                    transactionViewModel.getTotalIncomeBetweenDates(
                        firstDayOfMonth.time,
                        lastDayOfMonth.time
                    )
                }
                Log.d(TAG, "Total income: $totalIncome")

                val totalExpenses = withContext(Dispatchers.IO) {
                    transactionViewModel.getTotalExpensesBetweenDates(
                        firstDayOfMonth.time,
                        lastDayOfMonth.time
                    )
                }
                Log.d(TAG, "Total expenses: $totalExpenses")

                val monthlyBudget = preferencesManager.getMonthlyBudget()
                val balance = totalIncome - totalExpenses

                Log.d(TAG, "Fetching top expense categories")
                val categories = withContext(Dispatchers.IO) {
                    transactionViewModel.getTopExpenseCategories(
                        firstDayOfMonth.time,
                        lastDayOfMonth.time
                    ).first()
                }

                val reportText = buildString {
                    appendLine("Monthly Financial Report - ${dateFormatter.format(currentMonth.time)}")
                    appendLine()
                    appendLine("Income Summary:")
                    appendLine("Total Income: ${currencyFormatter.format(totalIncome)}")
                    appendLine()
                    appendLine("Expense Summary:")
                    appendLine("Total Expenses: ${currencyFormatter.format(totalExpenses)}")
                    appendLine("Monthly Budget: ${currencyFormatter.format(monthlyBudget)}")
                    appendLine("Budget Usage: ${if (monthlyBudget > 0) String.format("%.1f%%", (totalExpenses/monthlyBudget)*100) else "N/A"}")
                    appendLine()
                    appendLine("Top Expense Categories:")
                    categories.forEach { categoryTotal ->
                        appendLine("${categoryTotal.category}: ${currencyFormatter.format(categoryTotal.total)}")
                    }
                    appendLine()
                    appendLine("Balance: ${currencyFormatter.format(balance)}")
                    appendLine()
                    appendLine("Generated by Finance Tracker App")
                }

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Setting report preview text")
                    binding.tvReportPreview.text = reportText
                    binding.reportPreviewCard.visibility = View.VISIBLE
                    binding.btnGenerateReport.isEnabled = true
                    binding.btnDownloadReport.isEnabled = true
                    Log.d(TAG, "Report generation completed successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating report", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MonthlyReportActivity, 
                        "Error generating report: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                    binding.btnGenerateReport.isEnabled = true
                    binding.btnDownloadReport.isEnabled = false
                }
            }
        }
    }

    private fun downloadReport() {
        try {
            val reportText = binding.tvReportPreview.text.toString()
            val fileName = "Finance_Report_${fileDateFormatter.format(Date())}.pdf"
            
            // Create PDF document
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            
            // Load and draw logo
            val logo = BitmapFactory.decodeResource(resources, R.drawable.ic_finance_logo)
            if (logo != null) {
                // Scale logo to reasonable size (120x120 pixels for better visibility)
                val scaledLogo = Bitmap.createScaledBitmap(logo, 120, 120, true)
                // Center the logo horizontally
                val logoX = (pageInfo.pageWidth - scaledLogo.width) / 2f
                canvas.drawBitmap(scaledLogo, logoX, 30f, null)
                // Recycle the bitmaps to free memory
                scaledLogo.recycle()
                logo.recycle()
            }
            
            // Setup text paint
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            val contentPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            
            // Draw title (adjusted Y position to account for larger logo)
            canvas.drawText("Monthly Financial Report", 50f, 180f, titlePaint)
            canvas.drawText("Generated on: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}", 50f, 210f, contentPaint)
            
            // Draw content (adjusted starting Y position for larger logo)
            val lines = reportText.split("\n")
            var y = 250f
            val lineHeight = 20f
            
            lines.forEach { line ->
                if (line.contains("Total") || line.contains("Budget") || line.contains("Categories")) {
                    y += 10f // Add extra space before sections
                    canvas.drawText(line, 50f, y, titlePaint)
                    y += lineHeight + 10f // Add extra space after section headers
                } else {
                    canvas.drawText(line, 50f, y, contentPaint)
                    y += lineHeight
                }
            }
            
            // Draw footer
            canvas.drawText("Generated by Finance Tracker App", 50f, pageInfo.pageHeight - 50f, contentPaint)
            canvas.drawText("Developed by Havindu Nimadith", pageInfo.pageWidth - 250f, pageInfo.pageHeight - 50f, contentPaint)
            
            document.finishPage(page)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore API for Android 10 and above
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    Toast.makeText(this, "PDF Report downloaded to Downloads folder", Toast.LENGTH_LONG).show()
                }
            } else {
                // Legacy approach for Android 9 and below
                if (checkStoragePermission()) {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    
                    FileOutputStream(file).use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    Toast.makeText(this, "PDF Report downloaded to Downloads folder", Toast.LENGTH_LONG).show()
                } else {
                    requestStoragePermission()
                }
            }
            
            // Close the document
            document.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading report", e)
            Toast.makeText(this, "Error downloading report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || 
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            WRITE_EXTERNAL_STORAGE_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadReport()
            } else {
                Toast.makeText(this, "Storage permission required to download report", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating back", e)
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 