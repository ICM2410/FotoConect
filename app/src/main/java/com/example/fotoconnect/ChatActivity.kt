package com.example.fotoconnect

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fotoconnect.databinding.ActivityFeedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
class ChatActivity : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference
    var receiverRoom: String? = null
    var senderRoom: String? = null
    private var notid = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Pedir permisos para notificaciones
        requestPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid

        val userIcon = findViewById<ImageView>(R.id.userIcon)
        userIcon.setOnClickListener {
            finish() // Close the current activity
        }

        val userNameTextView = findViewById<TextView>(R.id.userNameTextView)
        userNameTextView.text = name

        val userImageView = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imagenView)

        // Fetch the contact image from Firebase
        if (receiverUid != null) {
            mDbRef = FirebaseDatabase.getInstance().getReference("users").child(receiverUid)
            mDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val contactImage = snapshot.child("contactImage").getValue(String::class.java)
                    if (contactImage != null) {
                        Glide.with(this@ChatActivity)
                            .load(contactImage)
                            .circleCrop()
                            .into(userImageView)
                    } else {
                        userImageView.setImageResource(R.drawable.fotoprueba3) // Default image
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load user image.", Toast.LENGTH_SHORT).show()
                    Log.e("ChatActivity", "Database error: ${error.message}")
                }
            })
        }

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        supportActionBar?.title = name

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sentButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        mDbRef = FirebaseDatabase.getInstance().getReference()

        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        messageList.add(message!!)
                        val messageText = message?.message ?: "Mensaje vacío"

                        // Enviar notificación por cada nuevo mensaje
                        val notification = buildNotification(
                            "Nuevo mensaje de $name",
                            messageText,
                            R.drawable.fotoconnectlogo,
                            ChatActivity::class.java
                        )
                        notify(notification)
                    }
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages.", Toast.LENGTH_SHORT).show()
                    Log.e("ChatActivity", "Database error: ${error.message}")
                }
            })

        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            val messageObject = Message(message, senderUid)
            mDbRef.child("chats").child(senderRoom!!).child("messages").push().setValue(messageObject)
                .addOnSuccessListener {
                    mDbRef.child("chats").child(receiverRoom!!).child("messages").push().setValue(messageObject)
                }
            messageBox.setText("")
        }


    }

    fun buildNotification(title: String, message: String, icon: Int, target: Class<*>): Notification {
        val builder = NotificationCompat.Builder(this, "Test")
        builder.setSmallIcon(icon)
        builder.setContentTitle(title)
        builder.setContentText(message)
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val intent = Intent(this, target)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true) // Remueve la notificación cuando se toque

        return builder.build()
    }

    val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {}
    )

    fun notify(notification: Notification) {
        notid++
        val notificationManager = NotificationManagerCompat.from(this)
        if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notid, notification)
        }
    }
}

