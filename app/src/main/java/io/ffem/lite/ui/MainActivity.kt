package io.ffem.lite.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R
import io.ffem.lite.common.Constants.IS_COMPOST_APP
import io.ffem.lite.common.SAMPLE_TEST_TYPE
import io.ffem.lite.databinding.ActivityMainBinding
import io.ffem.lite.model.TestSampleType
import io.ffem.lite.model.TestType
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.AlertUtil

/**
 * Activity to display info about the app.
 */
class MainActivity : AppUpdateActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {
        super.onResume()
        if (!AppPreferences.isShareDataSet()) {
            showDataCollectionConfirmAlert()
        }

        if (IS_COMPOST_APP) {
            if (isDiagnosticMode()) {
                b.cardTestButton.visibility = VISIBLE
            } else {
                b.cardTestButton.visibility = GONE
            }
        } else {
            b.cardTestButton.visibility = VISIBLE
        }
        b.cardTestButton.setOnClickListener {
            val intent = Intent(this, TestActivity::class.java)
            AppPreferences.setCalibration(this, false)
            AppPreferences.setTestType(this, TestType.CARD)
            startTest.launch(intent)
        }

        if (IS_COMPOST_APP) {
            if (isDiagnosticMode()) {
                b.colorimetricButton.visibility = VISIBLE
            } else {
                b.colorimetricButton.visibility = GONE
            }
        } else {
            b.colorimetricButton.visibility = VISIBLE
        }
        b.colorimetricButton.setOnClickListener {
//            val externalIntent: Intent? = packageManager
//                .getLaunchIntentForPackage(Constants.SURVEY_APP)
//            if (externalIntent != null) {
//                externalIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                startActivity(externalIntent)
////                closeApp(1000)
//            } else {
            val intent = Intent(this, TestActivity::class.java)
            AppPreferences.setCalibration(this, false)
            AppPreferences.setTestType(this, TestType.CUVETTE)
            startTest.launch(intent)
//            }
        }

        if (IS_COMPOST_APP) {
            if (isDiagnosticMode()) {
                b.titrationButton.visibility = VISIBLE
            } else {
                b.titrationButton.visibility = GONE
            }
        } else {
            b.titrationButton.visibility = VISIBLE
        }
        b.titrationButton.setOnClickListener {
            val intent = Intent(this, TestActivity::class.java)
            AppPreferences.setCalibration(this, false)
            AppPreferences.setTestType(this, TestType.TITRATION)
            startTest.launch(intent)
        }

        b.viewResultButton.setOnClickListener {
            val intent = Intent(this, ResultListActivity::class.java)
            startTest.launch(intent)
        }

        if (IS_COMPOST_APP) {
            b.compostButton.visibility = VISIBLE
            b.compostButton.setOnClickListener {
                val intent = Intent(this, TestActivity::class.java)
                AppPreferences.setCalibration(this, false)
                AppPreferences.setTestType(this, TestType.CUVETTE)
                intent.putExtra(SAMPLE_TEST_TYPE, TestSampleType.COMPOST)
                startTest.launch(intent)
            }

            b.soilButton.visibility = VISIBLE
            b.soilButton.setOnClickListener {
                val intent = Intent(this, TestActivity::class.java)
                AppPreferences.setCalibration(this, false)
                AppPreferences.setTestType(this, TestType.CUVETTE)
                intent.putExtra(SAMPLE_TEST_TYPE, TestSampleType.SOIL)
                startTest.launch(intent)
            }
        } else {
            b.compostButton.visibility = GONE
            b.soilButton.visibility = GONE
        }
    }

    private val startTest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

    private fun alertExternalAppNotFound() {
        val message = String.format(
            "%s\r\n\r\n%s",
            getString(R.string.external_app_not_installed),
            getString(R.string.install_external_app)
        )
        AlertUtil.showAlert(
            this, R.string.notFound, message, R.string.close,
            { _: DialogInterface?, _: Int -> closeApp(0) },
            null, null
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(baseContext, SettingsActivity::class.java)
                startSettings.launch(intent)
            }
        }
        return true
    }

    private val startSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private fun closeApp(delay: Int) {
        Handler().postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                val pid = Process.myPid()
                Process.killProcess(pid)
            }
        }, delay.toLong())
    }

    private fun showDataCollectionConfirmAlert() {
        MaterialAlertDialogBuilder(this@MainActivity, R.style.ConfirmDialogTheme)
            .setTitle(getString(R.string.data_sharing))
            .setMessage(
                "Share test data?\n" +
                        "\n" +
                        "You can change your selection at any time in the settings\n"
            )
            .setPositiveButton(
                getString(R.string.yes_share)
            ) { _, _ -> AppPreferences.setShareData(true) }
            .setNeutralButton(
                getString(R.string.no_thanks)
            ) { _, _ -> AppPreferences.setShareData(false) }
            .show()
    }
}
