package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.databinding.ActivityAboutBinding
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.isDiagnosticMode

/**
 * Activity to display info about the app.
 */
class AboutActivity : BaseActivity() {

    private var clickCount = 0

    private var dialog: NoticesDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val b = DataBindingUtil.setContentView<ActivityAboutBinding>(this, R.layout.activity_about)

        b.textVersion.text = App.getAppVersion()

        setTitle(R.string.about)
    }

    /**
     * Displays legal information.
     */
    fun onSoftwareNoticesClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dialog = NoticesDialogFragment.newInstance()
        dialog!!.show(supportFragmentManager, "NoticesDialog")
    }

    /**
     * Disables diagnostic mode.
     */
    fun disableDiagnosticsMode(@Suppress("UNUSED_PARAMETER") view: View) {
        Toast.makeText(
            baseContext, getString(R.string.diagnosticModeDisabled),
            Toast.LENGTH_SHORT
        ).show()

        AppPreferences.disableDiagnosticMode()

        switchLayoutForDiagnosticOrUserMode()

        changeActionBarStyleBasedOnCurrentMode()
    }

    /**
     * Turn on diagnostic mode if user clicks on version section CHANGE_MODE_MIN_CLICKS times.
     */
    fun switchToDiagnosticMode(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!isDiagnosticMode()) {
            clickCount++

            if (clickCount >= CHANGE_MODE_MIN_CLICKS) {
                clickCount = 0
                Toast.makeText(
                    baseContext, getString(
                        R.string.diagnosticModeEnabled
                    ), Toast.LENGTH_SHORT
                ).show()
                AppPreferences.enableDiagnosticMode()

                changeActionBarStyleBasedOnCurrentMode()

                switchLayoutForDiagnosticOrUserMode()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        switchLayoutForDiagnosticOrUserMode()
    }

    /**
     * Show the diagnostic mode layout.
     */
    private fun switchLayoutForDiagnosticOrUserMode() {
        if (isDiagnosticMode()) {
            findViewById<View>(R.id.layoutDiagnostics).visibility = View.VISIBLE
        } else {
            if (findViewById<View>(R.id.layoutDiagnostics).visibility == View.VISIBLE) {
                findViewById<View>(R.id.layoutDiagnostics).visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val CHANGE_MODE_MIN_CLICKS = 10
    }
}
