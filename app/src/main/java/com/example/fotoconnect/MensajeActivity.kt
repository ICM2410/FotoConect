package com.example.fotoconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MensajeActivity  : AppCompatActivity() {
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbref: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.messageview)

        mAuth = FirebaseAuth.getInstance()
        mDbref = FirebaseDatabase.getInstance().getReference()
        userList = ArrayList()
        adapter = UserAdapter(this, userList)

        userRecyclerView = findViewById(R.id.userRecyclerView)

        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter

        mDbref.child("user").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Borramos la lista para evitar repetidos
                userList.clear()
                for (postSnapshot in snapshot.children) {


                    val currentUser = postSnapshot.getValue(User::class.java)

                    if (mAuth.currentUser?.uid != currentUser?.uid) {
                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
        val navigationButton = findViewById<View>(R.id.ic_camara)
        navigationButton.setOnClickListener {
            // Start FeedActivity here
            val intent = Intent(this, FeedActivity::class.java)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            //logica del log out
            mAuth.signOut()
            val intent = Intent(this@MensajeActivity, IniciaSesion::class.java)
            finish()
            startActivity(intent)
            return true
        }
        return true

    }


}