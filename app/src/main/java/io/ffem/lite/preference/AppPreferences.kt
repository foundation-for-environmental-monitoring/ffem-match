package io.ffem.lite.preference

import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.util.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.util.PreferencesUtil

const val IS_TEST_MODE = false

fun isTestMode(): Boolean {
    return IS_TEST_MODE || isDiagnosticMode() && PreferencesUtil.getBoolean(
        App.app, R.string.testModeOnKey, false
    )
}

fun isSoundOn(): Boolean {
    return !isDiagnosticMode() || PreferencesUtil.getBoolean(App.app, R.string.soundOnKey, true)
}

fun sendDummyImage(): Boolean {
    return isDiagnosticMode() && PreferencesUtil.getBoolean(App.app, R.string.dummyImageKey, false)
}

fun isDiagnosticMode(): Boolean {
    return PreferencesUtil.getBoolean(App.app, R.string.diagnosticModeKey, false)
}

fun useFlashMode(): Boolean {
    return PreferencesUtil.getBoolean(App.app, R.string.useFlashModeKey, false)
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
    }

    fun disableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, false)
        PreferencesUtil.setBoolean(App.app, R.string.testModeOnKey, false)
        PreferencesUtil.setBoolean(App.app, R.string.dummyImageKey, false)
    }
}
