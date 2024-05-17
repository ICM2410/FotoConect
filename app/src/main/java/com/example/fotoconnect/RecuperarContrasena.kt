package com.example.fotoconnect

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity



class RecuperarContrasena : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)
    }

    
    override fun onBackPressed() {
        // Override the back button behavior
        // If you want to go back to IniciaSesion, start its activity
        val intent = Intent(this, IniciaSesion::class.java)
        startActivity(intent)
        finish()  // Optional: Close the current activity if needed
    }
}
