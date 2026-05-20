package com.example.budgetapp

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Insights : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var categoryInsightContainer: LinearLayout
    private var currentUsername: String? = null

    private val defaultCategories = listOf(
        "Food",
        "Transport",
        "Rent",
        "Groceries",
        "Entertainment",
        "Utilities",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)

        databaseHelper = DatabaseHelper(this)
        currentUsername = intent.getStringExtra("username")

        val username = currentUsername

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvHealthScore = findViewById<TextView>(R.id.tvHealthScore)
        val progressHealth = findViewById<ProgressBar>(R.id.progressHealth)
        val tvMainInsight = findViewById<TextView>(R.id.tvMainInsight)

        categoryInsightContainer = findViewById(R.id.categoryInsightContainer)

        btnBack.setOnClickListener {
            finish()
        }

        loadInsights(username, tvHealthScore, progressHealth, tvMainInsight)
    }

    private fun loadInsights(
        username: String,
        tvHealthScore: TextView,
        progressHealth: ProgressBar,
        tvMainInsight: TextView
    ) {
        val maxGoal = databaseHelper.getLatestBudget(username)
        val minGoal = databaseHelper.getLatestMinGoal(username)
        val totalSpent = databaseHelper.getTotalSpent(username)
        val categoryBudgets = databaseHelper.getCategoryBudgets(username)
        val categoryTotals = databaseHelper.getCategoryTotals(username)

        val score = calculateHealthScore(maxGoal, minGoal, totalSpent, categoryBudgets, categoryTotals)

        tvHealthScore.text = "Budget Health: $score/100"
        progressHealth.progress = score

        when {
            maxGoal == 0.0 -> {
                tvMainInsight.text = "Set your monthly budget goals to receive spending insights."
                tvMainInsight.setTextColor(Color.rgb(0, 80, 184))
            }

            totalSpent > maxGoal -> {
                tvMainInsight.text = "Warning: You have exceeded your maximum monthly goal."
                tvMainInsight.setTextColor(Color.RED)
            }

            totalSpent < minGoal -> {
                tvMainInsight.text = "You are currently below your minimum spending goal."
                tvMainInsight.setTextColor(Color.rgb(255, 140, 0))
            }

            else -> {
                tvMainInsight.text = "Good job. You are spending within your planned monthly range."
                tvMainInsight.setTextColor(Color.rgb(0, 138, 69))
            }
        }

        loadCategoryInsights(categoryBudgets, categoryTotals)
    }

    private fun calculateHealthScore(
        maxGoal: Double,
        minGoal: Double,
        totalSpent: Double,
        categoryBudgets: Map<String, Double>,
        categoryTotals: Map<String, Double>
    ): Int {
        if (maxGoal <= 0) return 0

        var score = 100

        if (totalSpent > maxGoal) {
            score -= 35
        }

        if (totalSpent < minGoal) {
            score -= 15
        }

        for ((category, spent) in categoryTotals) {
            val budget = categoryBudgets[category] ?: 0.0

            if (budget > 0 && spent > budget) {
                score -= 10
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun loadCategoryInsights(
        categoryBudgets: Map<String, Double>,
        categoryTotals: Map<String, Double>
    ) {
        categoryInsightContainer.removeAllViews()

        for (category in defaultCategories) {
            val budget = categoryBudgets[category] ?: 0.0
            val spent = categoryTotals[category] ?: 0.0

            val status = when {
                budget == 0.0 && spent == 0.0 -> "No budget set"
                budget == 0.0 && spent > 0.0 -> "No budget set, but spending exists"
                spent > budget -> "Overspent by R${(spent - budget).toInt()}"
                spent == budget && budget > 0 -> "Perfectly on budget"
                spent < budget && spent > 0 -> "Healthy"
                else -> "No spending yet"
            }

            val color = when {
                status.startsWith("Overspent") -> Color.RED
                status == "Healthy" || status == "Perfectly on budget" -> Color.rgb(0, 138, 69)
                status.contains("No budget") -> Color.rgb(255, 140, 0)
                else -> Color.DKGRAY
            }

            addCategoryInsightRow(category, budget, spent, status, color)
        }
    }

    private fun addCategoryInsightRow(
        category: String,
        budget: Double,
        spent: Double,
        status: String,
        color: Int
    ) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(14, 12, 14, 12)
        row.setBackgroundResource(R.drawable.expense_row_bg)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 10)
        row.layoutParams = params

        val title = TextView(this)
        title.text = "$category: R${spent.toInt()} / R${budget.toInt()}"
        title.textSize = 15f
        title.setTextColor(Color.BLACK)
        title.setTypeface(null, android.graphics.Typeface.BOLD)

        val statusText = TextView(this)
        statusText.text = status
        statusText.textSize = 13f
        statusText.setTextColor(color)

        row.addView(title)
        row.addView(statusText)

        categoryInsightContainer.addView(row)
    }
}