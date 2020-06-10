package io.ffem.lite.preference

import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.util.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.util.PreferencesUtil
import java.util.*
import java.util.concurrent.TimeUnit

fun getSampleTestImageNumber(): Int {
    return try {
        PreferencesUtil.getString(App.app, R.string.testImageNumberKey, "").toInt()
    } catch (e: Exception) {
        -1
    }
}

fun isTestRunning(): Boolean {
    return getSampleTestImageNumber() > -1 && BuildConfig.DEBUG && isDiagnosticMode()
}

fun isDiagnosticMode(): Boolean {
    return PreferencesUtil.getBoolean(App.app, R.string.diagnosticModeKey, false)
}

fun useFlashMode(): Boolean {
    return PreferencesUtil.getBoolean(App.app, R.string.useFlashModeKey, false)
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
        PreferencesUtil.setBoolean(App.app, R.string.dummyImageKey, false)
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
}
