package io.ffem.lite.preference

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.IS_CALIBRATION
import io.ffem.lite.ui.AboutActivity
import io.ffem.lite.ui.BarcodeActivity
import io.ffem.lite.util.PreferencesUtil

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

                    PreferencesUtil.setBoolean(requireContext(), IS_CALIBRATION, true)
                    val intent = Intent(requireContext(), BarcodeActivity::class.java)
                    startActivity(intent)
                }
                true
            }
        }

        val aboutPreference = findPreference<Preference>(getString(R.string.aboutKey))
        if (aboutPreference != null) {
            aboutPreference.summary =
                App.getAppVersion() + "\n" + getString(R.string.app_in_development)
            aboutPreference.setOnPreferenceClickListener {
                val intent = Intent(activity, AboutActivity::class.java)
                activity?.startActivity(intent)
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        calibrateClicked = false
    }
}



