package com.example.fotoconnect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fotoconnect.databinding.ActivityFeedBinding
import com.example.fotoconnect.model.Post
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val TAG = "FeedActivity"
class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("posts")

        // Attach a listener to read the data at our posts reference
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.e(TAG, "No data found")
                    return
                }
                val postList = mutableListOf<Post>()
                for (postSnapshot in dataSnapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                        Log.d(TAG, "Post: $it")
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Error while reading posts", databaseError.toException())
            }
        })
        

        //Boton mapa
        val mapButton = findViewById<View>(R.id.ic_mapa)
        mapButton.setOnClickListener {
            startActivity(Intent(this,MapsActivity::class.java))
        }


        // Setup navigation buttons as before
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
}
