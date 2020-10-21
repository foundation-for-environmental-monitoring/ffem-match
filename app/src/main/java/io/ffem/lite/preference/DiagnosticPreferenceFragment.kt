package io.ffem.lite.preference

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R.string
import io.ffem.lite.R.xml
import io.ffem.lite.util.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_RGB

class DiagnosticPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(xml.pref_diagnostic)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDistancePreference()

        setupAverageDistancePreference()
    }

    private fun setupDistancePreference() {
        val distancePreference =
            findPreference<Preference>(getString(string.colorDistanceToleranceKey)) as EditTextPreference

        distancePreference.summary = distancePreference.text
        distancePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                var value = newValue
                try {
                    if (Integer.parseInt(value.toString()) > MAX_TOLERANCE) {
                        value = MAX_TOLERANCE
                    }
                    if (Integer.parseInt(value.toString()) < 1) {
                        value = 1
                    }
                } catch (e: Exception) {
                    value = MAX_COLOR_DISTANCE_RGB
                }
                distancePreference.text = value.toString()
                distancePreference.summary = value.toString()
                false
            }
    }

    private fun setupAverageDistancePreference() {
        val distancePreference =
            findPreference<Preference>(getString(string.maxCardColorDistanceAllowedKey)) as EditTextPreference

        distancePreference.summary = distancePreference.text
        distancePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                var value = newValue
                try {
                    if (Integer.parseInt(value.toString()) > MAX_TOLERANCE) {
                        value = MAX_TOLERANCE
                    }
                    if (Integer.parseInt(value.toString()) < 1) {
                        value = 1
                    }
                } catch (e: Exception) {
                    value = MAX_COLOR_DISTANCE_CALIBRATION
                }
                distancePreference.text = value.toString()
                distancePreference.summary = value.toString()
                false
            }
    }

    companion object {
        private const val MAX_TOLERANCE = 399
    }
}