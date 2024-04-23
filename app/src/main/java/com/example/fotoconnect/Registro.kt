package com.example.fotoconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fotoconnect.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Registro : AppCompatActivity() {


    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var bdtSignUp: Button
    private lateinit var mAuth : FirebaseAuth
    private lateinit var mDbref: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        //Instancia de firebase
        mAuth = FirebaseAuth.getInstance()
        window.statusBarColor = Color.TRANSPARENT
        supportActionBar?.hide()


        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        bdtSignUp = findViewById(R.id.btn_SignUp)


        bdtSignUp.setOnClickListener {
            val name = edtName.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            signUp(name,email,password)
        }

        // boton de inicio de sesion
        val button2: Button = findViewById(R.id.button2)

        // Regresar a inicio de sesion
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

    private fun signUp(name:String, email: String, password: String){
        //Logica del signup
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // COdigo para mandar a home
                    addUserToDataBase(name, email, mAuth.currentUser?.uid!!)
                    val intent = Intent(this@Registro, FeedActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    Toast.makeText(this@Registro, "Un error a ocurrido", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addUserToDataBase(name: String, email: String, uid: String){
        mDbref = FirebaseDatabase.getInstance().getReference()

        mDbref.child("user").child(uid).setValue(User(name,email,uid))

    }
}
