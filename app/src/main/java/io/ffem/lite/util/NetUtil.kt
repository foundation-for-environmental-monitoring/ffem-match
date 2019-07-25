package io.ffem.lite.util

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import io.ffem.lite.R

object NetUtil {

    fun isInternetConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo

        val result = networkInfo != null && networkInfo.isConnected

        if (!result) {
            Toast.makeText(
                context, context.getString(R.string.no_Internet_connection),
                Toast.LENGTH_LONG
            ).show()
        }

        return result
    }
}