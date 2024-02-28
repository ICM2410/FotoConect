package com.example.fotoconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.messageview)

        window.statusBarColor = Color.TRANSPARENT

        // Delay the transition to IniciarSesion activity after 4 seconds
        Handler().postDelayed({
            val intent = Intent(this, IniciaSesion::class.java)
            startActivity(intent)
            finish()  // Optional: Close the current activity if needed
        }, 3000)  // 4 seconds delay
    }
}
