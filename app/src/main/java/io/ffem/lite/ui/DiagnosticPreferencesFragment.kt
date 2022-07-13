package io.ffem.lite.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_RGB

class DiagnosticPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.diagnostic_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDistancePreference()
        setupAverageDistancePreference()
        setupTestImagePreference()
    }

    private fun setupDistancePreference() {
        val distancePreference =
            findPreference<Preference>(getString(R.string.colorDistanceToleranceKey)) as EditTextPreference
        distancePreference.summary = distancePreference.text
        distancePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                var value = newValue
                try {
                    if (value.toString().toInt() > MAX_TOLERANCE) {
                        value = MAX_TOLERANCE
                    }
                    if (value.toString().toInt() < 1) {
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
            findPreference<Preference>(getString(R.string.colorAverageDistanceToleranceKey)) as EditTextPreference
        distancePreference.summary = distancePreference.text
        distancePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                var value = newValue
                try {
                    if (value.toString().toInt() > MAX_TOLERANCE) {
                        value = MAX_TOLERANCE
                    }
                    if (value.toString().toInt() < 1) {
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

    private fun setupTestImagePreference() {
        val testImagePreference =
            findPreference<EditTextPreference>(getString(R.string.testImageNumberKey))
        @Suppress("ConstantConditionIf")
        if (BuildConfig.DEBUG) {
            testImagePreference!!.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            getSampleImageSummary(testImagePreference.text, testImagePreference)
            testImagePreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                    testImagePreference.text = newValue.toString()
                    getSampleImageSummary(newValue, testImagePreference)
                    false
                }
        } else {
            testImagePreference?.isVisible = false
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

    companion object {
        private const val MAX_TOLERANCE = 399
    }
}