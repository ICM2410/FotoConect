package com.example.fotoconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class IniciaSesion : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciarsesion)

        window.statusBarColor = Color.TRANSPARENT

        // Find the "Iniciar sesión" button by its ID
        val iniciarSesionButton: Button = findViewById(R.id.iniciases)

        // Set a click listener for the "Iniciar sesión" button
        iniciarSesionButton.setOnClickListener {
            // Start FeedActivity when the button is clicked
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
            finish()  // Optional: Close the current activity if needed
        }

        // Find the "Olvidé mi contraseña" button by its ID
        val olvideContraseñaButton: Button = findViewById(R.id.button2)

        // Set a click listener for the "Olvidé mi contraseña" button
        olvideContraseñaButton.setOnClickListener {
            // Start RecuperarContrasena activity when the button is clicked
            val intent = Intent(this, RecuperarContrasena::class.java)
            startActivity(intent)
            finish()  // Optional: Close the current activity if needed
        }

        // Find the "No tienes una cuenta? Registrate!" button by its ID
        val registrateButton: Button = findViewById(R.id.button3)

        // Set a click listener for the "No tienes una cuenta? Registrate!" button
        registrateButton.setOnClickListener {
            // Start RegistroActivity when the button is clicked
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
            finish()  // Optional: Close the current activity if needed
        }
    }
}
