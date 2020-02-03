package ru.asshands.softwire.instaclone.activities

import android.os.Bundle
import android.util.Log
import ru.asshands.softwire.instaclone.R

class LikesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tag = this.localClassName
        setupBottomNavigation()
        Log.d(tag, "OnCreate")
    }
}
