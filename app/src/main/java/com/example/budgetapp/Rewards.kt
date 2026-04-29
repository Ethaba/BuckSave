package com.example.budgetapp

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
    private var currentUsername: String? = null

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
        val tvLevel = findViewById<TextView>(R.id.tvLevel)
        val tvPoints = findViewById<TextView>(R.id.tvPoints)
        val tvNextLevel = findViewById<TextView>(R.id.tvNextLevel)
        val progressLevel = findViewById<ProgressBar>(R.id.progressLevel)
        badgeContainer = findViewById(R.id.badgeContainer)

        btnBack.setOnClickListener {
            finish()
        }

        val points = calculatePoints(username)
        val level = getLevel(points)
        val nextLevel = getNextLevel(points)
        val progress = points.coerceAtMost(100)

        tvLevel.text = "Level: $level"
        tvPoints.text = "Points: $points / 100"
        progressLevel.progress = progress

        tvNextLevel.text = if (points >= 100) {
            "You reached Saver level!"
        } else {
            "Earn ${100 - points} points to reach $nextLevel"
        }

        loadBadges(username)
    }

    private fun calculatePoints(username: String): Int {
        var points = 0

        val hasBudget = databaseHelper.hasBudget(username)
        val expenseCount = databaseHelper.getExpenseCount(username)
        val withinBudget = databaseHelper.isWithinBudget(username)

        if (hasBudget) {
            points += 20
        }

        points += expenseCount * 10

        if (expenseCount >= 5) {
            points += 20
        }

        if (withinBudget && expenseCount > 0) {
            points += 30
        }

        return points
    }

    private fun getLevel(points: Int): String {
        return when {
            points >= 100 -> "Saver"
            points >= 60 -> "Budget Builder"
            points >= 20 -> "Rookie"
            else -> "Beginner"
        }
    }

    private fun getNextLevel(points: Int): String {
        return when {
            points < 20 -> "Rookie"
            points < 60 -> "Budget Builder"
            points < 100 -> "Saver"
            else -> "Saver"
        }
    }

    private fun loadBadges(username: String) {
        badgeContainer.removeAllViews()

        val hasBudget = databaseHelper.hasBudget(username)
        val expenseCount = databaseHelper.getExpenseCount(username)
        val withinBudget = databaseHelper.isWithinBudget(username)

        addBadge("💰 Budget Starter", "Set your first monthly budget", hasBudget)
        addBadge("🏁 First Expense", "Add your first expense", expenseCount >= 1)
        addBadge("🔥 Consistent Logger", "Add at least 5 expenses", expenseCount >= 5)
        addBadge("🧠 Smart Saver", "Stay within your monthly budget", withinBudget && expenseCount > 0)
        addBadge("⭐ Saver Level", "Reach 100 points", calculatePoints(username) >= 100)
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
        titleText.setTextColor(android.graphics.Color.BLACK)
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)

        val descriptionText = TextView(this)
        descriptionText.text = description
        descriptionText.textSize = 13f
        descriptionText.gravity = Gravity.START
        descriptionText.setTextColor(android.graphics.Color.DKGRAY)

        badgeRow.addView(titleText)
        badgeRow.addView(descriptionText)

        badgeContainer.addView(badgeRow)
    }
}