package com.example.budgetapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseList : AppCompatActivity() {

    private lateinit var expenseContainer: LinearLayout
    private var currentUsername: String? = null

    private val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    data class ExpenseItem(
        val category: String,
        val amount: Double,
        val date: String,
        val description: String,
        val receiptUri: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        currentUsername = intent.getStringExtra("username")

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val etStartDate = findViewById<EditText>(R.id.etStartDate)
        val etEndDate = findViewById<EditText>(R.id.etEndDate)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerFilterCategory)
        val btnApplyFilter = findViewById<Button>(R.id.btnApplyFilter)

        expenseContainer = findViewById(R.id.expenseContainer)

        val username = currentUsername

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val databaseHelper = DatabaseHelper(this)
        val categories = databaseHelper.getCategories(username)

        categories.add(0, "All Categories")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spinnerCategory.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        etStartDate.setOnClickListener {
            showDatePicker(etStartDate)
        }

        etEndDate.setOnClickListener {
            showDatePicker(etEndDate)
        }

        loadExpensesFromDatabase(username)

        btnApplyFilter.setOnClickListener {
            val startDateText = etStartDate.text.toString().trim()
            val endDateText = etEndDate.text.toString().trim()
            val selectedCategory = spinnerCategory.selectedItem.toString()

            if (startDateText.isNotEmpty() && endDateText.isEmpty()) {
                etEndDate.error = "Select end date"
                return@setOnClickListener
            }

            if (startDateText.isEmpty() && endDateText.isNotEmpty()) {
                etStartDate.error = "Select start date"
                return@setOnClickListener
            }

            if (startDateText.isNotEmpty() && endDateText.isNotEmpty()) {
                val startDate = dateFormat.parse(startDateText)
                val endDate = dateFormat.parse(endDateText)

                if (startDate == null || endDate == null) {
                    Toast.makeText(this, "Invalid date selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (startDate.after(endDate)) {
                    Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            loadExpensesFromDatabase(
                username,
                selectedCategory,
                startDateText,
                endDateText
            )
        }
    }

    private fun loadExpensesFromDatabase(
        username: String,
        selectedCategory: String = "All Categories",
        startDateText: String = "",
        endDateText: String = ""
    ) {
        val databaseHelper = DatabaseHelper(this)
        val expensesFromDb = databaseHelper.getAllExpenses(username)

        expenseContainer.removeAllViews()

        if (expensesFromDb.isEmpty()) {
            showEmptyMessage("No expense records found")
            return
        }

        val startDate = if (startDateText.isNotEmpty()) dateFormat.parse(startDateText) else null
        val endDate = if (endDateText.isNotEmpty()) dateFormat.parse(endDateText) else null

        var foundAny = false

        for (expense in expensesFromDb) {
            val amount = expense[0]
            val date = expense[1]
            val startTime = expense[2]
            val endTime = expense[3]
            val category = expense[4]
            val description = expense[5]
            val receiptUri = expense[6]

            if (selectedCategory != "All Categories" && category != selectedCategory) {
                continue
            }

            if (startDate != null && endDate != null) {
                val expenseDate = dateFormat.parse(date)

                if (expenseDate == null || expenseDate.before(startDate) || expenseDate.after(endDate)) {
                    continue
                }
            }

            foundAny = true

            val item = ExpenseItem(
                category,
                amount.toDouble(),
                date,
                "$description ($startTime - $endTime)",
                if (receiptUri.isEmpty()) null else receiptUri
            )

            addExpenseRow(item)
        }

        if (!foundAny) {
            showEmptyMessage("No expense records found for the selected filters")
        }
    }

    private fun showEmptyMessage(message: String) {
        expenseContainer.removeAllViews()

        val emptyText = TextView(this)
        emptyText.text = message
        emptyText.textSize = 14f
        emptyText.gravity = Gravity.CENTER
        emptyText.setTextColor(android.graphics.Color.BLACK)

        expenseContainer.addView(emptyText)
    }

    private fun addExpenseRow(expense: ExpenseItem) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(14, 12, 14, 12)
        row.setBackgroundResource(R.drawable.expense_row_bg)

        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.setMargins(0, 0, 0, 10)
        row.layoutParams = rowParams

        val topLine = TextView(this)
        topLine.text = "${expense.category} - R${expense.amount.toInt()} - ${expense.date}"
        topLine.textSize = 15f
        topLine.setTextColor(android.graphics.Color.BLACK)
        topLine.setTypeface(null, android.graphics.Typeface.BOLD)

        val descriptionLine = TextView(this)
        descriptionLine.text = expense.description
        descriptionLine.textSize = 13f
        descriptionLine.setTextColor(android.graphics.Color.DKGRAY)

        val receiptLine = TextView(this)

        if (expense.receiptUri != null) {
            receiptLine.text = "Click to View Receipt"
            receiptLine.textSize = 13f
            receiptLine.setTextColor(android.graphics.Color.rgb(0, 80, 184))

            receiptLine.setOnClickListener {
                val intent = Intent(this, ViewReceipt::class.java)
                intent.putExtra("imageUri", expense.receiptUri)
                startActivity(intent)
            }
        } else {
            receiptLine.text = "No receipt attached"
            receiptLine.textSize = 13f
            receiptLine.setTextColor(android.graphics.Color.GRAY)
        }

        row.addView(topLine)
        row.addView(descriptionLine)
        row.addView(receiptLine)

        expenseContainer.addView(row)
    }

    private fun showDatePicker(dateField: EditText) {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                dateField.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }
}