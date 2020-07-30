package io.ffem.lite.preference

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R.string
import io.ffem.lite.R.xml
import io.ffem.lite.app.App
import io.ffem.lite.ui.BarcodeActivity
import io.ffem.lite.util.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.util.PreferencesUtil

const val PERMISSION_REQUEST = 103

class DiagnosticPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(xml.pref_diagnostic)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCalibrationPreference()

        setupDistancePreference()

        setupAverageDistancePreference()
    }

    private fun setCalibrationPreference() {
        val calibratePreference = findPreference<Preference>("calibrate")
        if (calibratePreference != null) {
            calibratePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (useDummyImage()) {
                    val permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )

                    if (!hasPermission(*permissions))
                        askPermission(permissions = *permissions, requestCode = PERMISSION_REQUEST)

                } else {
                    PreferencesUtil.setBoolean(requireContext(), App.IS_CALIBRATION, true)
                    val intent = Intent(requireContext(), BarcodeActivity::class.java)
                    startActivityForResult(intent, 100)
                }
                true
            }
        }
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

    private fun hasPermission(vararg permissions: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            permissions.all { singlePermission ->
                requireContext().checkSelfPermission(singlePermission) ==
                        PackageManager.PERMISSION_GRANTED
            }
        else true
    }

    @Suppress("SameParameterValue")
    private fun askPermission(vararg permissions: String, @IntRange(from = 0) requestCode: Int) =
        ActivityCompat.requestPermissions(requireActivity(), permissions, requestCode)

    companion object {
        private const val MAX_TOLERANCE = 399
    }
}