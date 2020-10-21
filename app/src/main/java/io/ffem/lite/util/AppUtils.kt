package io.ffem.lite.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.ffem.lite.R

fun View.snackBar(message: String, duration: Int = BaseTransientBottomBar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}

fun View.snackBarAction(
    message: String,
    listener: View.OnClickListener
) {
    val snackbar = Snackbar.make(this, message, BaseTransientBottomBar.LENGTH_LONG)
    snackbar.setAction(R.string.settings, listener)
    snackbar.show()
}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
