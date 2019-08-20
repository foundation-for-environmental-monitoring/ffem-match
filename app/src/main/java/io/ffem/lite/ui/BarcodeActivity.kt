package io.ffem.lite.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.exifinterface.media.ExifInterface
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.API_URL
import io.ffem.lite.app.App.Companion.FILE_PATH_KEY
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.camera.CameraFragment
import io.ffem.lite.databinding.ActivityBarcodeBinding
import io.ffem.lite.model.TestResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
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
            sendToServer(
                intent.getStringExtra(TEST_ID_KEY),
                "Fluoride", intent.getStringExtra(FILE_PATH_KEY)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        b = DataBindingUtil.setContentView(this, R.layout.activity_barcode)
        container = findViewById(R.id.fragment_container)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)

        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(CameraFragment.CAPTURED_EVENT))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    private fun sendToServer(testId: String, barcodeValue: String, filePath: String) {

        try {
            // Add barcode value as exif metadata in the image.
            val imageDescription = "{\"test_type\" : $barcodeValue}"
            val exif = ExifInterface(filePath)
            exif.setAttribute("ImageDescription", imageDescription)
            exif.saveAttributes()
        } catch (e: IOException) {
            // handle the error
        }

        try {
            val file = File(filePath)
            val contentType = file.toURI().toURL().openConnection().contentType

            Timber.d("file: %s", file.path)
            Timber.d("contentType: %s", contentType)

            val fileBody = file.asRequestBody(contentType.toMediaTypeOrNull())
            val filename = file.name

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", "1")
                .addFormDataPart("testId", testId)
                .addFormDataPart("image", filename, fileBody)
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build()

            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.d(e, "Upload Failed!")
                }

                override fun onResponse(call: Call, response: Response) {
                    Timber.d("Upload completed!")

                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                    response.body!!.close()
                    val db = AppDatabase.getDatabase(baseContext)

                    val date = Date()
                    db.resultDao().insert(
                        TestResult(
                            testId, barcodeValue, date.time,
                            "", getString(R.string.analyzing)
                        )
                    )

                    finish()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
