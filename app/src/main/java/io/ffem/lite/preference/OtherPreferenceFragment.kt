package io.ffem.lite.preference

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.ui.AboutActivity

class OtherPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_other)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardTestPreference = findPreference<Preference>("cardTestPreference")
        if (cardTestPreference != null) {
            cardTestPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.layoutOther, CardTestPreferenceFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
        }

        val colorimetricTestPreference = findPreference<Preference>("colorimetricTestPreference")
        if (colorimetricTestPreference != null) {
            colorimetricTestPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.layoutOther, CuvettePreferenceFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
        }

        val dataPreference = findPreference<Preference>("dataSharingPreference")
        if (dataPreference != null) {
            dataPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.layoutOther, DataSharePreferenceFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
        }

        val aboutPreference = findPreference<Preference>(getString(R.string.aboutKey))
        if (aboutPreference != null) {
            aboutPreference.summary =
                App.getAppVersion(false)
            aboutPreference.setOnPreferenceClickListener {
                val intent = Intent(activity, AboutActivity::class.java)
                activity?.startActivity(intent)
                true
            }
        }
    }
}



