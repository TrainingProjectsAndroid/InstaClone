package ru.asshands.softwire.instaclone.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_add_friends.*
import kotlinx.android.synthetic.main.add_friends_item.view.*
import ru.asshands.softwire.instaclone.R
import ru.asshands.softwire.instaclone.models.User
import ru.asshands.softwire.instaclone.utils.FirebaseHelper
import ru.asshands.softwire.instaclone.utils.ValueEventListenerAdapter

class AddFriendsActivity : AppCompatActivity(), FriendsAdapter.Listener {

    private lateinit var mAdapter: FriendsAdapter
    private lateinit var mUsers: List<User>
    private lateinit var mUser: User
    private lateinit var mFirebase: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friends)

        mFirebase = FirebaseHelper(this)
        mAdapter = FriendsAdapter(this)
        val uid = mFirebase.currentUid()!!

        back_image.setOnClickListener { finish() }
        add_friends_recycler.adapter = mAdapter
        add_friends_recycler.layoutManager = LinearLayoutManager(this)

        mFirebase
            .database
            .child("users")
            .addValueEventListener(ValueEventListenerAdapter { dataSnapshot ->
                val allUsers = dataSnapshot
                    .children
                    .map { it.asUser()!!.copy(uid = it.key!!) }
                val (userList, otherUsersList) =
                    allUsers.partition { it.uid == uid }
                mUser = userList.first()
                mUsers = otherUsersList

                mAdapter.update(mUsers, mUser.follows)
            })
    }



    override fun follow(uid: String) {
        setFollow(uid, true) {
            mAdapter.followed(uid)
        }
    }

    override fun unFollow(uid: String) {
        setFollow(uid, false) {
            mAdapter.unFollowed(uid)
        }
    }

    private fun setFollow(uid: String, follow: Boolean, onSuccess: () -> Unit) {



        val followsTask = mFirebase
            .database
            .child("users")
            .child(mUser.uid!!)
            .child("follows")
            .child(uid)
            .setValueTrueOrRemove(follow)

        val followersTask = mFirebase
            .database
            .child("users")
            .child(uid)
            .child("followers")
            .child(mUser.uid!!)
            .setValueTrueOrRemove(follow)


        val feedPostsTask = task<Void> { taskSource ->

            mFirebase
                .database
                .child("feed-posts")
                .child(uid)
                .addListenerForSingleValueEvent(ValueEventListenerAdapter {
                    val postsMap = if (follow) {
                        it.children.map { post -> post.key to post.value }.toMap()
                    } else {
                        it.children.map { post -> post.key to null }.toMap()
                    }

                    mFirebase
                        .database
                        .child("feed-posts")
                        .child(mUser.uid!!)
                        .updateChildren(postsMap)
                        .addOnCompleteListener(TaskSourceOnCompleteListener(taskSource))
                })
        }

        Tasks
            .whenAll(followsTask, followersTask, feedPostsTask)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onSuccess()
                } else {
                    showToast(it.exception!!.message!!)
                }
            }
    }
}

class FriendsAdapter(private val listener: Listener) :
    RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    interface Listener {
        fun follow(uid: String)
        fun unFollow(uid: String)
    }

    private var mUsers = listOf<User>()
    private var mPositions = mapOf<String, Int>()
    private var mFollows = mapOf<String, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.add_friends_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            val user = mUsers[position]
            view.photo_image.loadUserPhoto(user.photo)
            view.username_text.text = user.username
            view.name_text.text = user.name
            view.follow_btn.setOnClickListener { listener.follow(user.uid) }
            view.unfollow_btn.setOnClickListener { listener.unFollow(user.uid) }

            val follows = mFollows[user.uid] ?: false
            if (follows) {
                view.follow_btn.visibility = View.GONE
                view.unfollow_btn.visibility = View.VISIBLE
            } else {
                view.follow_btn.visibility = View.VISIBLE
                view.unfollow_btn.visibility = View.GONE
            }
        }

    }



    override fun getItemCount() = mUsers.size

    fun update(users: List<User>, follows: Map<String, Boolean>) {
        mUsers = users
        mPositions = users.withIndex().map { (index, user) -> user.uid to index }.toMap()
        mFollows = follows
        notifyDataSetChanged()
    }

    fun followed(uid: String) {
        mFollows += (uid to true)
        notifyItemChanged(mPositions[uid]!!)
    }

    fun unFollowed(uid: String) {
        mFollows -= (uid)
        notifyItemChanged(mPositions[uid]!!)
    }


}