package io.ffem.lite.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaActionSound
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.View
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.databinding.ActivityBarcodeBinding
import io.ffem.lite.model.TestResult
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.PreferencesUtil
import java.io.File
import java.util.*

/** Combination of all flags required to put activity into immersive mode */
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

/**
 * Activity to display info about the app.
 */
class BarcodeActivity : BaseActivity() {

    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var b: ActivityBarcodeBinding
    private lateinit var container: FrameLayout

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!BuildConfig.TEST_RUNNING.get()) {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }

            saveImageData(intent)

            setResult(Activity.RESULT_OK, intent)

            Handler().postDelayed({
                finish()
            }, 2000)
        }
    }

    private val resultBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setResult(Activity.RESULT_OK, intent)
            Handler().postDelayed({
                finish()
            }, 2000)
        }
    }

    private fun saveImageData(data: Intent) {
        val id = data.getStringExtra(App.TEST_ID_KEY)

        val testName = data.getStringExtra(App.TEST_NAME_KEY)

        if (testName.isNullOrEmpty()) {
            return
        }

        if (id != null) {

            val testImageNumber = PreferencesUtil
                .getString(this, R.string.testImageNumberKey, "")

            val db = AppDatabase.getDatabase(baseContext)
            db.resultDao().insert(
                TestResult(
                    id, 0, testName, Date().time,
                    Date().time, "", testImageNumber, getString(R.string.outbox)
                )
            )
            analyzeImage()
        }
    }

    private fun analyzeImage() {
        val db = AppDatabase.getDatabase(baseContext)
        db.resultDao().getPendingResults().forEach {
            val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

            val fileName = it.name.replace(" ", "")
            val filePath = "$path${it.id}" + "_" + "$fileName.jpg"

            val file = File(filePath)

            val bitmap = BitmapFactory.decodeFile(file.path)

            if (bitmap != null) {
                ColorUtil.extractImage(this, it.id, bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        b = DataBindingUtil.setContentView(this, R.layout.activity_barcode)
        container = findViewById(R.id.fragment_container)

        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(App.CAPTURED_EVENT)
        )

        broadcastManager.registerReceiver(
            resultBroadcastReceiver,
            IntentFilter(App.LOCAL_RESULT_EVENT)
        )
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(broadcastReceiver)
        broadcastManager.unregisterReceiver(resultBroadcastReceiver)
    }

    companion object {
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L

//        /** Use external media if it is available, our app's file directory otherwise */
//        fun getOutputDirectory(context: Context): File {
//            val appContext = context.applicationContext
//            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
//                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
//            }
//            return if (mediaDir != null && mediaDir.exists())
//                mediaDir else appContext.filesDir
//        }
    }
}
