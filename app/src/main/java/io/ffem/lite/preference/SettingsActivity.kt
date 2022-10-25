package io.ffem.lite.preference

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.Toolbar
import io.ffem.lite.R
import io.ffem.lite.databinding.ActivitySettingsBinding
import io.ffem.lite.ui.BaseActivity
import io.ffem.lite.util.toast

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private fun removeAllFragments() {
        binding.layoutDiagnostics.visibility = GONE
        binding.layoutTesting.visibility = GONE
        binding.layoutData.visibility = GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.settings)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupActivity()

        supportFragmentManager.beginTransaction()
            .replace(R.id.layoutOther, OtherPreferenceFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        setupActivity()
    }

    private fun setupActivity() {
        if (isDiagnosticMode()) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.layoutDiagnostics, DiagnosticPreferenceFragment())
//                .commit()

            supportFragmentManager.beginTransaction()
                .replace(R.id.layoutTesting, TestingPreferenceFragment())
                .commit()

            supportFragmentManager.beginTransaction()
                .replace(R.id.layoutData, DataPreferenceFragment())
                .commit()

            binding.layoutDiagnostics.visibility = VISIBLE
            binding.layoutTesting.visibility = VISIBLE
            binding.layoutData.visibility = VISIBLE
        } else {
            binding.layoutDiagnostics.visibility = GONE
            binding.layoutTesting.visibility = GONE
            binding.layoutData.visibility = GONE
        }

        try {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
        } catch (ignored: Exception) {
            //Ignore crash in some devices
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        setTitle(R.string.settings)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isDiagnosticMode()) {
            menuInflater.inflate(R.menu.menu_settings, menu)
        }
        return true
    }

    fun onDisableDiagnostics(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        toast(getString(R.string.diagnosticModeDisabled))

        AppPreferences.disableDiagnosticMode()

        changeActionBarStyleBasedOnCurrentMode()

        invalidateOptionsMenu()

        removeAllFragments()

        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
