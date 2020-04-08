package io.ffem.lite.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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

        app = this
    }

    companion object {

        const val ERROR_MESSAGE = "error_message"

        const val LOCAL_RESULT_EVENT = "result_event"
        const val CAPTURED_EVENT = "captured_event"
        const val ERROR_EVENT = "error_event"

        const val PERMISSIONS_MISSING_KEY = "permissions_missing"
        const val TEST_INFO_KEY = "test_info"
        const val TEST_ID_KEY = "test_id"
        const val TEST_NAME_KEY = "test_name"
        const val TEST_VALUE_KEY = "value"

        private lateinit var testConfig: TestConfig

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

        private val gson: Gson =
            GsonBuilder().registerTypeAdapter(object :
                TypeToken<MutableList<CalibrationValue>>() {}.type, CalibrationValuesDeserializer())
                .create()

        // Append a reversed list of calibration point values to the calibration values list
        // to represent the colors on the right side of color card
        internal class CalibrationValuesDeserializer : JsonDeserializer<List<CalibrationValue>> {
            override fun deserialize(
                json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?
            ): List<CalibrationValue> {
                val values = ArrayList<CalibrationValue>()
                json!!.asJsonArray.mapTo(values) {
                    CalibrationValue(value = it.asJsonObject.get("value").asFloat)
                }
                values.addAll(values.reversed().map { CalibrationValue(value = it.value) })
                return values
            }
        }

        fun getTestName(id: String): String {
            if (!::testConfig.isInitialized) {
                val input = app.resources.openRawResource(R.raw.calibration)
                val content = FileUtil.readTextFile(input)
                testConfig = gson.fromJson(content, TestConfig::class.java)
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

        fun getTestInfo(id: String): TestInfo? {
            if (!::testConfig.isInitialized) {
                val input = app.resources.openRawResource(R.raw.calibration)
                val content = FileUtil.readTextFile(input)
                testConfig = gson.fromJson(content, TestConfig::class.java)
            }

            for (test in testConfig.tests) {
                if (test.uuid == id || test.uuid!!.substring(30) == id) {
                    return test
                }
            }
            return null
        }

        fun getCalibration(id: String): List<CalibrationValue> {
            if (!::testConfig.isInitialized) {
                val input = app.resources.openRawResource(R.raw.calibration)
                val content = FileUtil.readTextFile(input)
                testConfig = gson.fromJson(content, TestConfig::class.java)
            }

            var calibration: List<CalibrationValue> = testConfig.tests[0].values
            for (test in testConfig.tests) {
                if (test.uuid!!.substring(30) == id) {
                    calibration = test.values
                    break
                }
            }

            return calibration
        }
    }
}
