package com.example.fotoconnect

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fotoconnect.databinding.ActivityFeedBinding

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Accessing notificationl from custom_toolbar layout
        binding.costumToolbar.notificationl.setOnClickListener {
            // Launch your activity when the notification icon is clicked
            startActivity(Intent(this, TakepicActivity::class.java))
        }
        // Accessing notificationl from custom_toolbar layout
        binding.costumToolbar.people.setOnClickListener {
            // Launch your activity when the notification icon is clicked
            startActivity(Intent(this, MyUserActivity::class.java))
        }
        binding.bottomNavigation.selectedItemId = R.id.ic_camara
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.ic_mensaje -> {
                    startActivity(Intent(this, MensajeActivity::class.java))
                    true
                }
               /*R.id.ic_camara -> {
                    startActivity(Intent(this, TakepicActivity::class.java))
                    true
                }*/
                else -> false
            }
        }
    }
}
