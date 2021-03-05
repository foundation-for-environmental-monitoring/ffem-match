@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.FirebaseDatabase
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.camera.CameraFragment
import io.ffem.lite.common.*
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.Result
import io.ffem.lite.data.TestResult
import io.ffem.lite.databinding.ActivityTestBinding
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.PageIndex
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.AppPreferences.isCalibration
import io.ffem.lite.util.PreferencesUtil
import java.io.File
import java.io.File.separator
import java.util.*
import kotlin.math.round

/**
 * Activity to display info about the app.
 */
class TestActivity : BaseActivity(),
    CalibrationItemFragment.OnCalibrationSelectedListener,
    InstructionFragment.OnStartTestListener,
    ImageConfirmFragment.OnConfirmImageListener {

    private lateinit var b: ActivityTestBinding
    private lateinit var broadcastManager: LocalBroadcastManager
    private var testInfo: TestInfo? = null
    lateinit var model: TestInfoViewModel
    lateinit var mediaPlayer: MediaPlayer
    private val pageIndex = PageIndex()
    private var isExternalRequest: Boolean = false

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
                val db = AppDatabase.getDatabase(context)
                if (!isCalibration()) {
                    val testResult = db.resultDao().getResult(testInfo!!.fileName)
                    if (testResult != null) {
                        model.form = testResult
                    }
                }
                model.db = db
                if (testInfo!!.error == ErrorType.BAD_LIGHTING ||
                    testInfo!!.error == ErrorType.IMAGE_TILTED
                ) {
                    b.viewPager.currentItem = pageIndex.result
                } else {
                    b.viewPager.currentItem = pageIndex.confirmation
                }
            } else {
                b.viewPager.currentItem = pageIndex.result
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        b = ActivityTestBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        broadcastManager.registerReceiver(
            colorCardCapturedBroadcastReceiver,
            IntentFilter(CARD_CAPTURED_EVENT_BROADCAST)
        )

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
            isExternalRequest = true
            PreferencesUtil.setString(this, TEST_ID_KEY, uuid)
            PreferencesUtil.setBoolean(this, IS_CALIBRATION, false)
        } else {
            PreferencesUtil.removeKey(this, TEST_ID_KEY)
        }

        model = ViewModelProvider(this).get(
            TestInfoViewModel::class.java
        )

        b.viewPager.isUserInputEnabled = false
        val testPagerAdapter = TestPagerAdapter(this)
        testPagerAdapter.pageIndex = pageIndex
        b.viewPager.adapter = testPagerAdapter

        b.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == pageIndex.camera || position == pageIndex.instruction) {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                    view.invalidate()
                } else {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    view.invalidate()
                }
            }
        })

        mediaPlayer = MediaPlayer.create(this, R.raw.short_beep)
    }

    fun submitResult() {
        if (testInfo != null) {
            val resultIntent = Intent()
            if (testInfo!!.getResult() >= 0) {
                val db = AppDatabase.getDatabase(baseContext)
                val form = db.resultDao().getResult(testInfo!!.fileName)!!
                sendResultToCloudDatabase(testInfo!!, form)
                resultIntent.putExtra(TEST_VALUE_KEY, testInfo!!.getResultString(this))
                resultIntent.putExtra(testInfo!!.name + "_Result", testInfo!!.getResultString(this))
                resultIntent.putExtra(testInfo!!.name + "_Risk", testInfo!!.getRiskEnglish(this))
                resultIntent.putExtra("meta_device", Build.BRAND + ", " + Build.MODEL)
            } else {
                resultIntent.putExtra(TEST_VALUE_KEY, "")
            }
            setResult(Activity.RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun sendResultToCloudDatabase(testInfo: TestInfo, form: TestResult) {
        if (!BuildConfig.INSTRUMENTED_TEST_RUNNING.get() && !isExternalRequest) {
            val path = if (BuildConfig.DEBUG) {
                "result-debug"
            } else {
                "result"
            }
            val ref = FirebaseDatabase.getInstance().getReference(path).push()
            ref.setValue(
                Result(
                    testInfo.uuid!!,
                    testInfo.name,
                    testInfo.getRiskEnglish(this),
                    testInfo.getResultString(this),
                    testInfo.unit,
                    System.currentTimeMillis(),
                    form.source,
                    form.sourceType,
                    form.latitude,
                    form.longitude,
                    form.geoAccuracy,
                    form.comment,
                    App.getAppVersion(true),
                    Build.MODEL
                )
            )
        }
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
        broadcastManager.unregisterReceiver(resultBroadcastReceiver)
    }

    private fun pageBack() {
        if (b.viewPager.currentItem in pageIndex.camera..pageIndex.confirmation) {
            val testPagerAdapter = TestPagerAdapter(this)
            testPagerAdapter.pageIndex = pageIndex
            b.viewPager.adapter = testPagerAdapter
        } else {
            b.viewPager.currentItem = b.viewPager.currentItem - 1
        }
    }

    fun pageNext() {
        b.viewPager.currentItem = b.viewPager.currentItem + 1
    }

    override fun onBackPressed() {
        if (b.viewPager.currentItem > pageIndex.instruction) {
            pageBack()
        } else {
            finish()
        }
    }

    class TestPagerAdapter(
        activity: TestActivity
    ) : FragmentStateAdapter(activity) {

        var testInfo: TestInfo? = null
        private val testActivity = activity
        lateinit var pageIndex: PageIndex

        override fun getItemCount(): Int {
            return if (isCalibration()) {
                5
            } else {
                if (testActivity.testInfo == null) {
                    4
                } else {
                    if (testActivity.testInfo?.error == ErrorType.NO_ERROR) {
                        if (testActivity.isExternalRequest) {
                            4
                        } else {
                            5
                        }
                    } else {
                        4
                    }
                }
            }
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                pageIndex.instruction -> {
                    InstructionFragment()
                }
                pageIndex.camera -> {
                    CameraFragment()
                }
                pageIndex.confirmation -> {
                    ImageConfirmFragment()
                }
                pageIndex.result -> {
                    if (isCalibration()) {
                        CalibrationItemFragment()
                    } else {
                        ResultFragment(testActivity.isExternalRequest)
                    }
                }
                pageIndex.calibration -> {
                    if (isCalibration()) {
                        CalibrationResultFragment()
                    } else {
                        FormSubmitFragment()
                    }
                }
                pageIndex.submit -> {
                    FormSubmitFragment()
                }
                else -> {
                    if (isCalibration()) {
                        CalibrationResultFragment()
                    } else {
                        ResultFragment(testActivity.isExternalRequest)
                    }
                }
            }
        }
    }

    override fun onStartTest() {
        b.viewPager.currentItem = pageIndex.camera
    }

    override fun onConfirmImage(action: Int) {
        if (action == RESULT_OK) {
            b.viewPager.currentItem = pageIndex.result
        } else {
            val testPagerAdapter = TestPagerAdapter(this)
            testPagerAdapter.pageIndex = pageIndex
            b.viewPager.adapter = testPagerAdapter
        }
    }
}
