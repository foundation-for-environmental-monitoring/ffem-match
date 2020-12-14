@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.camera.CameraFragment
import io.ffem.lite.common.*
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.AppPreferences.isCalibration
import io.ffem.lite.preference.isTestRunning
import io.ffem.lite.preference.useColorCardVersion1
import io.ffem.lite.util.BarcodeColorUtil
import io.ffem.lite.util.PreferencesUtil
import kotlinx.android.synthetic.main.activity_barcode.*
import java.io.File
import java.io.File.separator
import java.util.*
import kotlin.math.round

const val DEBUG_MODE = "debugMode"
const val TEST_ID = "testId"

const val INSTRUCTION_PAGE = 0
const val CAMERA_PAGE = 1
const val CONFIRMATION_PAGE = 2
const val CALIBRATE_LIST_PAGE = 3
const val RESULT_PAGE = 4

/**
 * Activity to display info about the app.
 */
class BarcodeActivity : BaseActivity(),
    CalibrationItemFragment.OnCalibrationSelectedListener,
    InstructionFragment.OnStartTestListener,
    ImageConfirmFragment.OnConfirmImageListener {

    private lateinit var broadcastManager: LocalBroadcastManager
    private var testInfo: TestInfo? = null
    lateinit var model: TestInfoViewModel
    lateinit var mediaPlayer: MediaPlayer

    private val capturedPhotoBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isTestRunning() && !BuildConfig.INSTRUMENTED_TEST_RUNNING.get()) {
                mediaPlayer.start()
            }
            saveImageData(intent)
        }
    }

    private val colorCardCapturedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mediaPlayer.start()
            deleteExcessData()
        }
    }

    private val resultBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testInfo = intent.getParcelableExtra(TEST_INFO_KEY)

            if (testInfo != null) {
                model.setTest(testInfo)
                if (testInfo!!.error == ErrorType.BAD_LIGHTING ||
                    testInfo!!.error == ErrorType.IMAGE_TILTED
                ) {
                    view_pager.currentItem = RESULT_PAGE
                } else {
                    view_pager.currentItem = CONFIRMATION_PAGE
                }
            } else {
                view_pager.currentItem = RESULT_PAGE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContentView(R.layout.activity_barcode)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        if (useColorCardVersion1()) {
            broadcastManager.registerReceiver(
                capturedPhotoBroadcastReceiver,
                IntentFilter(CAPTURED_EVENT_BROADCAST)
            )
        } else {
            broadcastManager.registerReceiver(
                colorCardCapturedBroadcastReceiver,
                IntentFilter(CARD_CAPTURED_EVENT_BROADCAST)
            )
        }

        broadcastManager.registerReceiver(
            resultBroadcastReceiver,
            IntentFilter(RESULT_EVENT_BROADCAST)
        )

        AppPreferences.generateImageFileName()

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

        model = ViewModelProvider(this).get(
            TestInfoViewModel::class.java
        )

        view_pager.isUserInputEnabled = false
        val testPagerAdapter = TestPagerAdapter(this)
        view_pager.adapter = testPagerAdapter

        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == CAMERA_PAGE || position == INSTRUCTION_PAGE) {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                } else {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        })

        mediaPlayer = MediaPlayer.create(this, R.raw.short_beep)
    }

    fun submitResult(@Suppress("UNUSED_PARAMETER") view: View) {
        if (testInfo != null) {
            val resultIntent = Intent()
            if (testInfo!!.getResult() >= 0) {
                resultIntent.putExtra(TEST_VALUE_KEY, testInfo!!.getResult().toString())
                resultIntent.putExtra(
                    testInfo!!.name + "_Result",
                    testInfo!!.getResult().toString()
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
        analyzeImage(testInfo)
        deleteExcessData()
    }

    private fun deleteExcessData() {
        val db = AppDatabase.getDatabase(baseContext)
        // Keep only last 25 results to save drive space
        try {
            for (i in 0..1) {
                if (db.resultDao().getCount() > 25) {
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
        } finally {
            db.close()
        }
    }

    private fun analyzeImage(testInfo: TestInfo?) {
        val fileId = testInfo?.fileName!!
        val name = testInfo.name!!

        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                separator + "captures" + separator

        val testName = name.replace(" ", "")
        val filePath = "$path${fileId}" + separator + "$testName.jpg"
        val bitmap = BitmapFactory.decodeFile(File(filePath).path)

        if (bitmap != null) {
            BarcodeColorUtil.extractImage(this, bitmap)
        }
    }

    override fun onCalibrationSelected(calibrationValue: CalibrationValue) {
        model.test.get()!!.calibratedResultInfo.calibratedValue = calibrationValue
        pageNext()
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

    override fun onDestroy() {
        super.onDestroy()
        broadcastManager.unregisterReceiver(colorCardCapturedBroadcastReceiver)
        broadcastManager.unregisterReceiver(capturedPhotoBroadcastReceiver)
        broadcastManager.unregisterReceiver(resultBroadcastReceiver)
    }

    private fun pageBack() {
        if (view_pager.currentItem in CAMERA_PAGE..CONFIRMATION_PAGE) {
            val testPagerAdapter = TestPagerAdapter(this)
            view_pager.adapter = testPagerAdapter
        } else {
            view_pager.currentItem = view_pager.currentItem - 1
        }
    }

    private fun pageNext() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun onBackPressed() {
        if (view_pager.currentItem > INSTRUCTION_PAGE) {
            pageBack()
        } else {
            super.onBackPressed()
        }
    }

    class TestPagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {

        var testInfo: TestInfo? = null

        override fun getItemCount(): Int {
            return if (isCalibration()) {
                5
            } else {
                4
            }
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                INSTRUCTION_PAGE -> {
                    InstructionFragment()
                }
                CAMERA_PAGE -> {
                    CameraFragment()
                }
                CONFIRMATION_PAGE -> {
                    ImageConfirmFragment()
                }
                CALIBRATE_LIST_PAGE -> {
                    if (isCalibration()) {
                        CalibrationItemFragment()
                    } else {
                        ResultFragment()
                    }
                }
                else -> {
                    if (isCalibration()) {
                        CalibrationResultFragment()
                    } else {
                        ResultFragment()
                    }
                }
            }
        }
    }

    override fun onStartTest() {
        view_pager.currentItem = CAMERA_PAGE
    }

    override fun onConfirmImage(action: Int) {
        if (action == RESULT_OK) {
            if (isCalibration()) {
                if (model.test.get()?.error == ErrorType.NO_ERROR) {
                    view_pager.currentItem = CALIBRATE_LIST_PAGE
                } else {
                    view_pager.currentItem = RESULT_PAGE
                }
            } else {
                view_pager.currentItem = CALIBRATE_LIST_PAGE
            }
        } else {
            val testPagerAdapter = TestPagerAdapter(this)
            view_pager.adapter = testPagerAdapter
        }
    }
}
