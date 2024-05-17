package com.example.fotoconnect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

class IniciaSesion : AppCompatActivity() {
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var bdtSignUp: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciarsesion)
        mAuth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        supportActionBar?.hide()
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btn_login)
        bdtSignUp = findViewById(R.id.btn_SignUp)
        window.statusBarColor = Color.TRANSPARENT

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()
            login(email, password)
        }
        val olvideContraseñaButton: Button = findViewById(R.id.button2)
        olvideContraseñaButton.setOnClickListener {
            val intent = Intent(this, RecuperarContrasena::class.java)
            startActivity(intent)
            finish()
        }
        val registrateButton: Button = findViewById(R.id.btn_SignUp)
        registrateButton.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun login(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Check for location permission
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Get the location and update Firebase
                        getLastKnownLocationAndUpdateUser()
                    } else {
                        // Request location permission
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                    }
                } else {
                    Toast.makeText(this@IniciaSesion, "El usuario no existe", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun getLastKnownLocationAndUpdateUser() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    updateUserLocation(it)
                }
                // Navigate to FeedActivity even if location is null
                val intent = Intent(this@IniciaSesion, FeedActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                // Navigate to FeedActivity if location fetch fails
                val intent = Intent(this@IniciaSesion, FeedActivity::class.java)
                startActivity(intent)
                finish()
            }
    }

    private fun updateUserLocation(location: Location) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
            userReference.child("latitude").setValue(location.latitude)
            userReference.child("longitude").setValue(location.longitude)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("IniciaSesion", "Location updated successfully")
                    } else {
                        Log.e("IniciaSesion", "Failed to update location")
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, get the location and update Firebase
                    getLastKnownLocationAndUpdateUser()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@IniciaSesion, FeedActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                return
            }
        }
    }
}
