package io.ffem.lite.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.ffem.lite.R
import io.ffem.lite.barcode.BarcodeCaptureActivity
import io.ffem.lite.preference.SettingsActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val EXTERNAL_APP_PACKAGE_NAME = "io.ffem.collect"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val intent = Intent(baseContext, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun onOkClicked(@Suppress("UNUSED_PARAMETER") view: View) {

        var intent: Intent? = Intent(baseContext, BarcodeCaptureActivity::class.java)
        startActivity(intent)

        intent = packageManager
            .getLaunchIntentForPackage(EXTERNAL_APP_PACKAGE_NAME)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            closeApp(1000)
        } else {
            alertDependantAppNotFound()
        }
    }

    private fun closeApp(delay: Int) {
        Handler().postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                val pid = android.os.Process.myPid()
                android.os.Process.killProcess(pid)
            }
        }, delay.toLong())
    }

    private fun alertDependantAppNotFound() {
        val builder = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog)
        builder.setTitle(R.string.app_not_found)
            .setMessage(R.string.install_app)
            .setPositiveButton(R.string.go_to_play_store) { _, _ ->
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/developer?id=Foundation+for+Environmental+Monitoring")
                    )
                )
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { dialogInterface, _ -> dialogInterface.dismiss() }
            .setCancelable(false)
            .show()
    }
}
