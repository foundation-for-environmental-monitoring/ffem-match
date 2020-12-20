package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.databinding.ActivityAboutBinding
import io.ffem.lite.helper.ApkHelper.isTestDevice
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.toast

/**
 * Activity to display info about the app.
 */
class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding
    private var clickCount = 0

    private var dialog: NoticesDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.textVersion.text = App.getAppVersion()

        setTitle(R.string.about)
    }

    /**
     * Displays legal information.
     */
    fun onSoftwareNoticesClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!isTestDevice(this)) {
            dialog = NoticesDialogFragment.newInstance()
            dialog!!.show(supportFragmentManager, "NoticesDialog")
        }
    }

    /**
     * Disables diagnostic mode.
     */
    fun disableDiagnosticsMode(@Suppress("UNUSED_PARAMETER") view: View) {
        toast(getString(R.string.diagnosticModeDisabled))

        AppPreferences.disableDiagnosticMode()

        switchLayoutForDiagnosticOrUserMode()

        changeActionBarStyleBasedOnCurrentMode()

        finish()
    }

    /**
     * Turn on diagnostic mode if user clicks on version section CHANGE_MODE_MIN_CLICKS times.
     */
    fun switchToDiagnosticMode(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!isDiagnosticMode()) {
            clickCount++

            if (clickCount >= CHANGE_MODE_MIN_CLICKS) {
                clickCount = 0
                toast(getString(R.string.diagnosticModeEnabled))

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
            binding.layoutDiagnostics.visibility = View.VISIBLE
        } else {
            if (binding.layoutDiagnostics.visibility == View.VISIBLE) {
                binding.layoutDiagnostics.visibility = View.GONE
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
