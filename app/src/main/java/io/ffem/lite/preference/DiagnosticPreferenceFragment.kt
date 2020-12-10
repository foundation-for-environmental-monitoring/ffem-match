package io.ffem.lite.preference

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R.string
import io.ffem.lite.R.xml
import io.ffem.lite.util.DEFAULT_MAXIMUM_BRIGHTNESS
import io.ffem.lite.util.DEFAULT_MINIMUM_BRIGHTNESS
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

        setupMinimumBrightness()

        setupMaximumBrightness()
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

    private fun setupMinimumBrightness() {
        val minimumBrightnessPref =
            findPreference<Preference>(getString(string.minimum_brightness)) as EditTextPreference

        minimumBrightnessPref.summary = minimumBrightnessPref.text
        minimumBrightnessPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                var value = newValue
                try {
                    if (Integer.parseInt(value.toString()) > MAX_BRIGHTNESS) {
                        value = MAX_BRIGHTNESS
                    }
                    if (Integer.parseInt(value.toString()) < 1) {
                        value = 1
                    }
                } catch (e: Exception) {
                    value = DEFAULT_MINIMUM_BRIGHTNESS
                }
                minimumBrightnessPref.text = value.toString()
                minimumBrightnessPref.summary = value.toString()
                false
            }
    }

    private fun setupMaximumBrightness() {
        val maximumBrightnessPref =
            findPreference<Preference>(getString(string.maximum_brightness)) as EditTextPreference

        maximumBrightnessPref.summary = maximumBrightnessPref.text
        maximumBrightnessPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                var value = newValue
                try {
                    if (Integer.parseInt(value.toString()) > MAX_BRIGHTNESS) {
                        value = MAX_BRIGHTNESS
                    }
                    if (Integer.parseInt(value.toString()) < 1) {
                        value = 1
                    }
                } catch (e: Exception) {
                    value = DEFAULT_MAXIMUM_BRIGHTNESS
                }
                maximumBrightnessPref.text = value.toString()
                maximumBrightnessPref.summary = value.toString()
                false
            }
    }

    companion object {
        private const val MAX_TOLERANCE = 399
        private const val MAX_BRIGHTNESS = 255
    }
}