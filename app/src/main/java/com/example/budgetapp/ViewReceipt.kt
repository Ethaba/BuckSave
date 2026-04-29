package com.example.budgetapp

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ViewReceipt : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_receipt)

        val imgReceipt = findViewById<ImageView>(R.id.imgReceipt)
        val uriString = intent.getStringExtra("imageUri")

        if (uriString != null) {
            imgReceipt.setImageURI(Uri.parse(uriString))
        }
    }
}