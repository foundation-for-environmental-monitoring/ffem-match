package io.ffem.lite.helper

import android.content.Context
import android.provider.Settings
import java.util.*

object ApkHelper {

    /**
     * Checks if the app was installed from the app store or from an install file.
     * source: http://stackoverflow.com/questions/37539949/detect-if-an-app-is-installed-from-play-store
     *
     * @param context The context
     * @return True if app was not installed from the store
     */
    fun isNonStoreVersion(context: Context): Boolean {

        val validInstallers = ArrayList(
            listOf("com.android.vending", "com.google.android.feedback")
        )

        try {
            @Suppress("DEPRECATION")
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            return installer == null || !validInstallers.contains(installer)

        } catch (ignored: Exception) {
            // do nothing
        }

        return true
    }

    fun isTestDevice(context: Context): Boolean {
        try {
            val testLabSetting: String =
                Settings.System.getString(context.contentResolver, "firebase.test.lab")
            return "true" == testLabSetting
        } catch (ignored: Exception) {
            // do nothing
        }
        return false
    }
}
