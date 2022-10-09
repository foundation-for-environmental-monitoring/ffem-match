package io.ffem.lite.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R


class CardTestPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_card_test)
    }
}
