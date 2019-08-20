package io.ffem.lite.preference

import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.util.PreferencesUtil

const val IS_TEST_MODE = false

fun isTestMode(): Boolean {
    return IS_TEST_MODE || AppPreferences.isDiagnosticMode() && PreferencesUtil.getBoolean(
        App.app, R.string.testModeOnKey, false
    )
}

fun isSoundOn(): Boolean {
    return !AppPreferences.isDiagnosticMode() || PreferencesUtil.getBoolean(App.app, R.string.soundOnKey, true)
}

fun sendDummyImage(): Boolean {
    return AppPreferences.isDiagnosticMode() && PreferencesUtil.getBoolean(App.app, R.string.dummyImageKey, false)
}

object AppPreferences {

    fun isDiagnosticMode(): Boolean {
        return PreferencesUtil.getBoolean(App.app, R.string.diagnosticModeKey, false)
    }

    fun enableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, true)
    }

    fun disableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, false)
        PreferencesUtil.setBoolean(App.app, R.string.testModeOnKey, false)
        PreferencesUtil.setBoolean(App.app, R.string.dummyImageKey, false)
    }
}
