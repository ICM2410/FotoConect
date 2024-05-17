package com.example.fotoconnect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*

private const val TAG = "MyUserActivity"
private const val PICK_IMAGE_REQUEST = 1000

class MyUserActivity : AppCompatActivity() {
    private lateinit var userIcon: ImageView
    private lateinit var userImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userReference: DatabaseReference
    private lateinit var storageReference: FirebaseStorage
    private var imageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAG = "MyUserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_myprofile)

        userIcon = findViewById(R.id.userIcon)
        userImageView = findViewById(R.id.imagenView)
        userNameTextView = findViewById(R.id.Nombusuario)
        userEmailTextView = findViewById(R.id.userEmailTextView)

        val currentUser = FirebaseAuth.getInstance().currentUser
        userReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser!!.uid)
        storageReference = FirebaseStorage.getInstance()

        userIcon.setOnClickListener {
            onBackPressed()
        }

        userImageView.setOnClickListener {
            openGallery()
        }

        loadUserInfo()

        val moreSignVectorButton = findViewById<ImageView>(R.id.UserSettings)
        moreSignVectorButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, IniciaSesion::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserInfo() {
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    userNameTextView.text = user.name
                    userEmailTextView.text = user.email
                    Glide.with(this@MyUserActivity)
                        .load(user.contactImage)
                        .placeholder(R.drawable.fotoprueba3) // Add a placeholder image
                        .into(userImageView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading user info", error.toException())
                Toast.makeText(this@MyUserActivity, "Error loading user info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            uploadImageToFirebase()
        }
    }

    private fun uploadImageToFirebase() {
        if (imageUri != null) {
            val ref = storageReference.reference.child("profile_images/${UUID.randomUUID()}")
            val uploadTask = ref.putFile(imageUri!!)
            uploadTask.addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val newImageUrl = uri.toString()
                    updateUserProfilePicture(newImageUrl)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserProfilePicture(imageUrl: String) {
        userReference.child("contactImage").setValue(imageUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.fotoprueba3) // Add a placeholder image
                    .into(userImageView)
            } else {
                Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
