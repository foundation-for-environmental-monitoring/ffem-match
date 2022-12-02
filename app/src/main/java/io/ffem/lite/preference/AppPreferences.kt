package io.ffem.lite.preference

import android.content.Context
import android.hardware.Camera
import android.util.Pair
import androidx.preference.PreferenceManager
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.*
import io.ffem.lite.common.Constants.DEFAULT_MAXIMUM_BRIGHTNESS
import io.ffem.lite.common.Constants.DEFAULT_MINIMUM_BRIGHTNESS
import io.ffem.lite.common.Constants.DEFAULT_SHADOW_TOLERANCE
import io.ffem.lite.common.Constants.MAX_CARD_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.common.Constants.SAMPLING_COUNT_DEFAULT
import io.ffem.lite.common.Constants.SKIP_SAMPLING_COUNT
import io.ffem.lite.model.TestType
import io.ffem.lite.util.PreferencesUtil
import java.util.*
import java.util.concurrent.TimeUnit

fun getSampleTestImageNumber(): Int {
    var testImageNumber = -1
    if (isTestRunning()) {
        testImageNumber = try {
            PreferencesUtil
                .getString(App.app, R.string.testImageNumberKey, " -1").toInt()
        } catch (e: Exception) {
            -1
        }
    }
    return testImageNumber
}

fun getSampleTestImageNumberInt(): Int {
    return try {
        getSampleTestImageNumber()
    } catch (e: Exception) {
        -1
    }
}

fun isTestRunning(): Boolean {
    return BuildConfig.DEBUG && (isDiagnosticMode() || BuildConfig.INSTRUMENTED_TEST_RUNNING.get())
}

fun isDiagnosticMode(): Boolean {
    return PreferencesUtil.getBoolean(App.app, R.string.diagnosticModeKey, false)
}

fun useDummyImage(): Boolean {
    return isDiagnosticMode() && PreferencesUtil.getBoolean(
        App.app,
        R.string.dummyImageKey, false
    )
}

fun getColorDistanceTolerance(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.colorDistanceToleranceKey,
                MAX_COLOR_DISTANCE_RGB.toString()
            )
        )
    } else {
        MAX_COLOR_DISTANCE_RGB
    }
}

fun getCalibrationColorDistanceTolerance(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.maxCardColorDistanceAllowedKey,
                MAX_COLOR_DISTANCE_CALIBRATION.toString()
            )
        )
    } else {
        MAX_COLOR_DISTANCE_CALIBRATION
    }
}

fun getMinimumBrightness(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.minimum_brightness,
                DEFAULT_MINIMUM_BRIGHTNESS.toString()
            )
        )
    } else {
        DEFAULT_MINIMUM_BRIGHTNESS
    }
}

fun getMaximumBrightness(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.maximum_brightness,
                DEFAULT_MAXIMUM_BRIGHTNESS.toString()
            )
        )
    } else {
        DEFAULT_MAXIMUM_BRIGHTNESS
    }
}

fun getShadowTolerance(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.shadow_tolerance,
                DEFAULT_SHADOW_TOLERANCE.toString()
            )
        )
    } else {
        DEFAULT_SHADOW_TOLERANCE
    }
}

fun getCalibrationType(): Int {
    return PreferencesUtil.getString(
        App.app,
        R.string.calibrationTypePrefKey, "0"
    ).toInt()
}

object AppPreferences {

