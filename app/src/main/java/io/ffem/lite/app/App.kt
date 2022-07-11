package io.ffem.lite.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.TestConfig
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.FileUtil
import timber.log.Timber
import java.lang.reflect.Type

class App : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        app = this

        @Suppress("ConstantConditionIf")
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    companion object {

        private lateinit var testConfig: TestConfig

        /**
         * Gets the singleton app object.
         *
         * @return the singleton app
         */
        lateinit var app: App
            private set // Singleton

//        /**
//         * Gets the app version.
//         *
//         * @return The version name and number
//         */
//        fun getVersionName(): String {
//            try {
//                val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
//                return packageInfo.versionName
//            } catch (ignored: PackageManager.NameNotFoundException) {
//                // do nothing
//            }
//            return ""
//        }

        /**
         * Gets the app version.
         *
         * @return The version name and number
         */
        fun getAppVersion(includeCode: Boolean): String {
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

                version = when {
                    includeCode -> {
                        String.format(
                            "%s (%s)",
                            packageInfo.versionName.replace("Alpha", "").trim(),
                            versionCode
                        )
                    }
                    isDiagnosticMode() -> {
                        String.format("%s (Build %s)", packageInfo.versionName, versionCode)
                    }
                    else -> {
                        String.format(
                            "%s %s", context.getString(R.string.version),
                            packageInfo.versionName
                        )
                    }
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
                // do nothing
            }

            return version
        }

        fun openAppPermissionSettings() {
            val i = Intent()
            i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            i.addCategory(Intent.CATEGORY_DEFAULT)
            i.data = Uri.parse("package:" + app.packageName)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            app.startActivity(i)
        }

        private val gson: Gson =
            GsonBuilder().registerTypeAdapter(object :
                TypeToken<MutableList<CalibrationValue>>() {}.type, CalibrationValuesDeserializer())
                .create()

        // Append a reversed list of calibration point values to the calibration values list
        // to represent the colors on the right side of color card
        internal class CalibrationValuesDeserializer :
            JsonDeserializer<MutableList<CalibrationValue>> {
            override fun deserialize(
                json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?
            ): MutableList<CalibrationValue> {
                val values = ArrayList<CalibrationValue>()
                json!!.asJsonArray.mapTo(values) {
                    CalibrationValue(
                        value = it.asJsonObject.get("value").asDouble,
                        color = Color.TRANSPARENT,
                        calibrate = it.asJsonObject.get("calibrate")?.asBoolean ?: false
                    )
                }

                values.addAll(values.reversed().map {
                    CalibrationValue(
                        value = it.value,
                        color = Color.TRANSPARENT,
                        calibrate = it.calibrate
                    )
                })
                return values
            }
        }

        /**
         * Adds the hyphen separators to the parameter id if not already included
         */
        private fun checkHyphens(str: String): String {
            return if (!str.contains("-")) {
                val stringBuilder = StringBuilder(str)
                stringBuilder.insert(2, '-')
                stringBuilder.insert(5, '-')
                stringBuilder.toString()
            } else {
                str
            }
        }

        fun getTestInfo(parameterId: String): TestInfo? {
            if (parameterId.isNotEmpty()) {
                val id = checkHyphens(parameterId.uppercase())

                // The second character in the parameter id specifies the color card type
                val input = app.resources.openRawResource(R.raw.tests_circle)
                val content = FileUtil.readTextFile(input)
                testConfig = gson.fromJson(content, TestConfig::class.java)

                for (test in testConfig.tests) {
                    if (test.uuid.uppercase() == id) {
                        val newTest = test.copy()
                        newTest.fileName = AppPreferences.getImageFilename()
                        return newTest
                    }
                }
            }
            return null
        }

        fun getParameterValues(id: String): List<CalibrationValue> {
            val test = getTestInfo(id)
            return test?.subTest()?.values ?: emptyList()
        }
    }
}
