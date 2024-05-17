package com.example.fotoconnect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserAdapter(val context: Context, val userList: ArrayList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val notificationManager = NotificationManagerCompat.from(context)
    private var notid = 0
    private val CHANNEL_ID = "message_notification_channel"

    init {
        createNotificationChannel()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.mensajeitem, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.textName.text = currentUser.name

        if (currentUser.contactImage != null) {
            Glide.with(context)
                .load(currentUser.contactImage)
                .circleCrop()
                .into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.fotoprueba3)
        }

        // Fetch the last message for this user
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        val receiverUid = currentUser.uid
        val senderRoom = senderUid + receiverUid

        val mDbRef = FirebaseDatabase.getInstance().getReference("chats").child(senderRoom).child("messages")
        mDbRef.orderByKey().limitToLast(1).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    val message = postSnapshot.getValue(Message::class.java)
                    holder.preview.text = message?.message ?: "No messages yet"
                    currentUser.name?.let { sendNotification(it, message?.message ?: "No message content") }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserAdapter", "Database error: ${error.message}")
            }
        })

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)
            context.startActivity(intent)
        }
    }

    private fun sendNotification(senderName: String, message: String) {
        val notification = buildNotification(
            "New message from $senderName",
            message,
            R.drawable.fotoconnectlogo,
            ChatActivity::class.java
        )
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notid++
            notificationManager.notify(notid, notification)
        }
    }

    private fun buildNotification(title: String, message: String, icon: Int, target: Class<*>): Notification {
        val intent = Intent(context, target)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Remove the notification when touched
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Message Notifications"
            val descriptionText = "Notifications for new messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.txt_name)
        val userImage: ImageView = itemView.findViewById(R.id.imagenView)
        val preview: TextView = itemView.findViewById(R.id.preview)
    }
}
