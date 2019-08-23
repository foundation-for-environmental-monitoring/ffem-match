package io.ffem.lite.preference

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import io.ffem.lite.R
import io.ffem.lite.ui.BaseActivity

class SettingsActivity : BaseActivity() {

    private fun removeAllFragments() {
        findViewById<View>(R.id.layoutTesting).visibility = View.GONE
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

        if (AppPreferences.isDiagnosticMode()) {

            supportFragmentManager.beginTransaction()
                .add(R.id.layoutTesting, TestingPreferenceFragment())
                .commit()

            findViewById<View>(R.id.layoutTesting).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.layoutTesting).visibility = View.GONE
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
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
        if (AppPreferences.isDiagnosticMode()) {
            menuInflater.inflate(R.menu.menu_settings, menu)
        }
        return true
    }

    fun onDisableDiagnostics(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        Toast.makeText(
            baseContext, getString(R.string.diagnosticModeDisabled),
            Toast.LENGTH_SHORT
        ).show()

        AppPreferences.disableDiagnosticMode()

        changeActionBarStyleBasedOnCurrentMode()

        invalidateOptionsMenu()

        removeAllFragments()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
