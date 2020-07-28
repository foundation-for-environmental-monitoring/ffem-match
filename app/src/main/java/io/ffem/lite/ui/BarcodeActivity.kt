@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation.findNavController
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.IS_CALIBRATION
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_INFO_KEY
import io.ffem.lite.app.App.Companion.TEST_VALUE_KEY
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.camera.CameraFragmentDirections
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.TestResult
import io.ffem.lite.databinding.ActivityBarcodeBinding
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.getSampleTestImageNumber
import io.ffem.lite.preference.getSampleTestImageNumberInt
import io.ffem.lite.preference.isTestRunning
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.PreferencesUtil
import java.io.File
import java.io.File.separator
import java.util.*
import kotlin.math.round

const val DEBUG_MODE = "debugMode"
const val TEST_ID = "testId"

/**
 * Activity to display info about the app.
 */
class BarcodeActivity : BaseActivity(), CalibrationItemFragment.OnCalibrationSelectedListener {

    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var b: ActivityBarcodeBinding
    private var testInfo: TestInfo? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isTestRunning() && !BuildConfig.INSTRUMENTED_TEST_RUNNING.get()) {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }
            saveImageData(intent)
        }
    }

    private val resultBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testInfo = intent.getParcelableExtra(TEST_INFO_KEY)

            if (testInfo != null) {
                // Display the result screen
                if (findNavController(
                        this@BarcodeActivity,
                        R.id.fragment_container
                    ).currentDestination?.id == R.id.camera_fragment
                ) {
                    if (AppPreferences.isCalibration()) {
                        findNavController(this@BarcodeActivity, R.id.fragment_container)
                            .navigate(
                                CameraFragmentDirections.actionCameraFragmentToCalibrationItemFragment(
                                    testInfo!!
                                )
                            )
                    } else {
                        findNavController(this@BarcodeActivity, R.id.fragment_container)
                            .navigate(
                                CameraFragmentDirections
                                    .actionCameraFragmentToResultFragment(testInfo!!)
                            )
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = DataBindingUtil.setContentView(this, R.layout.activity_barcode)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(App.CAPTURED_EVENT)
        )

        broadcastManager.registerReceiver(
            resultBroadcastReceiver,
            IntentFilter(App.LOCAL_RESULT_EVENT)
        )

        if (BuildConfig.APPLICATION_ID == intent.action) {
            val uuid = intent.getStringExtra(TEST_ID)
            if (intent.getBooleanExtra(DEBUG_MODE, false)) {
                sendDummyResultForDebugging(uuid)
            }
            PreferencesUtil.setString(this, TEST_ID_KEY, uuid)
            PreferencesUtil.setBoolean(this, IS_CALIBRATION, false)
        } else {
            PreferencesUtil.removeKey(this, TEST_ID_KEY)
        }
    }

    fun submitResult(@Suppress("UNUSED_PARAMETER") view: View) {
        if (testInfo != null) {
            val resultIntent = Intent()
            if (testInfo!!.resultInfo.result >= 0) {
                resultIntent.putExtra(TEST_VALUE_KEY, testInfo!!.resultInfo.result.toString())
                resultIntent.putExtra(
                    testInfo!!.name + "_Result",
                    testInfo!!.resultInfo.result.toString()
                )
                resultIntent.putExtra(testInfo!!.name + "_Risk", testInfo!!.getRiskEnglish(this))
                resultIntent.putExtra("meta_device", Build.BRAND + ", " + Build.MODEL)
            } else {
                resultIntent.putExtra(TEST_VALUE_KEY, "")
            }
            setResult(Activity.RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun saveImageData(data: Intent) {
        val testInfo = data.getParcelableExtra<TestInfo>(TEST_INFO_KEY) ?: return

        if (!AppPreferences.isCalibration()) {
            val db = AppDatabase.getDatabase(baseContext)
            db.resultDao().insert(
                TestResult(
                    testInfo.fileName, testInfo.uuid!!, 0, testInfo.name!!, Date().time,
                    -1.0, -1.0, 0.0, ErrorType.NO_ERROR, getSampleTestImageNumber()
                )
            )
            deleteExcessData(db)
        }

        analyzeImage(testInfo.fileName, testInfo.name!!)
    }

    private fun deleteExcessData(db: AppDatabase) {
        // Keep only last 20 results to save drive space
        for (i in 0..1) {
            if (db.resultDao().getCount() > 20) {
                val result = db.resultDao().getOldestResult()

                val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                        separator + "captures"
                val directory = File("$path$separator${result.id}$separator")
                if (directory.exists() && directory.isDirectory) {
                    directory.deleteRecursively()
                }

                db.resultDao().deleteResult(result.id)
            }
        }
    }

    private fun analyzeImage(fileId: String, name: String) {
        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                separator + "captures" + separator

        val testName = name.replace(" ", "")
        val filePath = "$path${fileId}" + separator + "$testName.jpg"
        val bitmap = BitmapFactory.decodeFile(File(filePath).path)

        if (bitmap != null) {
            ColorUtil.extractImage(this, fileId, bitmap)
        }
    }

    override fun onCalibrationSelected(calibrationValue: CalibrationValue?, testInfo: TestInfo?) {
        testInfo!!.resultInfo.calibrationColor = calibrationValue!!.color
        findNavController(this@BarcodeActivity, R.id.fragment_container)
            .navigate(
                CalibrationItemFragmentDirections.actionCalibrationItemFragmentToCalibrationFragment(
                    testInfo, calibrationValue
                )
            )
    }

    /**
     * Create dummy results to send when in debug mode
     */
    private fun sendDummyResultForDebugging(uuid: String?) {
        if (uuid != null) {
            val testInfo = getTestInfo(uuid)
            if (testInfo != null) {
                val resultIntent = Intent()
                val random = Random()
                val maxValue = testInfo.values[testInfo.values.size / 2].value

                val result = (round(random.nextDouble() * maxValue * 100) / 100.0).toString()
                resultIntent.putExtra(TEST_VALUE_KEY, result)

                val pd = ProgressDialog(this)
                pd.setMessage("Sending dummy result...")
                pd.setCancelable(false)
                pd.show()

                setResult(Activity.RESULT_OK, resultIntent)
                Handler().postDelayed({
                    pd.dismiss()
                    finish()
                }, 3000)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (getSampleTestImageNumberInt() > -1) {
            val toast: Toast = Toast.makeText(this, R.string.dummy_image_message, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, -200)
            (toast.view.findViewById<View>(android.R.id.message) as TextView).setTextColor(Color.WHITE)
            toast.view.background.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
            toast.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(broadcastReceiver)
        broadcastManager.unregisterReceiver(resultBroadcastReceiver)
    }
}
