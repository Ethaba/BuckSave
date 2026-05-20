package com.example.budgetapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Reports : AppCompatActivity() {

    private lateinit var breakdownContainer: LinearLayout
    private lateinit var databaseHelper: DatabaseHelper
    private var currentUsername: String? = null

    private val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        databaseHelper = DatabaseHelper(this)
        currentUsername = intent.getStringExtra("username")

        val username = currentUsername

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val etStartDate = findViewById<EditText>(R.id.etStartDate)
        val etEndDate = findViewById<EditText>(R.id.etEndDate)
        val btnGenerateReport = findViewById<Button>(R.id.btnGenerateReport)
        val tvTotalSpent = findViewById<TextView>(R.id.tvTotalSpent)
        val tvReportInsight = findViewById<TextView>(R.id.tvReportInsight)
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val tvMinGoal = findViewById<TextView>(R.id.tvMinGoal)
        val tvMaxGoal = findViewById<TextView>(R.id.tvMaxGoal)

        breakdownContainer = findViewById(R.id.breakdownContainer)

        setupEmptyChart(pieChart)

        btnBack.setOnClickListener { finish() }

        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }

        showEmptyBreakdown(tvTotalSpent, pieChart)

        btnGenerateReport.setOnClickListener {
            val startDateText = etStartDate.text.toString().trim()
            val endDateText = etEndDate.text.toString().trim()

            if (startDateText.isEmpty()) {
                etStartDate.error = "Select start date"
                return@setOnClickListener
            }

            if (endDateText.isEmpty()) {
                etEndDate.error = "Select end date"
                return@setOnClickListener
            }

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

            generateReport(
                username,
                startDateText,
                endDateText,
                tvTotalSpent,
                tvReportInsight,
                tvMinGoal,
                tvMaxGoal,
                pieChart
            )
        }
    }

    private fun generateReport(
        username: String,
        startDateText: String,
        endDateText: String,
        tvTotalSpent: TextView,
        tvReportInsight: TextView,
        tvMinGoal: TextView,
        tvMaxGoal: TextView,
        pieChart: PieChart
    ) {
        breakdownContainer.removeAllViews()

        val startDate = dateFormat.parse(startDateText)
        val endDate = dateFormat.parse(endDateText)
        val expenses = databaseHelper.getAllExpenses(username)

        val categoryTotals = mutableMapOf<String, Double>()
        var totalSpent = 0.0

        for (expense in expenses) {
            val amount = expense[0].toDoubleOrNull() ?: 0.0
            val expenseDateText = expense[1]
            val category = expense[4]
            val expenseDate = dateFormat.parse(expenseDateText)

            if (expenseDate != null && startDate != null && endDate != null) {
                if (!expenseDate.before(startDate) && !expenseDate.after(endDate)) {
                    totalSpent += amount
                    categoryTotals[category] = (categoryTotals[category] ?: 0.0) + amount
                }
            }
        }

        tvTotalSpent.text = "Total Spent: R${totalSpent.toInt()}"

        val minGoal = databaseHelper.getLatestMinGoal(username)
        val maxGoal = databaseHelper.getLatestBudget(username)
        tvMinGoal.text = "Minimum Goal: R${minGoal.toInt()}"
        tvMaxGoal.text = "Maximum Goal: R${maxGoal.toInt()}"

        when {
            maxGoal == 0.0 -> {
                tvReportInsight.text = "Set your budget to get insights"
                tvReportInsight.setTextColor(Color.rgb(0, 80, 184))
            }

            totalSpent < minGoal -> {
                tvReportInsight.text = "You are below your minimum spending goal"
                tvReportInsight.setTextColor(Color.rgb(255, 140, 0))
            }

            totalSpent > maxGoal -> {
                tvReportInsight.text = "You exceeded your maximum budget"
                tvReportInsight.setTextColor(Color.RED)
            }

            else -> {
                tvReportInsight.text = "Good job! Your spending is within your planned range"
                tvReportInsight.setTextColor(Color.rgb(0, 138, 69))
            }
        }

        if (categoryTotals.isEmpty()) {
            showMessage("No expenses found for this period")
            setupEmptyChart(pieChart)
            return
        }

        for ((category, amount) in categoryTotals) {
            addBreakdownRow(category, amount)
        }

        setupPieChart(pieChart, categoryTotals)

        Toast.makeText(this, "Report generated", Toast.LENGTH_SHORT).show()
    }

    private fun setupPieChart(pieChart: PieChart, categoryTotals: Map<String, Double>) {
        val entries = ArrayList<PieEntry>()

        for ((category, amount) in categoryTotals) {
            entries.add(PieEntry(amount.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "")

        dataSet.colors = listOf(
            Color.rgb(0, 80, 184),
            Color.rgb(0, 138, 69),
            Color.rgb(255, 140, 0),
            Color.rgb(220, 53, 69),
            Color.rgb(128, 90, 213),
            Color.rgb(0, 150, 136),
            Color.rgb(96, 96, 96)
        )

        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 13f
        dataSet.sliceSpace = 2f

        val pieData = PieData(dataSet)

        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R${value.toInt()}"
            }
        })

        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = "Spending"
        pieChart.setCenterTextSize(16f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(11f)
        pieChart.legend.isEnabled = true
        pieChart.legend.textSize = 12f
        pieChart.legend.formSize = 10f
        pieChart.legend.formToTextSpace = 8f
        pieChart.legend.xEntrySpace = 16f
        pieChart.legend.yEntrySpace = 8f
        pieChart.setUsePercentValues(false)
        pieChart.animateY(800)
        pieChart.invalidate()
    }

    private fun setupEmptyChart(pieChart: PieChart) {
        pieChart.clear()
        pieChart.description.isEnabled = false
        pieChart.centerText = ""
        pieChart.setNoDataText("Generate a report to view chart")
        pieChart.setNoDataTextColor(Color.GRAY)
        pieChart.invalidate()
    }

    private fun showEmptyBreakdown(tvTotalSpent: TextView, pieChart: PieChart) {
        tvTotalSpent.text = "Total Spent: R0"
        breakdownContainer.removeAllViews()
        showMessage("Select a period and generate report")
        setupEmptyChart(pieChart)
    }

    private fun showMessage(message: String) {
        breakdownContainer.removeAllViews()

        val text = TextView(this)
        text.text = message
        text.textSize = 14f
        text.gravity = Gravity.CENTER
        text.setTextColor(Color.DKGRAY)

        breakdownContainer.addView(text)
    }

    private fun addBreakdownRow(category: String, amount: Double) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, 8, 0, 8)

        val categoryText = TextView(this)
        categoryText.text = category
        categoryText.textSize = 15f
        categoryText.setTextColor(Color.BLACK)

        val categoryParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        categoryText.layoutParams = categoryParams

        val amountText = TextView(this)
        amountText.text = "R${amount.toInt()}"
        amountText.textSize = 15f
        amountText.setTextColor(Color.BLACK)
        amountText.setTypeface(null, android.graphics.Typeface.BOLD)

        row.addView(categoryText)
        row.addView(amountText)

        breakdownContainer.addView(row)
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
}