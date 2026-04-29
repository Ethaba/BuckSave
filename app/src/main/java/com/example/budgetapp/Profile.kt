package com.example.budgetapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Profile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvProfileInitial = findViewById<TextView>(R.id.tvProfileInitial)
        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvFirstName = findViewById<TextView>(R.id.tvFirstName)
        val tvLastName = findViewById<TextView>(R.id.tvLastName)
        val tvUsername = findViewById<TextView>(R.id.tvUsername)

        btnBack.setOnClickListener {
            finish()
        }

        val username = intent.getStringExtra("username")
        val databaseHelper = DatabaseHelper(this)

        if (username != null) {
            val user = databaseHelper.getUser(username)

            if (user != null) {
                val email = user[0]
                val firstName = user[1]
                val lastName = user[2]
                val userName = user[3]

                tvEmail.text = email
                tvFirstName.text = firstName
                tvLastName.text = lastName
                tvUsername.text = "@$userName"
                tvFullName.text = "$firstName $lastName"

                if (firstName.isNotEmpty()) {
                    tvProfileInitial.text = firstName[0].uppercase()
                }
            }
        }
    }
}