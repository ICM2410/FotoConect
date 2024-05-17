package com.example.fotoconnect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
class PostAdapter(private val context: Context, private val postList: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userImageView: ImageView = itemView.findViewById(R.id.image_profile)
        private val postImageView1: ImageView = itemView.findViewById(R.id.post_image1)
        private val postImageView2: ImageView = itemView.findViewById(R.id.post_image2)
        private val userNameTextView: TextView = itemView.findViewById(R.id.usuario)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.new_text)
        private val timeAgoTextView: TextView = itemView.findViewById(R.id.time_ago)

        fun bind(post: Post) {
            userNameTextView.text = post.user?.name ?: "Unknown User"
            descriptionTextView.text = post.description
            timeAgoTextView.text = getTimeAgo(context, post.creation_time_miliseconds)

            Glide.with(context).load(post.user?.contactImage).into(userImageView)
            Glide.with(context).load(post.image_url1).into(postImageView1)
            Glide.with(context).load(post.image_url2).into(postImageView2)

            postImageView2.setOnClickListener {
                swapImages(post)
            }
        }

        private fun swapImages(post: Post) {
            val tempUrl = post.image_url1
            post.image_url1 = post.image_url2
            post.image_url2 = tempUrl

            Glide.with(context).load(post.image_url1).into(postImageView1)
            Glide.with(context).load(post.image_url2).into(postImageView2)
        }
    }
}

fun getTimeAgo(context: Context, timeInMilliseconds: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timeInMilliseconds

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.just_now)
        diff < TimeUnit.MINUTES.toMillis(60) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            context.resources.getQuantityString(R.plurals.minutes_ago, minutes.toInt(), minutes)
        }
        diff < TimeUnit.HOURS.toMillis(24) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            context.resources.getQuantityString(R.plurals.hours_ago, hours.toInt(), hours)
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            context.resources.getQuantityString(R.plurals.days_ago, days.toInt(), days)
        }
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timeInMilliseconds))
        }
    }
}

