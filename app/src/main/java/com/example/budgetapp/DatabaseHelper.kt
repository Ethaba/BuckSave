package com.example.budgetapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "BuckSaveDB", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT,
                firstName TEXT,
                lastName TEXT,
                username TEXT,
                password TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT,
                minGoal REAL,
                maxGoal REAL,
                notes TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT,
                name TEXT,
                amount REAL
            )
        """)

        db.execSQL("""
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT,
                amount REAL,
                date TEXT,
                startTime TEXT,
                endTime TEXT,
                category TEXT,
                description TEXT,
                receiptUri TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS budgets")
        db.execSQL("DROP TABLE IF EXISTS categories")
        db.execSQL("DROP TABLE IF EXISTS expenses")
        onCreate(db)
    }

    // ================= USER =================

    fun insertUser(
        email: String,
        firstName: String,
        lastName: String,
        username: String,
        password: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues()

        values.put("email", email)
        values.put("firstName", firstName)
        values.put("lastName", lastName)
        values.put("username", username)
        values.put("password", password)

        val result = db.insert("users", null, values)
        return result != -1L
    }

    fun loginUser(username: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE username=? AND password=?",
            arrayOf(username, password)
        )

        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getUser(username: String): Array<String>? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT email, firstName, lastName, username FROM users WHERE username=?",
            arrayOf(username)
        )

        var user: Array<String>? = null

        if (cursor.moveToFirst()) {
            user = arrayOf(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3)
            )
        }

        cursor.close()
        return user
    }

    // ================= BUDGET =================

    fun insertBudget(
        username: String,
        minGoal: Double,
        maxGoal: Double,
        notes: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues()

        values.put("username", username)
        values.put("minGoal", minGoal)
        values.put("maxGoal", maxGoal)
        values.put("notes", notes)

        val result = db.insert("budgets", null, values)
        return result != -1L
    }

    fun getLatestBudget(username: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT maxGoal FROM budgets WHERE username=? ORDER BY id DESC LIMIT 1",
            arrayOf(username)
        )

        var budget = 0.0

        if (cursor.moveToFirst()) {
            budget = cursor.getDouble(0)
        }

        cursor.close()
        return budget
    }

    fun getLatestMinGoal(username: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT minGoal FROM budgets WHERE username=? ORDER BY id DESC LIMIT 1",
            arrayOf(username)
        )

        var minGoal = 0.0

        if (cursor.moveToFirst()) {
            minGoal = cursor.getDouble(0)
        }

        cursor.close()
        return minGoal
    }

    fun hasBudget(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM budgets WHERE username=?",
            arrayOf(username)
        )

        var exists = false

        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0
        }

        cursor.close()
        return exists
    }

    // ================= CATEGORY =================

    fun insertCategory(
        username: String,
        name: String,
        amount: Double
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues()

        values.put("username", username)
        values.put("name", name)
        values.put("amount", amount)

        val result = db.insert("categories", null, values)
        return result != -1L
    }

    fun getCategories(username: String): MutableList<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT name FROM categories WHERE username=?",
            arrayOf(username)
        )

        while (cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }

        cursor.close()
        return list
    }

    // ================= EXPENSE =================

    fun insertExpense(
        username: String,
        amount: Double,
        date: String,
        startTime: String,
        endTime: String,
        category: String,
        description: String,
        receiptUri: String?
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues()

        values.put("username", username)
        values.put("amount", amount)
        values.put("date", date)
        values.put("startTime", startTime)
        values.put("endTime", endTime)
        values.put("category", category)
        values.put("description", description)
        values.put("receiptUri", receiptUri)

        val result = db.insert("expenses", null, values)
        return result != -1L
    }

    fun getTotalSpent(username: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(amount) FROM expenses WHERE username=?",
            arrayOf(username)
        )

        var total = 0.0

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        return total
    }

    fun getAllExpenses(username: String): MutableList<Array<String>> {
        val expenses = mutableListOf<Array<String>>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT amount, date, startTime, endTime, category, description, receiptUri FROM expenses WHERE username=? ORDER BY id DESC",
            arrayOf(username)
        )

        while (cursor.moveToNext()) {
            val amount = cursor.getDouble(0).toString()
            val date = cursor.getString(1)
            val startTime = cursor.getString(2)
            val endTime = cursor.getString(3)
            val category = cursor.getString(4)
            val description = cursor.getString(5)
            val receiptUri = cursor.getString(6)

            expenses.add(
                arrayOf(
                    amount,
                    date,
                    startTime,
                    endTime,
                    category,
                    description,
                    receiptUri ?: ""
                )
            )
        }

        cursor.close()
        return expenses
    }

    fun getCategoryTotals(username: String): MutableMap<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT category, SUM(amount) FROM expenses WHERE username=? GROUP BY category",
            arrayOf(username)
        )

        while (cursor.moveToNext()) {
            totals[cursor.getString(0)] = cursor.getDouble(1)
        }

        cursor.close()
        return totals
    }

    fun getExpenseCount(username: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM expenses WHERE username=?",
            arrayOf(username)
        )

        var count = 0

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }

        cursor.close()
        return count
    }

    fun isWithinBudget(username: String): Boolean {
        val budget = getLatestBudget(username)
        val spent = getTotalSpent(username)

        return budget > 0 && spent <= budget
    }

    fun getCategoryBudgets(username: String): MutableMap<String, Double> {
        val budgets = mutableMapOf<String, Double>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT name, amount FROM categories WHERE username=?",
            arrayOf(username)
        )

        while (cursor.moveToNext()) {
            budgets[cursor.getString(0)] = cursor.getDouble(1)
        }

        cursor.close()
        return budgets
    }
}