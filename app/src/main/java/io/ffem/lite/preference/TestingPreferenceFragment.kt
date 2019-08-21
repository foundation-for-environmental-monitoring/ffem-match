package io.ffem.lite.preference

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R

class TestingPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_testing)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBackgroundColor(view)

        val testModeOnPreference = findPreference<Preference>(getString(R.string.testModeOnKey))
        testModeOnPreference?.setOnPreferenceClickListener {
            setBackgroundColor(view)
            true
        }
    }

    private fun setBackgroundColor(view: View) {
        if (isTestMode()) {
            view.setBackgroundColor(Color.rgb(255, 165, 0))
        } else {
            view.setBackgroundColor(Color.rgb(255, 240, 220))
        }
    }
}
