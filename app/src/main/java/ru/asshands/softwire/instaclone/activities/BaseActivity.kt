package ru.asshands.softwire.instaclone.activities

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.bottom_navigation_view.*
import ru.asshands.softwire.instaclone.R

var position = 0

abstract class BaseActivity : AppCompatActivity() {

    fun setupBottomNavigation() {
        bottom_navigation_view.menu.getItem(position).isChecked = true
        bottom_navigation_view.setTextVisibility(false)
        bottom_navigation_view.setIconSize(29f)
        //bottom_navigation_view.enableItemShiftingMode(false)
        //bottom_navigation_view.enableShiftingMode(false)
        bottom_navigation_view.enableAnimation(false)

        for (i in 0 until bottom_navigation_view.menu.size()) {
            bottom_navigation_view.setIconTintList(i, null)
            Log.d("TAG", "i=$i")
            bottom_navigation_view.enableShiftingMode(i, false)
        }

        bottom_navigation_view.setOnNavigationItemSelectedListener {
            position = bottom_navigation_view.getMenuItemPosition(it)
            Log.d("TAG", "$position")
            val nextActivity =
                when (it.itemId) {
                    R.id.nav_item_home -> MainActivity::class.java
                    R.id.nav_item_likes -> LikesActivity::class.java
                    R.id.nav_item_profile -> ProfileActivity::class.java
                    R.id.nav_item_search -> SearchActivity::class.java
                    R.id.nav_item_share -> ShareActivity::class.java
                    else -> {
                        Log.d("BaseActivity", "WrongNavItem")
                        null
                    }
                }

            if (nextActivity != null) {
                val intent = Intent(this, nextActivity)
                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                startActivity(intent)
                overridePendingTransition(0, 0)
                return@setOnNavigationItemSelectedListener true
            } else {
                false
            }
        }

    }
}