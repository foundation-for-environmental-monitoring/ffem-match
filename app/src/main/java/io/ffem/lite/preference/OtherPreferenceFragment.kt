package io.ffem.lite.preference

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.IS_CALIBRATION
import io.ffem.lite.preference.AppPreferences.runColorCardTest
import io.ffem.lite.preference.AppPreferences.useExternalSensor
import io.ffem.lite.ui.AboutActivity
import io.ffem.lite.ui.TestActivity

class OtherPreferenceFragment : PreferenceFragmentCompat() {
    private var calibrateClicked: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_other)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calibratePreference = findPreference<Preference>("calibrate")
        if (calibratePreference != null) {
            calibratePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (!calibrateClicked) {
                    calibrateClicked = true

                    calibrate()

//                    PreferencesUtil.setBoolean(requireContext(), IS_CALIBRATION, true)
//                    val intent = Intent(requireContext(), TestActivity::class.java)
//                    startActivity(intent)
                }
                true
            }
        }

        val aboutPreference = findPreference<Preference>(getString(R.string.aboutKey))
        if (aboutPreference != null) {
            aboutPreference.summary =
                App.getAppVersion(false) + "\n" + getString(R.string.app_in_development)
            aboutPreference.setOnPreferenceClickListener {
                val intent = Intent(activity, AboutActivity::class.java)
                activity?.startActivity(intent)
                true
            }
        }
    }

    private var startCalibrate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (useExternalSensor(requireContext()) || runColorCardTest()) {
                requireActivity().finish()
            }
        }

    private fun calibrate(): PreferenceFragmentCompat? {
        val intent = Intent(requireActivity(), TestActivity::class.java)
        intent.putExtra(IS_CALIBRATION, true)
        AppPreferences.setCalibration(requireContext(), true)
        startCalibrate.launch(intent)
        return null
    }

    override fun onResume() {
        super.onResume()
        calibrateClicked = false
    }
}



