package com.example.budgetapp

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SetBudget : AppCompatActivity() {

    private lateinit var categoryContainer: LinearLayout
    private lateinit var databaseHelper: DatabaseHelper

    private val categoryList = mutableListOf<Pair<String, Double>>()
    private var currentUsername: String? = null

    private val defaultCategories = mutableListOf(
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
        setContentView(R.layout.activity_set_budget)

        databaseHelper = DatabaseHelper(this)
        currentUsername = intent.getStringExtra("username")

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val etMinGoal = findViewById<EditText>(R.id.etMinGoal)
        val etMaxBudget = findViewById<EditText>(R.id.etMaxBudget)
        val spinnerCategoryName = findViewById<Spinner>(R.id.spinnerCategoryName)
        val etCategoryAmount = findViewById<EditText>(R.id.etCategoryAmount)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val btnAddCategory = findViewById<Button>(R.id.btnAddCategory)
        val btnSaveBudget = findViewById<Button>(R.id.btnSaveBudget)

        categoryContainer = findViewById(R.id.categoryContainer)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            defaultCategories
        )
        spinnerCategoryName.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        btnAddCategory.setOnClickListener {
            val name = spinnerCategoryName.selectedItem.toString()
            val amountText = etCategoryAmount.text.toString().trim()

            if (amountText.isEmpty()) {
                etCategoryAmount.error = "Enter category amount"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                etCategoryAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            val alreadyExists = categoryList.any {
                it.first.equals(name, ignoreCase = true)
            }

            if (alreadyExists) {
                Toast.makeText(this, "$name already added", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            categoryList.add(Pair(name, amount))
            addCategoryRow(name, amount)

            etCategoryAmount.text.clear()
        }

        btnSaveBudget.setOnClickListener {
            val username = currentUsername

            if (username.isNullOrEmpty()) {
                Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minGoalText = etMinGoal.text.toString().trim()
            val maxGoalText = etMaxBudget.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (minGoalText.isEmpty()) {
                etMinGoal.error = "Enter minimum monthly goal"
                return@setOnClickListener
            }

            if (maxGoalText.isEmpty()) {
                etMaxBudget.error = "Enter maximum monthly goal"
                return@setOnClickListener
            }

            val minGoal = minGoalText.toDoubleOrNull()
            val maxGoal = maxGoalText.toDoubleOrNull()

            if (minGoal == null || minGoal < 0) {
                etMinGoal.error = "Enter a valid minimum goal"
                return@setOnClickListener
            }

            if (maxGoal == null || maxGoal <= 0) {
                etMaxBudget.error = "Enter a valid maximum goal"
                return@setOnClickListener
            }

            if (minGoal > maxGoal) {
                etMinGoal.error = "Minimum goal cannot be greater than maximum goal"
                return@setOnClickListener
            }

            if (categoryList.isEmpty()) {
                Toast.makeText(this, "Please add at least one category budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val budgetSaved = databaseHelper.insertBudget(username, minGoal, maxGoal, notes)

            databaseHelper.clearCategories(username)

            var allCategoriesSaved = true

            for (category in categoryList) {
                val saved = databaseHelper.insertCategory(username, category.first, category.second)

                if (!saved) {
                    allCategoriesSaved = false
                }
            }

            if (budgetSaved && allCategoriesSaved) {
                Toast.makeText(this, "Budget goals and categories saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save budget", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addCategoryRow(categoryName: String, amount: Double) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, 10, 0, 10)

        val nameText = TextView(this)
        nameText.text = "$categoryName - R${amount.toInt()}"
        nameText.textSize = 15f
        nameText.setTextColor(android.graphics.Color.BLACK)

        val nameParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        nameText.layoutParams = nameParams

        val editButton = Button(this)
        editButton.text = "Edit"
        editButton.textSize = 11f
        editButton.setBackgroundColor(android.graphics.Color.rgb(0, 80, 184))
        editButton.setTextColor(android.graphics.Color.WHITE)

        val editParams = LinearLayout.LayoutParams(120, 80)
        editParams.setMargins(4, 0, 4, 0)
        editButton.layoutParams = editParams

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        deleteButton.textSize = 11f
        deleteButton.setBackgroundColor(android.graphics.Color.rgb(220, 53, 69))
        deleteButton.setTextColor(android.graphics.Color.WHITE)

        val deleteParams = LinearLayout.LayoutParams(140, 80)
        deleteParams.setMargins(4, 0, 0, 0)
        deleteButton.layoutParams = deleteParams

        editButton.setOnClickListener {
            val spinner = findViewById<Spinner>(R.id.spinnerCategoryName)
            val categoryIndex = defaultCategories.indexOf(categoryName)

            if (categoryIndex >= 0) {
                spinner.setSelection(categoryIndex)
            }

            findViewById<EditText>(R.id.etCategoryAmount).setText(amount.toString())

            categoryList.remove(Pair(categoryName, amount))
            categoryContainer.removeView(row)
        }

        deleteButton.setOnClickListener {
            categoryList.remove(Pair(categoryName, amount))
            categoryContainer.removeView(row)
        }

        row.addView(nameText)
        row.addView(editButton)
        row.addView(deleteButton)

        categoryContainer.addView(row)
    }
}