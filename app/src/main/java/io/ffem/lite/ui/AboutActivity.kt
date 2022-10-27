package io.ffem.lite.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.Constants.PRIVACY_POLICY_LINK
import io.ffem.lite.databinding.ActivityAboutBinding
import io.ffem.lite.helper.ApkHelper.isTestDevice
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.toast

/**
 * Activity to display info about the app.
 */
class AboutActivity : BaseActivity() {
    private lateinit var b: ActivityAboutBinding
    private var clickCount = 0

    private var dialog: NoticesDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAboutBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        setTitle(R.string.about)

        b.versionText.setOnClickListener {
            switchToDiagnosticMode()
        }

        b.noticesLinkText.setOnClickListener {
            // Display legal info
            if (!isTestDevice(this)) {
                dialog = NoticesDialogFragment.newInstance()
                dialog!!.show(supportFragmentManager, "NoticesDialog")
            }
        }

        if (PRIVACY_POLICY_LINK.isNotEmpty()) {
            b.privacyPolicyLink.setOnClickListener {
                if (!isTestDevice(this)) {
                    val intent = Intent(this, WebViewActivity::class.java)
                    intent.putExtra("url", PRIVACY_POLICY_LINK)
                    startActivity(intent)
                }
            }
        } else {
            b.privacyPolicyLink.visibility = GONE
        }

        b.disableDiagnosticsFab.setOnClickListener {
            toast(getString(R.string.diagnosticModeDisabled))
            AppPreferences.disableDiagnosticMode()
            switchLayoutForDiagnosticOrUserMode()
            changeActionBarStyleBasedOnCurrentMode()
            finish()
        }
    }

    /**
     * Turn on diagnostic mode if user clicks on version section CHANGE_MODE_MIN_CLICKS times.
     */
    private fun switchToDiagnosticMode() {
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
            b.layoutDiagnostics.visibility = VISIBLE
        } else {
            if (b.layoutDiagnostics.visibility == VISIBLE) {
                b.layoutDiagnostics.visibility = GONE
            }
        }
        b.versionText.text = App.getAppVersion(false)
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
