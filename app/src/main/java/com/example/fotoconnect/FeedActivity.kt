package com.example.fotoconnect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fotoconnect.databinding.ActivityFeedBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val TAG = "FeedActivity"
private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        postList = mutableListOf()
        postAdapter = PostAdapter(this, postList)
        val recyclerView: RecyclerView = binding.userRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("posts")

        // Attach a listener to read the data at our posts reference
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.e(TAG, "No data found")
                    return
                }
                postList.clear()
                for (postSnapshot in dataSnapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                        Log.d(TAG, "Post: $it")
                    }
                }
                // Reverse the post list to display the most recent posts first
                postList.reverse()
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Error while reading posts", databaseError.toException())
            }
        })

        val mapButton = findViewById<View>(R.id.ic_mapa)
        mapButton.setOnClickListener {
            // Check for location permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Get the location and update Firebase
                getLastKnownLocationAndUpdateUser {
                    startActivity(Intent(this, MapActivity::class.java))
                }
            } else {
                // Request location permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }

        val navigationButton = findViewById<View>(R.id.ic_mensaje)
        navigationButton.setOnClickListener {
            val intent = Intent(this, MensajeActivity::class.java)
            startActivity(intent)
        }

        val notificationButton = findViewById<View>(R.id.notificationl)
        notificationButton.setOnClickListener {
            val intent = Intent(this, TakepicActivity::class.java)
            startActivity(intent)
        }

        val peopleButton = findViewById<View>(R.id.people)
        peopleButton.setOnClickListener {
            val intent = Intent(this, MyUserActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getLastKnownLocationAndUpdateUser(onSuccess: () -> Unit) {
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
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    updateUserLocation(it)
                }
                onSuccess()
            }
            .addOnFailureListener {
                onSuccess()
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
                        Log.d(TAG, "Location updated successfully")
                    } else {
                        Log.e(TAG, "Failed to update location")
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
                    getLastKnownLocationAndUpdateUser {
                        startActivity(Intent(this, MapActivity::class.java))
                    }
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MapActivity::class.java))
                }
                return
            }
        }
    }

    companion object {
        private const val TAG = "FeedActivity"
    }
}
