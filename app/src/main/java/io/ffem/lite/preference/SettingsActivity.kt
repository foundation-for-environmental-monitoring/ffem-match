package io.ffem.lite.preference

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.ui.BaseActivity
import io.ffem.lite.util.PreferencesUtil

class SettingsActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mScrollView: ScrollView? = null
    private var mScrollPosition: Int = 0

    private fun removeAllFragments() {
        findViewById<View>(R.id.layoutTesting).visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActivity()
    }

    public override fun onRestart() {
        super.onRestart()
        setupActivity()
    }

    override fun onResume() {
        super.onResume()

        PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupActivity() {

        setTitle(R.string.settings)

        setContentView(R.layout.activity_settings)

        fragmentManager.beginTransaction()
            .replace(R.id.layoutOther, OtherPreferenceFragment())
            .commit()

        if (AppPreferences.isDiagnosticMode()) {

            fragmentManager.beginTransaction()
                .add(R.id.layoutTesting, TestingPreferenceFragment())
                .commit()

            findViewById<View>(R.id.layoutTesting).visibility = View.VISIBLE
        }

        mScrollView = findViewById(R.id.scrollViewSettings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        try {
            setSupportActionBar(toolbar)
        } catch (ignored: Exception) {
            //Ignore crash in Samsung
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        if (getString(R.string.languageKey).equals(s, false)) {
            App.app.setAppLanguage("", false, null)
            val resultIntent = Intent(intent)
            resultIntent.getBooleanExtra("refresh", true)
            setResult(Activity.RESULT_OK, resultIntent)
            PreferencesUtil.setBoolean(this, R.string.refreshKey, true)
            recreate()
        }
    }

    public override fun onPause() {
        val scrollbarPosition = mScrollView!!.scrollY

        PreferencesUtil.setInt(this, "settingsScrollPosition", scrollbarPosition)

        super.onPause()

        PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPostResume() {
        super.onPostResume()

        mScrollPosition = PreferencesUtil.getInt(this, "settingsScrollPosition", 0)

        mScrollView!!.post { mScrollView!!.scrollTo(0, mScrollPosition) }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
