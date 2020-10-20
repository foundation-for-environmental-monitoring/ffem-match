package io.ffem.lite.preference

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import io.ffem.lite.R
import io.ffem.lite.ui.BaseActivity
import io.ffem.lite.util.toast
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.app_bar_layout.*

class SettingsActivity : BaseActivity() {

    private fun removeAllFragments() {
        layoutDiagnostics.visibility = GONE
        layoutTesting.visibility = GONE
        layoutData.visibility = GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.settings)

        setContentView(R.layout.activity_settings)

        setupActivity()
    }

    override fun onResume() {
        super.onResume()
        setupActivity()
    }

    private fun setupActivity() {

        supportFragmentManager.beginTransaction()
            .replace(R.id.layoutOther, OtherPreferenceFragment())
            .commit()

        if (isDiagnosticMode()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.layoutDiagnostics, DiagnosticPreferenceFragment())
                .commit()

            supportFragmentManager.beginTransaction()
                .replace(R.id.layoutTesting, TestingPreferenceFragment())
                .commit()

            supportFragmentManager.beginTransaction()
                .replace(R.id.layoutData, DataPreferenceFragment())
                .commit()

            layoutDiagnostics.visibility = VISIBLE
            layoutTesting.visibility = VISIBLE
            layoutData.visibility = VISIBLE
        } else {
            layoutDiagnostics.visibility = GONE
            layoutTesting.visibility = GONE
            layoutData.visibility = GONE
        }

        try {
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
