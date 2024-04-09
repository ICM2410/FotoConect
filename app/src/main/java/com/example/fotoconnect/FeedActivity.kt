package com.example.fotoconnect

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fotoconnect.databinding.ActivityFeedBinding

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Para iniciar buttom navigation
        val navigationButton = findViewById<View>(R.id.ic_mensaje)
        navigationButton.setOnClickListener {
            // Start FeedActivity here
            val intent = Intent(this, MensajeActivity::class.java)
            startActivity(intent)
        }

        val notificationButton = findViewById<View>(R.id.notificationl)


        notificationButton.setOnClickListener {
            // Start NotificationActivity here
            val intent = Intent(this, TakepicActivity::class.java)
            startActivity(intent)
        }
        val peopleButton = findViewById<View>(R.id.people)

        peopleButton.setOnClickListener {
            // Start NotificationActivity here
            val intent = Intent(this, MyUserActivity::class.java)
            startActivity(intent)
        }
    }
}