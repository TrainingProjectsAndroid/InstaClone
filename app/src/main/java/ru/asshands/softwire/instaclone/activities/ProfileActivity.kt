package ru.asshands.softwire.instaclone.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.GenericTypeIndicator
import kotlinx.android.synthetic.main.activity_profile.*

import ru.asshands.softwire.instaclone.models.User
import ru.asshands.softwire.instaclone.utils.FirebaseHelper
import ru.asshands.softwire.instaclone.utils.GlideApp
import ru.asshands.softwire.instaclone.utils.ValueEventListenerAdapter
import ru.asshands.softwire.instaclone.R
import java.lang.Exception


class ProfileActivity : BaseActivity() {
    private lateinit var mUser: User
    private lateinit var mFirebase: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tag = this.localClassName
        setupBottomNavigation()
        Log.d(tag, "OnCreate")

        edit_profile_btn.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        settings_image.setOnClickListener {
            val intent = Intent(this, ProfileSettingsActivity::class.java)
            startActivity(intent)
        }

        add_friends_image.setOnClickListener {
            val intent = Intent(this, AddFriendsActivity::class.java)
            startActivity(intent)
        }

        mFirebase = FirebaseHelper(this)
        mFirebase.currentUserReference().addValueEventListener(ValueEventListenerAdapter {
            mUser = it.asUser()!!
            profile_image.loadUserPhoto(mUser.photo)
            username_text.text = mUser.username
        })

        images_recycler.layoutManager = GridLayoutManager(this, 3)
        mFirebase.database
            .child("images")
            .child(mFirebase.currentUid()!!)
            .addValueEventListener(ValueEventListenerAdapter { imagesRequest ->
                //         if (imagesRequest.getValue(true) !== null) {
                try {
                    val imagesMap =
                        imagesRequest.getValue(true) as Map<String, String>
                    //не знаю как по другому сделать(
                    val images = imagesMap.map { it.value }
                    images_recycler.adapter = ImagesAdapter(images)
                } catch (e: TypeCastException) {
                    Log.e("TAG", e.toString())
                    showToast("Ошибка при загрузке изображений. См логи.")
                }
            })
    }
}

class ImagesAdapter(private val images: List<String>) :
    RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    class ViewHolder(val image: ImageView) : RecyclerView.ViewHolder(image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val image = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.image_item, parent, false) as ImageView
        return ViewHolder(image)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.image.loadImage(images[position])
    }

/*    private fun ImageView.loadImage(image: String) {
        GlideApp
            .with(this)
            .load(image)
            .centerCrop()
            .fallback(R.drawable.ic_broken_image)
            .into(this)
    }*/

    override fun getItemCount() = images.size
}

class SquareImageView(context: Context, attrs: AttributeSet) : ImageView(context, attrs) {
    // метод, который вызывается когда layout измеряет размер картинки:
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
