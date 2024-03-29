package com.example.fotoconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class IniciaSesion : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var bdtSignUp: Button
    private lateinit var mAuth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciarsesion)


        mAuth = FirebaseAuth.getInstance()
        supportActionBar?.hide()

       //Inicializacion de variables
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btn_login)
        bdtSignUp = findViewById(R.id.btn_SignUp)

        //App bar
        window.statusBarColor = Color.TRANSPARENT

        // Find the "Iniciar sesión" button by its ID
        val iniciarSesionButton: Button = findViewById(R.id.btn_login)

        // Set a click listener for the "Iniciar sesión" button
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()


            login(email, password);
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
        val registrateButton: Button = findViewById(R.id.btn_SignUp)

        // Set a click listener for the "No tienes una cuenta? Registrate!" button
        registrateButton.setOnClickListener {
            // Start RegistroActivity when the button is clicked
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
            finish()  // Optional: Close the current activity if needed
        }
    }
    private fun login(email: String, password: String){
        //Logica del login
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //Codigo de inicio de sesion
                    val intent = Intent(this@IniciaSesion, FeedActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@IniciaSesion, "El usuario no exite", Toast.LENGTH_SHORT).show()

                }
            }

    }
}
