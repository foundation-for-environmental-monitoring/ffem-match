package io.ffem.lite.preference

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import java.io.File


class DataPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_data)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBackgroundColor(view)

        findPreference<Preference>(getString(R.string.data_delete_key))
            ?.setOnPreferenceClickListener {

                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Delete data")
                builder.setMessage("Delete all photos and tests results?")
                builder.setPositiveButton(R.string.delete) { _, _ ->

                    val folder = File(
                        view.context.getExternalFilesDir(
                            Environment.DIRECTORY_PICTURES
                        ).toString() + File.separator + "captures"
                    )
                    if (folder.exists() && folder.isDirectory) {
                        folder.listFiles()?.forEach {
                            it.delete()
                        }
                    }

                    val db = AppDatabase.getDatabase(view.context)
                    db.resultDao().deleteAll()

                    Toast.makeText(
                        requireContext(),
                        "Data deleted", Toast.LENGTH_SHORT
                    ).show()
                }

                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                }

                builder.show()

                true
            }
    }

    private fun setBackgroundColor(view: View) {
        view.setBackgroundColor(Color.rgb(255, 240, 220))
    }
}
