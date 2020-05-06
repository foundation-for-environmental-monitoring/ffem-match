package io.ffem.lite.preference

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.BuildConfig
import io.ffem.lite.R

class TestingPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_testing)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestImagePreference()
    }

    private fun setupTestImagePreference() {
        val testImagePreference =
            findPreference<Preference>(getString(R.string.testImageNumberKey)) as EditTextPreference
        @Suppress("ConstantConditionIf")
        if (BuildConfig.DEBUG) {
            getSampleImageSummary(testImagePreference.text, testImagePreference)
            testImagePreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                    testImagePreference.text = newValue.toString()
                    getSampleImageSummary(newValue, testImagePreference)
                    false
                }
        } else {
            testImagePreference.isVisible = false
        }
    }

    private fun getSampleImageSummary(newValue: Any?, testImagePreference: EditTextPreference) {
        var value = ""
        if (newValue != null) {
            value = newValue.toString()
        }

        try {
            if (value.toInt() > -1 && value.toInt() < 50) {
                testImagePreference.summary = getString(R.string.test_mode_image_selected, value)
            } else {
                testImagePreference.summary = getString(R.string.no_image_selected)
            }
        } catch (e: Exception) {
            testImagePreference.text = ""
            testImagePreference.summary = getString(R.string.no_image_selected)
        }
    }
}
