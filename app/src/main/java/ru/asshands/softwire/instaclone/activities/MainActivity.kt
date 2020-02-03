package ru.asshands.softwire.instaclone.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.feed_item.view.*
import ru.asshands.softwire.instaclone.R
import ru.asshands.softwire.instaclone.models.FeedPost
import ru.asshands.softwire.instaclone.utils.FirebaseHelper
import ru.asshands.softwire.instaclone.utils.ValueEventListenerAdapter

class MainActivity : BaseActivity(), FeedAdapter.Listener {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirebase: FirebaseHelper
    private lateinit var mAdapter: FeedAdapter
    private var mLikesListener: Map<String, ValueEventListener> = emptyMap()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tag = this.localClassName
        setupBottomNavigation()
        Log.d(tag, "OnCreate")
        mFirebase = FirebaseHelper(this)
        mFirebase.auth.addAuthStateListener {
            if (it.currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mFirebase.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {

            mFirebase.database
                .child("feed-posts")
                .child(currentUser.uid)
                .addValueEventListener(ValueEventListenerAdapter {
                    val posts = it.children.map { dataSnapshot ->
                        dataSnapshot.asFeedPost()!!

                    }.sortedByDescending { map -> map.timeStampDate() }
                    mAdapter = FeedAdapter(this, posts)
                    feed_recycler.adapter = mAdapter
                    feed_recycler.layoutManager = LinearLayoutManager(this)
                })
        }
    }

    override fun toggleLike(postId: String) {
        val reference = mFirebase
            .database
            .child("likes")
            .child(postId)
            .child(mFirebase.currentUid()!!)
        reference.addListenerForSingleValueEvent(ValueEventListenerAdapter {
            reference.setValueTrueOrRemove(!it.exists())
        })
    }

    override fun loadLikes(postId: String, position: Int) {
        fun createListener() = mFirebase
            .database
            .child("likes")
            .child(postId)
            .addValueEventListener(ValueEventListenerAdapter {
                val userLikes = it.children.map { userLikes -> userLikes.key }.toSet()
                val postLikes = FeedPostLikes(
                    userLikes.size,
                    userLikes.contains(mFirebase.currentUid())
                )
                mAdapter.updatePostLikes(position, postLikes)
            })

        val createNewListener = (mLikesListener[postId] == null)
        if (createNewListener) mLikesListener += (postId to createListener())
    }

    override fun onDestroy() {
        super.onDestroy()
        mLikesListener.values.forEach { mFirebase.database.removeEventListener(it) }
    }
}

data class FeedPostLikes(val likesCount: Int, val likedByUser: Boolean)

class FeedAdapter(private val listener: Listener, private val posts: List<FeedPost>) :
    RecyclerView.Adapter<FeedAdapter.ViewHolder>() {

    interface Listener {
        fun toggleLike(postId: String)
        fun loadLikes(postId: String, position: Int)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    private var postLikes: Map<Int, FeedPostLikes> = emptyMap()
    private val defaultPostLikes = FeedPostLikes(0, false)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return ViewHolder(view)
    }

    fun updatePostLikes(position: Int, likes: FeedPostLikes) {
        postLikes += (position to likes)
        notifyItemChanged(position)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val likes = postLikes[position] ?: defaultPostLikes
        val post = posts[position]
        with(holder.view) {
            user_photo_image.loadUserPhoto(post.photo)
            username_text.text = post.username
            post_image.loadImage(post.image)
            if (likes.likesCount == 0) {
                likes_text.visibility = View.GONE
            } else {
                likes_text.visibility = View.VISIBLE
                likes_text.text = "${likes.likesCount} likes"
            }
            caption_text.setCaptionText(post.username, post.caption)
            like_image.setOnClickListener { listener.toggleLike(post.id) }
            like_image.setImageResource(
                if (likes.likedByUser) R.drawable.ic_likes_active
                else R.drawable.ic_likes_border
            )
            listener.loadLikes(post.id, position)
        }
    }

    private fun TextView.setCaptionText(username: String, caption: String) {
        val usernameSpannable = SpannableString(username)
        usernameSpannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            usernameSpannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        usernameSpannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    widget.context.showToast("Username is clicked")
                }

                override fun updateDrawState(ds: TextPaint) {}

            }, 0,
            usernameSpannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        text = SpannableStringBuilder()
            .append(usernameSpannable)
            .append(" ")
            .append(caption)
        movementMethod = LinkMovementMethod.getInstance()
    }

    override fun getItemCount() = posts.size

}
