package ru.asshands.softwire.instaclone.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.android.synthetic.main.activity_edit_profile.*
import ru.asshands.softwire.instaclone.R
import ru.asshands.softwire.instaclone.models.User
import ru.asshands.softwire.instaclone.utils.CameraHelper
import ru.asshands.softwire.instaclone.utils.FirebaseHelper
import ru.asshands.softwire.instaclone.utils.ValueEventListenerAdapter
import ru.asshands.softwire.instaclone.views.PasswordDialog

class EditProfileActivity : AppCompatActivity(), PasswordDialog.Listener {

    private lateinit var mUser: User
    private lateinit var mPendingUser: User
    private lateinit var mFirebase: FirebaseHelper
    private lateinit var mCamera: CameraHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        val tag = this.localClassName

        mCamera = CameraHelper(this)
        back_image.setOnClickListener { finish() }
        save_image.setOnClickListener { updateProfile() }
        change_photo_text.setOnClickListener { mCamera.takeCameraPicture() }

        mFirebase = FirebaseHelper(this)
        mFirebase.currentUserReference()
            .addListenerForSingleValueEvent(ValueEventListenerAdapter {
                mUser = it.asUser()!!
                name_input.setText(mUser.name)
                username_input.setText(mUser.username)
                website_input.setText(mUser.website)
                bio_input.setText(mUser.bio)
                email_input.setText(mUser.email)
                phone_input.setText(mUser.phone)
                profile_image.loadUserPhoto(mUser.photo)
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mCamera.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // upload image to firebase storage
            val uid = mFirebase.currentUid()!!
            mFirebase.storage
                .child("users/$uid/photo")
                .putFile(mCamera.imageUri!!)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        mFirebase.storage
                            .child("users/$uid/photo")
                            .downloadUrl
                            .addOnSuccessListener { uri ->
                                val photoUrl = uri.toString()
                                mFirebase.updateUserPhoto(photoUrl) {
                                    mUser = mUser.copy(photo = photoUrl)
                                    profile_image.loadUserPhoto(mUser.photo)
                                }
                            }
                    } else {
                        showToast(it.exception!!.message!!)
                    }
                }
        }
    }


    private fun updateProfile() {
        mPendingUser = readInputs()
        val error = validate(mPendingUser)
        if (error == null) {
            if (mPendingUser.email == mUser.email) {
                updateUser(mPendingUser)
            } else {
                PasswordDialog().show(supportFragmentManager, "password_dialog")
            }
        } else {
            showToast(error)
        }
    }

    private fun readInputs(): User {
        return User(
            name = name_input.text.toString(),
            username = username_input.text.toString(),
            email = email_input.text.toString(),
            website = website_input.text.toStringOrNull(),
            bio = bio_input.text.toStringOrNull(),
            phone = phone_input.text.toStringOrNull()
        )
    }

    private fun validate(user: User): String? =
        when {
            user.name.isEmpty() -> "Please enter name"
            user.username.isEmpty() -> "Please enter username"
            user.email.isEmpty() -> "Please enter email"
            else -> null
        }

    override fun onPasswordConfirm(password: String) {
        if (password.isNotEmpty()) {
            //credential = удостоверение
            val credential = EmailAuthProvider.getCredential(mUser.email, password)
            mFirebase.reauthenticate(credential) {
                mFirebase.updateEmail(mPendingUser.email) {
                    updateUser(mPendingUser)
                }
            }
        } else {
            showToast("You should enter your password")
        }
    }

    private fun updateUser(user: User) {
        val updatesMap = mutableMapOf<String, Any?>()
        if (mUser.name !== user.name) updatesMap["name"] = user.name
        if (mUser.username !== user.username) updatesMap["username"] = user.username
        if (mUser.website !== user.website) updatesMap["website"] = user.website
        if (mUser.bio !== user.bio) updatesMap["bio"] = user.bio
        if (mUser.email !== user.email) updatesMap["email"] = user.email
        if (mUser.phone !== user.phone) updatesMap["phone"] = user.phone

        mFirebase.updateUser(updatesMap) {
            showToast("Profile saved")
            finish()
        }
    }

}

