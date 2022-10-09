package io.ffem.lite.preference

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R
import io.ffem.lite.common.IS_CALIBRATION
import io.ffem.lite.model.TestType
import io.ffem.lite.ui.TestActivity


class CuvettePreferenceFragment : PreferenceFragmentCompat() {
    private var calibrateClicked: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_cuvette_test)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calibratePreference = findPreference<Preference>("calibrate")
        if (calibratePreference != null) {
            calibratePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (!calibrateClicked) {
                    calibrateClicked = true
                    calibrate()
                }
                true
            }
        }
    }

    private var startCalibrate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (AppPreferences.useExternalSensor(requireContext()) || AppPreferences.runColorCardTest()) {
                requireActivity().finish()
            }
        }

    private fun calibrate(): PreferenceFragmentCompat? {
        val intent = Intent(requireActivity(), TestActivity::class.java)
        intent.putExtra(IS_CALIBRATION, true)
        AppPreferences.setTestType(requireContext(), TestType.CUVETTE)
        AppPreferences.setCalibration(requireContext(), true)
        startCalibrate.launch(intent)
        return null
    }

    override fun onResume() {
        super.onResume()
        calibrateClicked = false
    }
}
