package com.example.budgetapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Dashboard : AppCompatActivity() {

    private var currentUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        currentUsername = intent.getStringExtra("username")

        val navReports = findViewById<TextView>(R.id.navReports)
        val navRewards = findViewById<TextView>(R.id.navRewards)
        val navProfile = findViewById<TextView>(R.id.navProfile)

        val btnSetBudget = findViewById<Button>(R.id.btnSetBudget)
        val btnAddExpense = findViewById<Button>(R.id.btnAddExpense)
        val btnViewExpenses = findViewById<Button>(R.id.btnViewExpenses)
        val btnInsights = findViewById<Button>(R.id.btnInsights)

        btnSetBudget.setOnClickListener {
            val intent = Intent(this, SetBudget::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        btnAddExpense.setOnClickListener {
            val intent = Intent(this, AddExpense::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        btnViewExpenses.setOnClickListener {
            val intent = Intent(this, ExpenseList::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        btnInsights.setOnClickListener {
            val intent = Intent(this, Insights::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        navReports.setOnClickListener {
            val intent = Intent(this, Reports::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        navRewards.setOnClickListener {
            val intent = Intent(this, Rewards::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            intent.putExtra("username", currentUsername)
            startActivity(intent)
        }

        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val username = currentUsername

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val tvTotalBudget = findViewById<TextView>(R.id.tvTotalBudget)
        val tvMinGoal = findViewById<TextView>(R.id.tvMinGoal)
        val tvRemaining = findViewById<TextView>(R.id.tvRemaining)
        val tvUsed = findViewById<TextView>(R.id.tvUsed)
        val tvProgressPercent = findViewById<TextView>(R.id.tvProgressPercent)
        val progressBudget = findViewById<ProgressBar>(R.id.progressBudget)
        val tvUserLevel = findViewById<TextView>(R.id.tvUserLevel)

        val tvFoodPercent = findViewById<TextView>(R.id.tvFoodPercent)
        val tvTransportPercent = findViewById<TextView>(R.id.tvTransportPercent)
        val tvRentPercent = findViewById<TextView>(R.id.tvRentPercent)
        val tvOtherPercent = findViewById<TextView>(R.id.tvOtherPercent)

        val databaseHelper = DatabaseHelper(this)

        val totalBudget = databaseHelper.getLatestBudget(username)
        val minGoal = databaseHelper.getLatestMinGoal(username)
        val usedAmount = databaseHelper.getTotalSpent(username)
        val remainingAmount = totalBudget - usedAmount

        val percentageUsed = if (totalBudget > 0) {
            ((usedAmount / totalBudget) * 100).toInt()
        } else {
            0
        }

        tvTotalBudget.text = "Total Budget: R${totalBudget.toInt()}"
        tvMinGoal.text = "Minimum Goal: R${minGoal.toInt()}"
        tvRemaining.text = "Remaining: R${remainingAmount.toInt()}"
        tvUsed.text = "Used: R${usedAmount.toInt()}"
        tvProgressPercent.text = "$percentageUsed%"
        progressBudget.progress = percentageUsed.coerceAtMost(100)

        // Change remaining text color based on balance
        if (remainingAmount < 0) {
            tvRemaining.setTextColor(android.graphics.Color.RED)
        } else {
            tvRemaining.setTextColor(android.graphics.Color.parseColor("#008A45"))
        }

        if (usedAmount < minGoal) {

            progressBudget.progressDrawable =
                getDrawable(R.drawable.progress_budget_orange)

        }
        else if (usedAmount > totalBudget) {

            progressBudget.progressDrawable =
                getDrawable(R.drawable.progress_budget_red)

        }
        else {

            progressBudget.progressDrawable =
                getDrawable(R.drawable.progress_budget)

        }

        val categoryTotals = databaseHelper.getCategoryTotals(username)
        val categoryBudgets = databaseHelper.getCategoryBudgets(username)

        val foodSpent = categoryTotals["Food"] ?: 0.0
        val transportSpent = categoryTotals["Transport"] ?: 0.0
        val rentSpent = categoryTotals["Rent"] ?: 0.0

        val foodBudget = categoryBudgets["Food"] ?: 0.0
        val transportBudget = categoryBudgets["Transport"] ?: 0.0
        val rentBudget = categoryBudgets["Rent"] ?: 0.0

        var otherSpent = 0.0
        var otherBudget = 0.0

        for ((category, total) in categoryTotals) {
            if (category != "Food" && category != "Transport" && category != "Rent") {
                otherSpent += total
            }
        }

        for ((category, budget) in categoryBudgets) {
            if (category != "Food" && category != "Transport" && category != "Rent") {
                otherBudget += budget
            }
        }

        val foodPercent = calculateCategoryPercent(foodSpent, foodBudget)
        val transportPercent = calculateCategoryPercent(transportSpent, transportBudget)
        val rentPercent = calculateCategoryPercent(rentSpent, rentBudget)
        val otherPercent = calculateCategoryPercent(otherSpent, otherBudget)

        tvFoodPercent.text = "$foodPercent%"
        tvTransportPercent.text = "$transportPercent%"
        tvRentPercent.text = "$rentPercent%"
        tvOtherPercent.text = "$otherPercent%"

        tvFoodPercent.setTextColor(
            if (foodSpent > foodBudget && foodBudget > 0) android.graphics.Color.RED
            else android.graphics.Color.rgb(46, 66, 125)
        )

        tvTransportPercent.setTextColor(
            if (transportSpent > transportBudget && transportBudget > 0) android.graphics.Color.RED
            else android.graphics.Color.rgb(46, 66, 125)
        )

        tvRentPercent.setTextColor(
            if (rentSpent > rentBudget && rentBudget > 0) android.graphics.Color.RED
            else android.graphics.Color.rgb(46, 66, 125)
        )

        tvOtherPercent.setTextColor(
            if (otherSpent > otherBudget && otherBudget > 0) android.graphics.Color.RED
            else android.graphics.Color.rgb(46, 66, 125)
        )

        val points = calculatePoints(username)
        val level = getLevel(points)

        tvUserLevel.text = level
    }

    private fun calculateCategoryPercent(categorySpent: Double, categoryBudget: Double): Int {
        return if (categoryBudget > 0) {
            ((categorySpent / categoryBudget) * 100).toInt()
        } else {
            0
        }
    }

    private fun calculatePoints(username: String): Int {
        val databaseHelper = DatabaseHelper(this)
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

        return points.coerceAtMost(150)
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
}