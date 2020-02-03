package ru.asshands.softwire.instaclone.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_share.*
import ru.asshands.softwire.instaclone.R
import ru.asshands.softwire.instaclone.models.FeedPost
import ru.asshands.softwire.instaclone.models.User
import ru.asshands.softwire.instaclone.utils.CameraHelper
import ru.asshands.softwire.instaclone.utils.FirebaseHelper
import ru.asshands.softwire.instaclone.utils.GlideApp
import ru.asshands.softwire.instaclone.utils.ValueEventListenerAdapter

class ShareActivity : BaseActivity() {
    private lateinit var mCamera: CameraHelper
    private lateinit var mFirebase: FirebaseHelper
    private lateinit var mUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        val tag = this.localClassName
        Log.d(tag, "OnCreate")

        mCamera = CameraHelper(this)
        mCamera.takeCameraPicture()
        mFirebase = FirebaseHelper(this)

        back_image.setOnClickListener { finish() }
        share_text.setOnClickListener { share() }

        mFirebase.currentUserReference().addValueEventListener(ValueEventListenerAdapter {
            mUser = it.asUser()!!
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mCamera.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                GlideApp.with(this).load(mCamera.imageUri).centerCrop().into(post_image)
            } else {
                finish()
            }
        }
    }

    private fun share() {
        val imageUri = mCamera.imageUri
        if (imageUri !== null) {
            val uid = mFirebase.currentUid()!!
            mFirebase.storage
                .child("users")
                .child(uid)
                .child("images")
                .child(imageUri.lastPathSegment!!)
                .putFile(imageUri)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        mFirebase.storage
                            .child("users/$uid/images/${imageUri.lastPathSegment}")
                            .downloadUrl
                            .addOnSuccessListener { uri ->
                                val imageDownloadUrl = uri.toString()
                                mFirebase.database
                                    .child("images")
                                    .child(uid)
                                    .push()
                                    .setValue(imageDownloadUrl)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            mFirebase.database
                                                .child("feed-posts")
                                                .child(uid)
                                                .push()
                                                .setValue(makeFeedPost(uid, imageDownloadUrl))
                                                .addOnCompleteListener {
                                                    if (it.isSuccessful) {
                                                        startActivity(
                                                            Intent(
                                                                this,
                                                                ProfileActivity::class.java
                                                            )
                                                        )
                                                        finish()
                                                    }
                                                }
                                        } else {
                                            showToast(task.exception!!.message!!)
                                        }
                                    }
                            }
                    } else {
                        showToast(it.exception!!.message!!)
                    }
                }
        }

    }

    private fun makeFeedPost(uid: String, imageDownloadUrl: String): FeedPost {
        return FeedPost(
            uid = uid,
            username = mUser.username,
            image = imageDownloadUrl,
            caption = caption_input.text.toString(),
            photo = mUser.photo
        )
    }


}


