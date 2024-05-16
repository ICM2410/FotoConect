package com.example.fotoconnect

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class Registro : AppCompatActivity() {
    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var bdtSignUp: Button
    private lateinit var imgProfile: ImageView
    private lateinit var btnPickImage: Button
    private var profileImageUri: Uri? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbref: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        mAuth = FirebaseAuth.getInstance()
        mDbref = FirebaseDatabase.getInstance().getReference()
        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        bdtSignUp = findViewById(R.id.btn_SignUp)
        imgProfile = findViewById(R.id.img_profile)
        btnPickImage = findViewById(R.id.btn_pick_image)
        btnPickImage.setOnClickListener { pickImage() }
        bdtSignUp.setOnClickListener {
            val name = edtName.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()
            signUp(name, email, password)
        }
    }
    private fun signUp(name: String, email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val uid = mAuth.currentUser?.uid ?: return@addOnCompleteListener
                uploadProfileImage(uid) { imageUrl ->
                    getLastLocation { latitude, longitude ->
                        addUserToDatabase(name, email, uid, latitude, longitude, imageUrl)
                    }
                }
            } else {
                Toast.makeText(this, "An error occurred: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun uploadProfileImage(uid: String, callback: (String) -> Unit) {
        if (profileImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().getReference("profileImages/$uid.jpg")
            storageRef.putFile(profileImageUri!!).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload image: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            profileImageUri = data?.data
            imgProfile.setImageURI(profileImageUri)
        }
    }
    private fun getLastLocation(callback: (Double, Double) -> Unit) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callback(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Could not get location. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun addUserToDatabase(name: String, email: String, uid: String, latitude: Double, longitude: Double, contactImage: String) {
        val user = User(name, email, uid, latitude, longitude, contactImage)
        mDbref.child("users").child(uid).setValue(user).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, FeedActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Failed to register user", Toast.LENGTH_SHORT).show()
            }
        }
    }
    companion object {
        private const val IMAGE_PICK_CODE = 1000
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

