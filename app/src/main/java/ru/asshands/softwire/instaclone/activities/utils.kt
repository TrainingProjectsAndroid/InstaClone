package ru.asshands.softwire.instaclone.activities

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import ru.asshands.softwire.instaclone.R
import ru.asshands.softwire.instaclone.models.FeedPost
import ru.asshands.softwire.instaclone.models.User
import ru.asshands.softwire.instaclone.utils.GlideApp

fun Context.showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

fun coordinateBtnAndInputs(btn: Button, vararg inputs: EditText) {
    val watcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            btn.isEnabled = inputs.all { it.text.isNotEmpty() }
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }

    inputs.forEach { it.addTextChangedListener(watcher) }
    btn.isEnabled = inputs.all { it.text.isNotEmpty() }
}

fun Editable.toStringOrNull(): String? {
    val str = toString()
    return if (str.isEmpty()) null else str
}

fun ImageView.loadUserPhoto(url: String?) = ifNotDestroed {
    GlideApp
        .with(this)
        .load(url)
        .fallback(R.drawable.person)
        .into(this)
}

fun ImageView.loadImage(image: String?) = ifNotDestroed {
    GlideApp
        .with(this)
        .load(image)
        .centerCrop()
        .fallback(R.drawable.ic_broken_image)
        .into(this)
}

private fun View.ifNotDestroed(block: () -> Unit) {
    if (!(context as Activity).isDestroyed) {
        block()
    }
}

fun <T> task(block: (TaskCompletionSource<T>) -> Unit): Task<T> {
    //Нифига не понял но очень интересно))
    val taskSource = TaskCompletionSource<T>()
    block(taskSource)
    return taskSource.task
}

class TaskSourceOnCompleteListener<T>(private val taskSource: TaskCompletionSource<T>) :
    OnCompleteListener<T> {
    override fun onComplete(task: Task<T>) {
        if (task.isSuccessful) {
            taskSource.setResult(task.result)
        } else {
            taskSource.setException(task.exception!!)
        }
    }

}

fun DataSnapshot.asUser(): User? = getValue(User::class.java)?.copy(uid = key!!)

fun DataSnapshot.asFeedPost(): FeedPost? = getValue(FeedPost::class.java)?.copy(id = key!!)
//fun DataSnapshot.asFeedPost(): FeedPost? = asFeedPost()?.copy(id = key!!)


fun DatabaseReference.setValueTrueOrRemove(value: Boolean) =
    if (value) {
        setValue(true)
    } else {
        removeValue()
    }