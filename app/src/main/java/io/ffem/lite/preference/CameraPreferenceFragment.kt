package io.ffem.lite.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R.xml

class CameraPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(xml.pref_camera)
    }
}