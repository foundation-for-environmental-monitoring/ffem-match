package io.ffem.lite.preference

import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.util.PreferencesUtil

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
        PreferencesUtil.setBoolean(App.app, R.string.dummyResultKey, false)
    }
}
