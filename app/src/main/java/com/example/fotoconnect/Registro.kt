package com.example.fotoconnect

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Registro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        window.statusBarColor = Color.TRANSPARENT
        // Add any additional logic or UI setup specific to the RegistroActivity here
    }    override fun onBackPressed() {
        super.onBackPressed()
        // Add any specific behavior you want when the back button is pressed
    }
}
