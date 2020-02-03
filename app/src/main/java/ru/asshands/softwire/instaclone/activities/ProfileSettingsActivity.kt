package ru.asshands.softwire.instaclone.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_profile_settings.*
import ru.asshands.softwire.instaclone.R
import ru.asshands.softwire.instaclone.utils.FirebaseHelper

class ProfileSettingsActivity : AppCompatActivity(){
    private lateinit var mFirebase: FirebaseHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        mFirebase = FirebaseHelper(this)
        sign_out_text.setOnClickListener {
            mFirebase.auth.signOut() // ПОЧЕМУ УХОДИТ НА LoginActivity ??????????????
        }
        back_image.setOnClickListener { finish() }
    }
}
