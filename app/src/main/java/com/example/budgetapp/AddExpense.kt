package com.example.budgetapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AddExpense : AppCompatActivity() {

    private var receiptImageUri: Uri? = null
    private lateinit var imgReceiptPreview: ImageView
    private lateinit var databaseHelper: DatabaseHelper
    private var currentUsername: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptImageUri = uri
            imgReceiptPreview.setImageURI(uri)
            imgReceiptPreview.visibility = View.VISIBLE
            Toast.makeText(this, "Receipt added", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        databaseHelper = DatabaseHelper(this)
        currentUsername = intent.getStringExtra("username")

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etDate = findViewById<EditText>(R.id.etDate)
        val etStartTime = findViewById<EditText>(R.id.etStartTime)
        val etEndTime = findViewById<EditText>(R.id.etEndTime)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerExpenseCategory)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnUploadReceipt = findViewById<Button>(R.id.btnUploadReceipt)
        val btnSaveExpense = findViewById<Button>(R.id.btnSaveExpense)

        imgReceiptPreview = findViewById(R.id.imgReceiptPreview)

        val username = currentUsername

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val categories = databaseHelper.getCategories(username)

        if (categories.isEmpty()) {
            categories.add("Food")
            categories.add("Transport")
            categories.add("Rent")
            categories.add("Other")
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spinnerCategory.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        etDate.setOnClickListener {
            showDatePicker(etDate)
        }

        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }

        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }

        btnUploadReceipt.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSaveExpense.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            val date = etDate.text.toString().trim()
            val startTime = etStartTime.text.toString().trim()
            val endTime = etEndTime.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val description = etDescription.text.toString().trim()

            if (amountText.isEmpty()) {
                etAmount.error = "Enter amount"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                etAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            if (date.isEmpty()) {
                etDate.error = "Select date"
                return@setOnClickListener
            }

            if (startTime.isEmpty()) {
                etStartTime.error = "Select start time"
                return@setOnClickListener
            }

            if (endTime.isEmpty()) {
                etEndTime.error = "Select end time"
                return@setOnClickListener
            }

            val saved = databaseHelper.insertExpense(
                username,
                amount,
                date,
                startTime,
                endTime,
                category,
                description,
                receiptImageUri?.toString()
            )

            if (saved) {
                Toast.makeText(this, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(dateField: EditText) {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val finalDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dateField.setText(finalDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun showTimePicker(timeField: EditText) {
        val calendar = Calendar.getInstance()

        val timePicker = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val finalTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                timeField.setText(finalTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )

        timePicker.show()
    }
}