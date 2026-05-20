package com.example.budgetapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Rewards : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var badgeContainer: LinearLayout
    private lateinit var pointsContainer: LinearLayout
    private var currentUsername: String? = null

    private val maxPoints = 150

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        databaseHelper = DatabaseHelper(this)
        currentUsername = intent.getStringExtra("username")

        val username = currentUsername

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvLevelIcon = findViewById<TextView>(R.id.tvLevelIcon)
        val tvLevel = findViewById<TextView>(R.id.tvLevel)
        val tvPoints = findViewById<TextView>(R.id.tvPoints)
        val tvNextLevel = findViewById<TextView>(R.id.tvNextLevel)
        val progressLevel = findViewById<ProgressBar>(R.id.progressLevel)

        badgeContainer = findViewById(R.id.badgeContainer)
        pointsContainer = findViewById(R.id.pointsContainer)

        btnBack.setOnClickListener {
            finish()
        }

        val points = calculatePoints(username)
        val level = getLevel(points)

        tvLevelIcon.text = getLevelIcon(points)
        tvLevel.text = "Level: $level"
        tvPoints.text = "Points: $points / $maxPoints"
        progressLevel.max = maxPoints
        progressLevel.progress = points.coerceAtMost(maxPoints)

        tvNextLevel.text = getNextLevelMessage(points)

        loadPointsBreakdown(username)
        loadBadges(username)
    }

    private fun calculatePoints(username: String): Int {
        var points = 0

        val hasBudget = databaseHelper.hasBudget(username)
        val expenseCount = databaseHelper.getExpenseCount(username)
        val withinBudget = databaseHelper.isWithinBudget(username)
        val categoryBudgets = databaseHelper.getCategoryBudgets(username)
        val categoryTotals = databaseHelper.getCategoryTotals(username)

        if (hasBudget) points += 20

        points += (expenseCount * 8).coerceAtMost(40)

        if (expenseCount >= 5) points += 15
        if (expenseCount >= 10) points += 20

        if (categoryBudgets.isNotEmpty()) points += 15

        if (withinBudget && expenseCount > 0) points += 30

        if (isAllCategoriesWithinBudget(categoryBudgets, categoryTotals)) {
            points += 30
        }

        return points.coerceAtMost(maxPoints)
    }

    private fun isAllCategoriesWithinBudget(
        categoryBudgets: Map<String, Double>,
        categoryTotals: Map<String, Double>
    ): Boolean {
        if (categoryBudgets.isEmpty() || categoryTotals.isEmpty()) {
            return false
        }

        for ((category, spent) in categoryTotals) {
            val budget = categoryBudgets[category] ?: 0.0

            if (budget > 0 && spent > budget) {
                return false
            }
        }

        return true
    }

    private fun getLevel(points: Int): String {
        return when {
            points >= 130 -> "Budget Master"
            points >= 100 -> "Saver"
            points >= 60 -> "Budget Builder"
            points >= 20 -> "Rookie"
            else -> "Beginner"
        }
    }

    private fun getLevelIcon(points: Int): String {
        return when {
            points >= 130 -> "👑"
            points >= 100 -> "⭐"
            points >= 60 -> "📈"
            points >= 20 -> "🌱"
            else -> "🔰"
        }
    }

    private fun getNextLevelMessage(points: Int): String {
        return when {
            points < 20 -> "Earn ${20 - points} more points to reach Rookie"
            points < 60 -> "Earn ${60 - points} more points to reach Budget Builder"
            points < 100 -> "Earn ${100 - points} more points to reach Saver"
            points < 130 -> "Earn ${130 - points} more points to reach Budget Master"
            else -> "You have reached the highest level!"
        }
    }

    private fun loadPointsBreakdown(username: String) {
        pointsContainer.removeAllViews()

        val hasBudget = databaseHelper.hasBudget(username)
        val expenseCount = databaseHelper.getExpenseCount(username)
        val withinBudget = databaseHelper.isWithinBudget(username)
        val categoryBudgets = databaseHelper.getCategoryBudgets(username)
        val categoryTotals = databaseHelper.getCategoryTotals(username)

        val expensePoints = (expenseCount * 8).coerceAtMost(40)

        addPointRow("Budget setup", if (hasBudget) 20 else 0, "Set minimum and maximum goals")
        addPointRow("Expense logging", expensePoints, "Earn up to 40 points for adding expenses")
        addPointRow("5 expense streak", if (expenseCount >= 5) 15 else 0, "Add at least 5 expenses")
        addPointRow("10 expense streak", if (expenseCount >= 10) 20 else 0, "Add at least 10 expenses")
        addPointRow("Category planning", if (categoryBudgets.isNotEmpty()) 15 else 0, "Set category budgets")
        addPointRow("Monthly control", if (withinBudget && expenseCount > 0) 30 else 0, "Stay within maximum goal")

        val categoryControlPoints =
            if (isAllCategoriesWithinBudget(categoryBudgets, categoryTotals)) 30 else 0

        addPointRow("Category control", categoryControlPoints, "Keep categories within budget")
    }

    private fun addPointRow(title: String, points: Int, description: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(0, 8, 0, 8)

        val topLine = TextView(this)
        topLine.text = "$title  •  +$points pts"
        topLine.textSize = 14f
        topLine.setTextColor(if (points > 0) Color.rgb(0, 138, 69) else Color.DKGRAY)
        topLine.setTypeface(null, android.graphics.Typeface.BOLD)

        val desc = TextView(this)
        desc.text = description
        desc.textSize = 12f
        desc.setTextColor(Color.DKGRAY)

        row.addView(topLine)
        row.addView(desc)

        pointsContainer.addView(row)
    }

    private fun loadBadges(username: String) {
        badgeContainer.removeAllViews()

        val hasBudget = databaseHelper.hasBudget(username)
        val expenseCount = databaseHelper.getExpenseCount(username)
        val withinBudget = databaseHelper.isWithinBudget(username)
        val categoryBudgets = databaseHelper.getCategoryBudgets(username)
        val categoryTotals = databaseHelper.getCategoryTotals(username)
        val points = calculatePoints(username)

        addBadge("💰 Budget Starter", "Set your first minimum and maximum budget goals", hasBudget)
        addBadge("📂 Category Planner", "Create budgets for spending categories", categoryBudgets.isNotEmpty())
        addBadge("🏁 First Expense", "Add your first expense record", expenseCount >= 1)
        addBadge("🔥 Consistent Logger", "Add at least 5 expenses", expenseCount >= 5)
        addBadge("⚡ Power Tracker", "Add at least 10 expenses", expenseCount >= 10)
        addBadge("🧠 Smart Saver", "Stay within your maximum monthly goal", withinBudget && expenseCount > 0)
        addBadge(
            "🛡️ Category Controller",
            "Keep all spending categories within their planned budgets",
            isAllCategoriesWithinBudget(categoryBudgets, categoryTotals)
        )
        addBadge("⭐ Saver Level", "Reach 100 points", points >= 100)
        addBadge("👑 Budget Master", "Reach 130 points", points >= 130)
    }

    private fun addBadge(title: String, description: String, earned: Boolean) {
        val badgeRow = LinearLayout(this)
        badgeRow.orientation = LinearLayout.VERTICAL
        badgeRow.setPadding(14, 12, 14, 12)
        badgeRow.setBackgroundResource(R.drawable.expense_row_bg)

        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.setMargins(0, 0, 0, 10)
        badgeRow.layoutParams = rowParams

        val titleText = TextView(this)
        titleText.text = if (earned) "$title  • Earned" else "$title  • Locked"
        titleText.textSize = 15f
        titleText.setTextColor(if (earned) Color.rgb(0, 138, 69) else Color.DKGRAY)
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)

        val descriptionText = TextView(this)
        descriptionText.text = description
        descriptionText.textSize = 13f
        descriptionText.gravity = Gravity.START
        descriptionText.setTextColor(Color.DKGRAY)

        badgeRow.addView(titleText)
        badgeRow.addView(descriptionText)

        badgeContainer.addView(badgeRow)
    }
}