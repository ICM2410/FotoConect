package com.example.fotoconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Registro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        window.statusBarColor = Color.TRANSPARENT

        // Find the "reg2" button by its ID
        val reg2Button: Button = findViewById(R.id.reg2)

        // Set a click listener for the "reg2" button
        reg2Button.setOnClickListener {
            // Start FeedActivity when the button is clicked
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }

        // Find the "button2" button by its ID
        val button2: Button = findViewById(R.id.button2)

        // Set a click listener for the "button2" button
        button2.setOnClickListener {
            // Start IniciaSesion activity when the button is clicked
            val intent = Intent(this, IniciaSesion::class.java)
            startActivity(intent)
            finish()  // Optional: Close the current activity if needed
        }

        // Add any additional logic or UI setup specific to the RegistroActivity here
    }

    override fun onBackPressed() {
        // Override the back button behavior
        // If you want to go back to IniciaSesion, start its activity
        val intent = Intent(this, IniciaSesion::class.java)
        startActivity(intent)
        finish()  // Optional: Close the current activity if needed
    }
}