    fun enableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, true)
        PreferencesUtil.setLong(
            App.app, R.string.diagnosticEnableTimeKey,
            Calendar.getInstance().timeInMillis
        )
    }

    fun disableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, false)
        PreferencesUtil.setBoolean(App.app, R.string.testModeOnKey, false)
        PreferencesUtil.removeKey(App.app, R.string.testImageNumberKey)
    }

    fun checkDiagnosticModeExpiry() {
        if (isDiagnosticMode()) {
            val lastCheck = PreferencesUtil.getLong(App.app, R.string.diagnosticEnableTimeKey)
            if (TimeUnit.MILLISECONDS.toMinutes(Calendar.getInstance().timeInMillis - lastCheck) > 20) {
                disableDiagnosticMode()
            } else {
                PreferencesUtil.setLong(
                    App.app, R.string.diagnosticEnableTimeKey,
                    Calendar.getInstance().timeInMillis
                )
            }
        }
    }

    fun isCalibration(): Boolean {
        return PreferencesUtil.getBoolean(App.app, IS_CALIBRATION, false)
    }

    fun generateImageFileName() {
        PreferencesUtil.setString(App.app, IMAGE_FILE_NAME, UUID.randomUUID().toString())
    }

    fun getImageFilename(): String {
        return PreferencesUtil.getString(App.app, IMAGE_FILE_NAME, UUID.randomUUID().toString())!!
    }

    /**
     * The color distance tolerance for when matching colors.
     *
     * @return the tolerance value
     */
    fun getAveragingColorDistanceTolerance(context: Context): Int = try {
        if (isDiagnosticMode()) {
            PreferencesUtil.getString(
                context.applicationContext!!,
                R.string.colorAverageDistanceToleranceKey,
                java.lang.String.valueOf(MAX_COLOR_DISTANCE_CALIBRATION)
            ).toInt()
        } else {
            MAX_COLOR_DISTANCE_CALIBRATION
        }
    } catch (e: NullPointerException) {
        MAX_COLOR_DISTANCE_CALIBRATION
    }

    fun runColorCardTest(): Boolean {
        return PreferencesUtil.getBoolean(
            App.app,
            R.string.runColorCardPrefKey, false
        ) && isDiagnosticMode()
    }

    fun getSampleTestImageNumberInt(): Int {
        return try {
            getSampleTestImageNumber()
        } catch (e: Exception) {
            -1
        }
    }

    fun useCameraFlash(isCardTest: Boolean): Boolean {
        return if (isCardTest) {
            PreferencesUtil.getBoolean(
                App.app, R.string.torchCardKey, true
            )
        } else {
            PreferencesUtil.getBoolean(
                App.app, R.string.torchCuvetteKey, false
            )
        }
    }

    fun isSoundOn(context: Context): Boolean {
        return !isDiagnosticMode() ||
                PreferencesUtil.getBoolean(context, R.string.soundOnKey, true)
    }

    fun getCalibrationColorDistanceTolerance(context: Context, testType: TestType): Int {
        var key = MAX_COLOR_DISTANCE_CALIBRATION
        if (testType == TestType.CARD) {
            key = MAX_CARD_COLOR_DISTANCE_CALIBRATION
        }

        return if (isDiagnosticMode()) {
            Integer.parseInt(
                PreferencesUtil.getString(
                    context,
                    R.string.maxCardColorDistanceAllowedKey,
                    key.toString()
                )
            )
        } else {
            key
        }
    }

    /**
     * The color distance tolerance for when matching colors.
     *
     * @return the tolerance value
     */
    fun getColorDistanceTolerance(context: Context): Int {
        return if (isDiagnosticMode()) {
            PreferencesUtil.getString(
                context,
                R.string.colorDistanceToleranceKey,
                java.lang.String.valueOf(MAX_COLOR_DISTANCE_RGB)
            ).toInt()
        } else {
            MAX_COLOR_DISTANCE_RGB
        }
    }

    fun useFaceDownMode(context: Context): Boolean {
        return (isDiagnosticMode()
                && PreferencesUtil.getBoolean(context, R.string.useFaceDownModeKey, false))
    }

    fun useExternalSensor(context: Context): Boolean {
        return PreferencesUtil.getBoolean(
            context.applicationContext!!,
            R.string.useExternalCameraKey, false
        ) && isDiagnosticMode()
    }


    fun getCameraZoom(context: Context): Int {
        return if (isDiagnosticMode()) {
            PreferencesUtil.getInt(
                context,
                R.string.cameraZoomPercentKey, 0
            )
        } else {
            0
        }
    }

    fun getCameraResolution(context: Context): Pair<Int, Int> {
        val res = Pair(640, 480)
        return try {
            if (isDiagnosticMode()) {
                val resolution = PreferencesUtil.getString(
                    context.applicationContext!!,
                    R.string.cameraResolutionKey, "640-480"
                )
                val resolutions = resolution.split("-").toTypedArray()
                val widthTemp = resolutions[0].toInt()
                val heightTemp = resolutions[1].toInt()
                val width = kotlin.math.max(heightTemp, widthTemp)
                val height = kotlin.math.min(heightTemp, widthTemp)
                Pair(width, height)
            } else {
                res
            }
        } catch (e: Exception) {
            res
        }
    }

    fun getCameraFocusMode(focusModes: List<String?>): String {
        return when {
            focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED) -> {
                Camera.Parameters.FOCUS_MODE_FIXED
            }
            focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY) -> {
                Camera.Parameters.FOCUS_MODE_INFINITY
            }
            focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) -> {
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
            focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) -> {
                Camera.Parameters.FOCUS_MODE_AUTO
            }
            else -> {
                ""
            }
        }
    }

    fun getSamplingTimes(context: Context): Int {
        val samplingTimes: Int = if (isDiagnosticMode()) {
            PreferencesUtil.getString(
                context.applicationContext!!,
                R.string.samplingsTimeKey,
                java.lang.String.valueOf(SAMPLING_COUNT_DEFAULT)
            ).toInt()
        } else {
            SAMPLING_COUNT_DEFAULT
        }
        //Add skip count as the first few samples may not be valid
        return samplingTimes + SKIP_SAMPLING_COUNT
    }

    fun setCalibration(context: Context, isCalibration: Boolean) {
        PreferencesUtil.setBoolean(context, IS_CALIBRATION, isCalibration)
    }

    fun setTestType(context: Context, testType: TestType) {
        PreferencesUtil.setString(context, START_TEST_TYPE, testType.toString())
    }

    fun getTestType(): TestType {
        val value = PreferencesUtil.getString(App.app, START_TEST_TYPE, TestType.CARD.toString())
        return if (value.isNullOrEmpty()) {
            TestType.CARD
        } else {
            TestType.valueOf(value)
        }
    }

    fun returnDummyResults(context: Context): Boolean {
        return PreferencesUtil.getBoolean(
            context.applicationContext!!,
            R.string.dummyResultKey, false
        ) && isDiagnosticMode()
    }

    fun getShowDebugInfo(context: Context): Boolean = PreferencesUtil.getBoolean(
        context.applicationContext!!, R.string.showDebugMessagesKey, false
    )

    fun setShowDebugInfo(value: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.app)
        sharedPreferences!!.edit()
            .putBoolean(App.app.getString(R.string.showDebugMessagesKey), value)
            .apply()
    }

    fun setShareData(value: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.app)
        sharedPreferences!!.edit().putBoolean(App.app.getString(R.string.dataSharingKey), value)
            .apply()
        sharedPreferences.edit().putBoolean(DATA_SHARING_SET, true).apply()
    }

    fun getShareData(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.app)
        return sharedPreferences.getBoolean(App.app.getString(R.string.dataSharingKey), false)
    }

    fun isShareDataSet(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.app)
        return sharedPreferences.getBoolean(DATA_SHARING_SET, false)
    }

    fun getEmailAddress(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.app)
        return sharedPreferences.getString(App.app.getString(R.string.emailAddressKey), "")
            .toString()
    }

    fun setEmailAddress(value: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.app)
        sharedPreferences!!.edit().putString(App.app.getString(R.string.emailAddressKey), value)
            .apply()
    }

    fun getLastSelectedTestsTab(context: Context): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return if (sharedPreferences!!.contains(LAST_TESTS_TAB_SELECTED))
            sharedPreferences.getString(LAST_TESTS_TAB_SELECTED, "").toString() else ""
    }

    fun setLastSelectedTestsTab(value: String, context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences!!.edit().putString(LAST_TESTS_TAB_SELECTED, value).apply()
    }

//    fun isAppUpdateCheckRequired(): Boolean {
//        if (BuildConfig.INSTRUMENTED_TEST_RUNNING.get()) {
//            return true
//        }
//        val lastCheck = PreferencesUtil.getLong(App.app, "lastUpdateCheck")
//        return TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - lastCheck) > 0
//    }
}
