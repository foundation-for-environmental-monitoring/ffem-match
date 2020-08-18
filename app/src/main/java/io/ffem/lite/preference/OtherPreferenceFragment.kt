package io.ffem.lite.preference

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.IS_CALIBRATION
import io.ffem.lite.ui.AboutActivity
import io.ffem.lite.ui.BarcodeActivity
import io.ffem.lite.util.PreferencesUtil

class OtherPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_other)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calibratePreference = findPreference<Preference>("calibrate")
        if (calibratePreference != null) {
            calibratePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (useDummyImage()) {
                    val permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )

                    if (!isHasPermission(*permissions))
                        askPermission(permissions = *permissions, requestCode = PERMISSION_REQUEST)

                } else {
                    PreferencesUtil.setBoolean(requireContext(), IS_CALIBRATION, true)
                    val intent = Intent(requireContext(), BarcodeActivity::class.java)
                    startActivityForResult(intent, 100)
                }
                true
            }
        }

        val aboutPreference = findPreference<Preference>(getString(R.string.aboutKey))
        if (aboutPreference != null) {
            aboutPreference.summary = App.getAppVersion()
            aboutPreference.setOnPreferenceChangeListener { preference: Preference, any: Any ->
                val intent = Intent(activity, AboutActivity::class.java)
                activity?.startActivity(intent)
                true
            }
        }
    }


    private fun isHasPermission(vararg permissions: String): Boolean {
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
}



