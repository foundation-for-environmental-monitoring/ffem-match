package io.ffem.lite.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaActionSound
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.R
import io.ffem.lite.camera.CameraFragment
import io.ffem.lite.databinding.ActivityBarcodeBinding
import io.ffem.lite.preference.sendDummyImage
import java.io.File

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
            if (!sendDummyImage()) {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        b = DataBindingUtil.setContentView(this, R.layout.activity_barcode)
        container = findViewById(R.id.fragment_container)

        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(CameraFragment.CAPTURED_EVENT)
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
    }

    companion object {
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}
