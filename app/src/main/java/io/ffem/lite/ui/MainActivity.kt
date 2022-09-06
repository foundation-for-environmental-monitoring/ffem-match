package io.ffem.lite.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import io.ffem.lite.R
import io.ffem.lite.common.Constants
import io.ffem.lite.databinding.ActivityMainBinding
import io.ffem.lite.model.TestType
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.util.AlertUtil

/**
 * Activity to display info about the app.
 */
class MainActivity : BaseActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        b.cardTestButton.setOnClickListener {
            val intent = Intent(this, TestActivity::class.java)
            AppPreferences.setCalibration(this, false)
            AppPreferences.setTestType(this, TestType.CARD)
            startTest.launch(intent)
        }

        b.colorimetricButton.setOnClickListener {
            val externalIntent: Intent? = packageManager
                .getLaunchIntentForPackage(Constants.SURVEY_APP)
            if (externalIntent != null) {
                externalIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(externalIntent)
//                closeApp(1000)
            } else {
                val intent = Intent(this, TestActivity::class.java)
                AppPreferences.setCalibration(this, false)
                AppPreferences.setTestType(this, TestType.CUVETTE)
                startTest.launch(intent)
            }
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
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
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

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val intent = Intent(baseContext, SettingsActivity::class.java)
        startSettings.launch(intent)
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
}
