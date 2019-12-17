package io.ffem.lite.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import com.google.gson.Gson
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.model.TestConfig
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.FileUtil
import io.ffem.lite.util.PreferencesUtil
import timber.log.Timber
import java.util.*

class App : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        app = this

        @Suppress("ConstantConditionIf")
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        app = this
    }

    /**
     * Sets the language of the app on start. The language can be one of system language, language
     * set in the app preferences or language requested via the languageCode parameter
     *
     * @param languageCode If null uses language from app preferences else uses this value
     */
    fun setAppLanguage(languageCode: String, isExternal: Boolean, handler: Handler?) {

        try {
            val locale: Locale

            var code: String = languageCode

            //the languages supported by the app
            val supportedLanguages = resources.getStringArray(R.array.language_codes)

            //the current system language set in the device settings
            val currentSystemLanguage = Locale.getDefault().language.substring(0, 2)

            //the language the system was set to the last time the app was run
            val previousSystemLanguage =
                PreferencesUtil.getString(this, R.string.systemLanguageKey, "")

            //if the system language was changed in the device settings then set that as the app language
            if (previousSystemLanguage != currentSystemLanguage && listOf(*supportedLanguages).contains(
                    currentSystemLanguage
                )
            ) {
                PreferencesUtil.setString(this, R.string.systemLanguageKey, currentSystemLanguage)
                PreferencesUtil.setString(this, R.string.languageKey, currentSystemLanguage)
            }

            if (code == "" || !listOf(*supportedLanguages).contains(code)) {
                //if requested language code is not supported then use language from preferences
                code = PreferencesUtil.getString(this, R.string.languageKey, "")
                if (!listOf(*supportedLanguages).contains(code)) {
                    //no language was selected in the app settings so use the system language
                    val currentLanguage = resources.configuration.locale.language
                    code = when {
                        currentLanguage == currentSystemLanguage -> //app is already set to correct language
                            return
                        listOf(*supportedLanguages).contains(currentSystemLanguage) -> //set to system language
                            currentSystemLanguage
                        else -> "en"
                    }
                }
            }

            val res = resources
            val dm = res.displayMetrics
            val config = res.configuration

            locale = Locale(code, Locale.getDefault().country)

            //if the app language is not already set to languageCode then set it now
            if (!config.locale.language.substring(0, 2).equals(
                    code, ignoreCase = true
                ) || !config.locale.country.equals(Locale.getDefault().country, ignoreCase = true)
            ) {

                config.locale = locale
                config.setLayoutDirection(locale)
                res.updateConfiguration(config, dm)

                //if this session was launched from an external app then do not restart this app
                if (!isExternal && handler != null) {
                    val msg = handler.obtainMessage()
                    handler.sendMessage(msg)
                }
            }
        } catch (ignored: Exception) {
            // do nothing
        }
    }

    companion object {

        const val ERROR_MESSAGE = "error_message"
        const val LOCAL_RESULT_EVENT = "result_event"
        const val CAPTURED_EVENT = "captured_event"

        const val ERROR_EVENT = "error_event"

        const val SOUND_ON = true

        const val RESULT_SOUND_PLAYED_KEY = "result_sound_played"
        const val CONNECTION_ERROR_NOTIFIED_KEY = "connection_error_notified"
        const val PERMISSIONS_MISSING_KEY = "permissions_missing"

        // Keys
        const val FILE_PATH_KEY = "file_path"
        const val TEST_ID_KEY = "test_id"
        const val TEST_RESULT = "test_result"
        const val TEST_NAME_KEY = "test_name"

        const val API_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000"

        lateinit var testConfig: TestConfig

        /**
         * Gets the singleton app object.
         *
         * @return the singleton app
         */
        lateinit var app: App
            private set // Singleton

        /**
         * Gets the app version.
         *
         * @return The version name and number
         */
        fun getVersionName(): String {
            try {
                val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
                return packageInfo.versionName
            } catch (ignored: PackageManager.NameNotFoundException) {
                // do nothing
            }
            return ""
        }

        /**
         * Gets the app version.
         *
         * @return The version name and number
         */
        fun getAppVersion(): String {
            var version = ""
            try {
                val context = app
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                @Suppress("DEPRECATION") val versionCode: Long =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        packageInfo.versionCode.toLong()
                    }

                version = if (isDiagnosticMode()) {
                    String.format("%s (Build %s)", packageInfo.versionName, versionCode)
                } else {
                    String.format(
                        "%s %s", context.getString(R.string.version),
                        packageInfo.versionName
                    )
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
                // do nothing
            }

            return version
        }

        fun openAppPermissionSettings(context: Activity?) {
            if (context == null) {
                return
            }
            val i = Intent()
            i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            i.addCategory(Intent.CATEGORY_DEFAULT)
            i.data = Uri.parse("package:" + context.packageName)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(i)
        }

        fun getTestName(id: String): String {
            if (!::testConfig.isInitialized) {
                val input = app.resources.openRawResource(R.raw.calibration)
                val content = FileUtil.readTextFile(input)
                testConfig = Gson().fromJson(content, TestConfig::class.java)
            }

            var testName = ""
            for (test in testConfig.tests) {
                if (test.uuid!!.substring(30) == id) {
                    testName = test.name!!
                    break
                }
            }
            return testName
        }

    }
}
