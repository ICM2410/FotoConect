package com.example.fotoconnect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import java.util.Locale
import java.util.UUID

class TakepicActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private lateinit var imagePreview1: ImageView
    private lateinit var imagePreview2: ImageView
    private var imageUri1: Uri? = null
    private var imageUri2: Uri? = null
    private var isFirstImage = true

    private val CHANNEL_ID = "post_upload_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imagen)

        imagePreview1 = findViewById(R.id.imagePreview1)
        imagePreview2 = findViewById(R.id.imagePreview2)
        val captureBtn: Button = findViewById(R.id.capture_btn)
        val publishBtn: Button = findViewById(R.id.publish_btn)
        val userIcon: ImageView = findViewById(R.id.userIcon)

        userIcon.setOnClickListener {
            finish()
        }
        captureBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera()
                }
            } else {
                openCamera()
            }
        }
        publishBtn.setOnClickListener {
            if (imageUri1 != null && imageUri2 != null) {
                uploadImagesAndPublishPost()
            } else {
                Toast.makeText(this, "Please capture both images before publishing", Toast.LENGTH_SHORT).show()
            }
        }

        createNotificationChannel()
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the camera")
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)

        if (isFirstImage) {
            imageUri1 = imageUri
        } else {
            imageUri2 = imageUri
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_CAPTURE_CODE) {
            if (isFirstImage) {
                imagePreview1.setImageURI(imageUri1)
                isFirstImage = false
            } else {
                imagePreview2.setImageURI(imageUri2)
                isFirstImage = true
            }
        }
    }

    private fun uploadImagesAndPublishPost() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef1 = storageRef.child("images/${UUID.randomUUID()}")
        val imageRef2 = storageRef.child("images/${UUID.randomUUID()}")
        val uploadTask1 = imageRef1.putFile(imageUri1!!)
        val uploadTask2 = imageRef2.putFile(imageUri2!!)

        uploadTask1.addOnSuccessListener { taskSnapshot ->
            imageRef1.downloadUrl.addOnSuccessListener { uri1 ->
                uploadTask2.addOnSuccessListener { taskSnapshot2 ->
                    imageRef2.downloadUrl.addOnSuccessListener { uri2 ->
                        publishPost(uri1.toString(), uri2.toString())
                    }
                }
            }
        }
    }

    private fun publishPost(imageUrl1: String, imageUrl2: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser!!.uid)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    val userLocation = LatLng(user.latitude ?: 0.0, user.longitude ?: 0.0)
                    val geocoder = Geocoder(this@TakepicActivity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(userLocation.latitude, userLocation.longitude, 1)
                    val address = if (addresses!!.isNotEmpty()) {
                        addresses[0].getAddressLine(0)
                    } else {
                        "Location unknown"
                    }
                    val post = Post(
                        description = address,
                        image_url1 = imageUrl1,
                        creation_time_miliseconds = System.currentTimeMillis(),
                        user = user,
                        image_url2 = imageUrl2
                    )
                    val postReference = FirebaseDatabase.getInstance().getReference("posts")
                    postReference.push().setValue(post).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@TakepicActivity, "Post publicado satisfactoriamente", Toast.LENGTH_SHORT).show()
                            sendNotification()
                            startActivity(Intent(this@TakepicActivity, FeedActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@TakepicActivity, "Failed to publish post", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@TakepicActivity, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TakepicActivity, "Failed to load user profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Post Upload Notifications"
            val descriptionText = "Notifications for successful post uploads"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification() {
        // For Android 13 and above, check if the app has POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            return
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications_outline_svgrepo_com) // Make sure this icon exists in your drawable folder
            .setContentTitle("Publicación exitosa!")
            .setContentText("Tu publicación ha sido subida satisfactoriamente.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set to high for better visibility

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}