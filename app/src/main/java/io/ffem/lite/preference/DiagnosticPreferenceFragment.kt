/*
 * Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Caddisfly.
 *
 * Akvo Caddisfly is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Caddisfly is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Caddisfly. If not, see <http://www.gnu.org/licenses/>.
 */

package io.ffem.lite.preference

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R.string
import io.ffem.lite.R.xml
import io.ffem.lite.util.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_RGB

class DiagnosticPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(xml.pref_diagnostic)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBackgroundColor(view)

        setupDistancePreference()

        setupAverageDistancePreference()
    }

    private fun setBackgroundColor(view: View) {
        if (isTestMode()) {
            view.setBackgroundColor(Color.rgb(255, 165, 0))
        } else {
            view.setBackgroundColor(Color.rgb(255, 240, 220))
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

    companion object {
        private const val MAX_TOLERANCE = 399
    }
}