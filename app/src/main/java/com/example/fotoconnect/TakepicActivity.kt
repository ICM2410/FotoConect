package com.example.fotoconnect

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fotoconnect.R

class TakepicActivity : AppCompatActivity() {
     private val PERMISSION_CODE = 1000;
     private val IMAGE_CAPTURE_CODE   = 1001
    private lateinit var Imagenaver: ImageView // Declare ImageView

    var image_uri: Uri?= null
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_imagen)

         val captureBtn: Button = findViewById(R.id.capture_btn)
         Imagenaver = findViewById(R.id.Imagenaver)
         val userIcon: ImageView = findViewById(R.id.userIcon)
         userIcon.setOnClickListener {
             // Finish the current activity to return to the previous one
             finish()
         }

         captureBtn.setOnClickListener {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                     checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                 ) {
                     // Request permissions
                     val permission = arrayOf(
                         Manifest.permission.CAMERA,
                         Manifest.permission.WRITE_EXTERNAL_STORAGE
                     )
                     requestPermissions(permission, PERMISSION_CODE)
                 } else {
                     // Permissions granted, proceed with capturing image
                     openCamera()
                 }
             } else {
                 // Device doesn't need runtime permissions, proceed with capturing image
                 // For now, leave this block empty
                 openCamera()

             }
         }
     }

     private fun openCamera() {
         val values  = ContentValues()
         values.put(MediaStore.Images.Media.TITLE, "New Picture")
         values.put(MediaStore.Images.Media.DESCRIPTION, "From the camera")
         image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

         //camera intent
         val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
         startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
     }

     override fun onRequestPermissionsResult(
         requestCode: Int,
         permissions: Array<out String>,
         grantResults: IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
         when(requestCode){
             PERMISSION_CODE -> {
                 if (grantResults.isNotEmpty() && grantResults[0] ==  PackageManager.PERMISSION_GRANTED){
                     openCamera()
                 }else{
                     Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                 }
             }
         }
     }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)

         if (resultCode == Activity.RESULT_OK){
             //Set image captured to imageview
             Imagenaver.setImageURI(image_uri)
         }
     }
}
