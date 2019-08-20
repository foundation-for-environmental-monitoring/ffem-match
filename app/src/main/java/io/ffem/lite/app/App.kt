package io.ffem.lite.app

import android.content.pm.PackageManager
import android.os.Handler
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.util.PreferencesUtil
import timber.log.Timber
import java.util.*

class App : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        app = this

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

            var code: String? = languageCode

            //the languages supported by the app
            val supportedLanguages = resources.getStringArray(R.array.language_codes)

            //the current system language set in the device settings
            val currentSystemLanguage = Locale.getDefault().language.substring(0, 2)

            //the language the system was set to the last time the app was run
            val previousSystemLanguage = PreferencesUtil.getString(this, R.string.systemLanguageKey, "")

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
                    code!!,
                    ignoreCase = true
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

        // Keys
        const val FILE_PATH_KEY = "file_path"
        const val TEST_ID_KEY = "test_id"
        const val TEST_NAME_KEY = "test_name"

        const val API_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000"

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
        fun getAppVersion(): String {
            var version = ""
            try {
                val context = app
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                version = if (AppPreferences.isDiagnosticMode()) {
                    String.format("%s (Build %s)", packageInfo.versionName, packageInfo.versionCode)
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
    }
}
