package io.ffem.lite.common

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import io.ffem.lite.R

object TestHelper {

    fun enterDiagnosticMode() {
        for (i in 0..9) {
            onView(withId(R.id.textVersion)).perform(click())
        }
    }

    fun leaveDiagnosticMode() {
        onView(withId(R.id.fabDisableDiagnostics)).perform(click())
    }

    fun clearPreferences(activityTestRule: ActivityTestRule<*>) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activityTestRule.activity)
        prefs.edit().clear().apply()
    }
}
