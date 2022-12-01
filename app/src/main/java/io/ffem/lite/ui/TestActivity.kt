@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.Settings
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.*
import io.ffem.lite.common.Constants.EXTERNAL_ACTION
import io.ffem.lite.common.Constants.MESSAGE_TWO_LINE_FORMAT
import io.ffem.lite.data.*
import io.ffem.lite.data.DataHelper.getJsonResult
import io.ffem.lite.data.DataHelper.getTestInfo
import io.ffem.lite.databinding.ActivityTestBinding
import io.ffem.lite.helper.InstructionHelper.setupInstructions
import io.ffem.lite.helper.SwatchHelper.isSwatchListValid
import io.ffem.lite.model.*
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.AppPreferences.returnDummyResults
import io.ffem.lite.preference.AppPreferences.setCalibration
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.File.separator
import java.util.*
import kotlin.math.max

fun openAppPermissionSettings(context: Context) {
    val i = Intent()
    i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    i.addCategory(Intent.CATEGORY_DEFAULT)
    i.data = Uri.parse("package:" + context.packageName)
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    startActivity(context, i, null)
}

class TestActivity : BaseActivity(), TitrationFragment.OnSubmitResultListener,
    BaseRunTest.OnResultListener,
    BaseRunTestFragment.OnResultListener,
    SensorFragmentBase.OnSubmitResultListener,
    RecommendationFragment.OnSubmitResultListener,
    SelectDilutionFragment.OnDilutionSelectedListener,
    CalibrateOptionFragment.OnCalibrationOptionListener,
    EditCustomDilution.OnCustomDilutionListener,
    CardInstructionFragment.OnStartTestListener,
    CalibrationFragment.OnCalibrationSelected,
    ImageConfirmFragment.OnConfirmImageListener,
    CalibrationDetailsDialog.OnCalibrationDetailsSavedListener,
    CalibrationExpiryDialog.OnCalibrationExpirySavedListener {
    internal var isExternalSurvey: Boolean = false
    private lateinit var b: ActivityTestBinding
    private val mainScope = MainScope()
    private lateinit var timerScope: CoroutineScope
    private lateinit var testViewModel: TestInfoViewModel
    private lateinit var testInfo: TestInfo
    private val pageIndex = PageIndex()
    private var currentDilution = 1
    private val instructionList = ArrayList<Instruction>()
    var isCalibration: Boolean = false
    private var redoTest: Boolean = false
    private var alertDialogToBeDestroyed: AlertDialog? = null
    lateinit var mediaPlayer: MediaPlayer
    private lateinit var broadcastManager: LocalBroadcastManager
    private val testCompletedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isCalibration && testViewModel.calibrationColor == Color.TRANSPARENT) {
                pageBack()
                showError(
                    String.format(
                        MESSAGE_TWO_LINE_FORMAT, getString(R.string.could_not_calibrate),
                        getString(R.string.check_chamber_placement)
                    ),
                    null
                )
            } else if (!isCalibration && testViewModel.test.get()!!
                    .subTest().resultInfo.result < 0
            ) {
                pageBack()
                showError(
                    String.format(
                        MESSAGE_TWO_LINE_FORMAT, getString(R.string.error_test_failed),
                        getString(R.string.check_chamber_placement)
                    ),
                    null
                )
            } else {
                SoundUtil.playShortResource(context, R.raw.success)

                val db = AppDatabase.getDatabase(baseContext)
                val subTest = testInfo.subTest()
                db.resultDao().insert(
                    TestResult(
                        testInfo.fileName,
                        testInfo.uuid,
                        0,
                        testInfo.name!!,
                        testInfo.sampleType.toString(),
                        Date().time,
                        subTest.getResult(),
                        subTest.maxValue,
                        subTest.getMarginOfError(),
                        error = ErrorType.NO_ERROR,
                        unit = subTest.unit.toString()
                    )
                )

                testViewModel.setTest(testInfo)
                if (!isCalibration) {
                    val testResult = db.resultDao().getResult(testInfo.fileName)
                    if (testResult != null) {
                        testViewModel.form = testResult
                    }
                }
                testViewModel.db = db
                pageNext()
            }
        }
    }

    private val colorCardCapturedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mediaPlayer.start()
//            deleteExcessData()
        }
    }

    private val resultBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            testInfo = intent.getParcelableExtra(TEST_INFO_KEY)

            if (testInfo != null) {
                testViewModel.setTest(testInfo)
                val db = AppDatabase.getDatabase(context)
                if (!isCalibration) {
                    val testResult = db.resultDao().getResult(testInfo.fileName)
                    if (testResult != null) {
                        testViewModel.form = testResult
                    }
                }
                testViewModel.db = db
                val subTest = testInfo.subTest()
                if (subTest.error == ErrorType.BAD_LIGHTING ||
                    subTest.error == ErrorType.IMAGE_TILTED
                ) {
                    b.viewPager.currentItem = pageIndex.resultPage
                } else {
                    pageNext()
                }
            } else {
                b.viewPager.currentItem = pageIndex.resultPage
            }
        }
    }

    private fun registerBroadcastReceiver() {
        if (::broadcastManager.isInitialized) {
            broadcastManager.registerReceiver(
                testCompletedBroadcastReceiver,
                IntentFilter(BROADCAST_TEST_COMPLETED)
            )
        }
    }

//    fun isCalibration(): Boolean {
//        return PreferencesUtil.getBoolean(this, IS_CALIBRATION, false)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        val themeUtils = ThemeUtils(this)
        setTheme(themeUtils.appTheme)
        super.onCreate(savedInstanceState)
        title = ""
        hideSystemUI()

        b = ActivityTestBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        mediaPlayer = MediaPlayer.create(this, R.raw.short_beep)

        isCalibration = intent.getBooleanExtra(IS_CALIBRATION, false)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        broadcastManager.registerReceiver(
            colorCardCapturedBroadcastReceiver,
            IntentFilter(CARD_CAPTURED_EVENT_BROADCAST)
        )

        broadcastManager.registerReceiver(
            resultBroadcastReceiver,
            IntentFilter(RESULT_EVENT_BROADCAST)
        )

        b.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position > 0 && b.indicatorPgr.activePage == position) {
                    return
                }

                b.indicatorPgr.activePage = max(0, position)
                b.indicatorPgr.invalidate()

                if (position < 1) {
                    b.footerLyt.visibility = View.GONE
                } else {
                    b.footerLyt.visibility = View.VISIBLE
                }

                if (position < 1) {
                    b.backTxt.visibility = View.INVISIBLE
                } else {
                    b.backTxt.visibility = View.VISIBLE
                }

                invalidateOptionsMenu()

                showHideFooter(position)

                if (position == pageIndex.testPage &&
                    (testInfo.subtype == TestType.CUVETTE || testInfo.subtype == TestType.CARD)
                ) {
                    if (!AppPreferences.useExternalSensor(applicationContext)) {
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                    hideSystemUI()
                } else {
//                    showSystemUI()
                    if (::timerScope.isInitialized) {
                        timerScope.cancel()
                    }
                }

                lifecycleScope.launch {
                    delay(100)
                    if (b.viewPager.currentItem == 0) {
                        title = if (isCalibration) {
                            getString(R.string.calibrate)
                        } else {
                            getString(R.string.select_test)
                        }
                    } else if (::testInfo.isInitialized) {
                        title = testInfo.name?.toLocalString()
                    } else if (b.viewPager.currentItem == pageIndex.confirmationPage) {
                        title = getString(R.string.confirm)
                    }
                }
            }
        })

        testViewModel = ViewModelProvider(this)[TestInfoViewModel::class.java]

        lifecycle.coroutineScope.launchWhenCreated {
            delay(300)
            if (EXTERNAL_ACTION == intent.action) {
                runBlocking {
                    launch {
                        getTestSelectedByExternalApp(intent)
                    }
                }
            }

            val testInfo = testViewModel.test.get()
            if (testInfo != null) {
                testViewModel.loadCalibrations()
                if (!isDiagnosticMode() && testInfo.subtype == TestType.CUVETTE &&
                    !AppPreferences.useExternalSensor(this@TestActivity) && !isSwatchListValid(
                        testInfo,
                        false
                    )
                ) {
                    alertCalibrationIncomplete(
                        this@TestActivity, testInfo
                    )
                    return@launchWhenCreated
                }

                val db = CalibrationDatabase.getDatabase(this@TestActivity)
                try {
                    val calibrationDetail = db.calibrationDao().getCalibrationDetail(testInfo.uuid)
                    if (calibrationDetail != null) {
                        val milliseconds = calibrationDetail.expiry
                        if (milliseconds > 0 && milliseconds <= Date().time) {
                            if (!isDiagnosticMode()) {
                                alertCalibrationExpired(this@TestActivity)
                                return@launchWhenCreated
                            }
                        }
                    }
                } finally {
                    db.close()
                }

                startTest()
            } else {
                val testPagerAdapter =
                    TestPagerAdapter(this@TestActivity, testInfo, isExternalSurvey)
                testPagerAdapter.pageIndex = pageIndex
                b.viewPager.adapter = testPagerAdapter
                b.indicatorPgr.showDots = true
            }
        }
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
//        findViewById<AppBarLayout>(R.id.appBarLayout).visibility = View.VISIBLE
    }

    private fun hideSystemUI() {
//        findViewById<AppBarLayout>(R.id.appBarLayout).visibility = View.GONE
//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun alertCalibrationExpired(activity: Activity) {
        val message = String.format(
            MESSAGE_TWO_LINE_FORMAT,
            activity.getString(R.string.error_calibration_expired),
            activity.getString(R.string.order_fresh_batch)
        )
        AlertUtil.showAlert(
            activity, R.string.cannot_start_test,
            message, R.string.ok,
            { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                activity.finish()
            }, null,
            { dialogInterface: DialogInterface ->
                dialogInterface.dismiss()
                activity.finish()
            }
        )
    }

    private fun alertCalibrationIncomplete(
        activity: Activity, testInfo: TestInfo
    ) {
        var message = activity.getString(
            R.string.error_calibration_incomplete,
            testInfo.name!!.toLocalString()
        )
        message = String.format(
            MESSAGE_TWO_LINE_FORMAT, message,
            activity.getString(R.string.do_you_want_to_calibrate)
        )

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.invalid_calibration)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                activity.setResult(Activity.RESULT_CANCELED)
                activity.finish()
//                calibrate()
            }
            .create()
            .show()
    }

    private var startCalibrate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
//
//    private fun calibrate(): PreferenceFragmentCompat? {
//        val intent = Intent(this, TestActivity::class.java)
//        intent.putExtra(IS_CALIBRATION, true)
//        intent.putExtra(TEST_ID_KEY, testInfo.uuid)
//        setCalibration(this, true)
//        startCalibrate.launch(intent)
//        return null
//    }

    private fun runCuvetteTest() {
        timerScope = MainScope()
        timerScope.launch {
            if (AppPreferences.useFaceDownMode(baseContext) &&
                BuildConfig.USE_SCREEN_PINNING.get() && !isAppInLockTaskMode(baseContext)
            ) {
                if (b.viewPager.currentItem == pageIndex.testPage &&
                    !returnDummyResults(baseContext)
                ) {
                    startLockTask()
                }
            }
        }
    }

    private fun startTest() {
        val testPagerAdapter = TestPagerAdapter(this, testInfo, isExternalSurvey)
        redoTest = true
        setupInstructions(
            testInfo,
            isExternalSurvey,
            instructionList,
            pageIndex,
            currentDilution,
            isCalibration,
            redoTest,
            this
        )

        testPagerAdapter.testInfo = testInfo
        testPagerAdapter.instructions = instructionList

        b.indicatorPgr.pageCount = pageIndex.totalPageCount

        b.viewPager.currentItem = 0

        testViewModel.loadCalibrations()
        setCalibration(this, isCalibration)
        testViewModel.isCalibration = isCalibration

        showHideFooter(b.viewPager.currentItem)

        testPagerAdapter.pageIndex = pageIndex
        b.viewPager.adapter = testPagerAdapter
        b.indicatorPgr.showDots = true
    }

    private fun getTestSelectedByExternalApp(intent: Intent) {
        var uuid = intent.getStringExtra(TEST_ID_KEY)
        if (uuid.isNullOrEmpty()) {
            uuid = intent.getStringExtra(EXT_TEST_ID_KEY)
        }
        if (uuid != null) {
            val test = getTestInfo(uuid, this)
            if (test != null) {
                testInfo = test
                val formulaList = mutableListOf<String>()
                if (intent.getStringExtra("f1") != null) {
                    formulaList.add(intent.getStringExtra("f1").toString())
                }
                if (intent.getStringExtra("f2") != null) {
                    formulaList.add(intent.getStringExtra("f2").toString())
                }
                var index = 0
                for (r in testInfo.results) {
                    if (formulaList.size > index) {
                        r.formula = formulaList[index]
                        index++
                    }
                }

                testViewModel.setTest(testInfo)
                isExternalSurvey = true
            } else {
                setTitle(R.string.not_found)
                alertTestTypeNotSupported()
            }
        }
    }

    private fun alertTestTypeNotSupported() {
        var message = getString(R.string.error_test_not_available)
        message = String.format(
            MESSAGE_TWO_LINE_FORMAT,
            message,
            getString(R.string.please_contact_support)
        )
        AlertUtil.showAlert(
            this, R.string.cannot_start_test, message,
            R.string.ok,
            { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                finish()
            }, null,
            { dialogInterface: DialogInterface ->
                dialogInterface.dismiss()
                finish()
            }
        )
    }

    private fun showHideFooter(position: Int) {
        b.indicatorPgr.visibility = View.GONE
        b.indicatorPgr.invalidate()
        b.indicatorPgr.visibility = View.VISIBLE
        b.nextTxt.visibility = View.VISIBLE
        b.backTxt.visibility = View.VISIBLE
        when {
            position == pageIndex.dilutionPage || position == pageIndex.calibrationPage -> {
                b.viewPager.isUserInputEnabled = false
                if (position > 1) {
                    b.nextTxt.visibility = View.VISIBLE
                } else {
                    b.nextTxt.visibility = View.INVISIBLE
                }
            }
            isCalibration && position == 0 -> {
                b.nextTxt.visibility = View.INVISIBLE
            }
            position == pageIndex.submitPage || position == pageIndex.startTimerPage
                    || position == pageIndex.testPage -> {
                b.viewPager.isUserInputEnabled = false
                b.nextTxt.visibility = View.INVISIBLE
            }
            else -> {
                b.viewPager.isUserInputEnabled = true
                b.nextTxt.visibility = View.VISIBLE
            }
        }
        if ((b.nextTxt.visibility == View.INVISIBLE && b.backTxt.visibility == View.INVISIBLE) ||
            position == pageIndex.testPage || position == pageIndex.totalPageCount - 1 ||
            pageIndex.listPage == position
        ) {
            b.footerLyt.visibility = View.GONE
        } else {
            b.footerLyt.visibility = View.VISIBLE
        }
        if (isCalibration && b.viewPager.currentItem == 1) {
            b.footerLyt.visibility = View.GONE
        }
        if (isCalibration && b.viewPager.currentItem == pageIndex.resultPage) {
            b.footerLyt.visibility = View.GONE
        }
        if (this::testInfo.isInitialized) {
            if (position == pageIndex.resultPage && !isCalibration) {
                b.footerLyt.visibility = View.VISIBLE
            } else {
                b.footerLyt.visibility = View.GONE
            }
        }
        if (position == pageIndex.resultPage) {
            b.viewPager.isUserInputEnabled = false
        }
    }

    private fun setTestResult() {
        val resultIntent = Intent()
        val resultsValues = SparseArray<String>()
        for (i in testInfo.results.indices) {
            val result = testInfo.results[i]
            resultIntent.putExtra(
                result.name?.replace(" ", "_"), result.getResultString()
            )
            resultIntent.putExtra(
                result.name?.replace(" ", "_") + "_" + UNIT,
                testInfo.subTest().unit
            )
            resultsValues.append(result.id, result.getResultString())
            if (testInfo.results.size == 1) {
                if (result.error == ErrorType.NO_ERROR) {
                    resultIntent.putExtra(VALUE, result.getResultString())
                }
            }
        }
        val resultJson = getJsonResult(testInfo, testInfo.results, -1, null, this)
        resultIntent.putExtra(ConstantJsonKey.RESULT_JSON, resultJson.toString())
        setResult(Activity.RESULT_OK, resultIntent)
    }

    override fun onSubmitResult(results: ArrayList<Result>) {
        setTestResult()

        val db = AppDatabase.getDatabase(this)
        val subTest = testInfo.subTest()
        var name2 = ""
        var value2 = -2.0
        if (testInfo.results.size > 1) {
            name2 = testInfo.results[1].name.toString()
            value2 = testInfo.results[1].resultInfo.result
        }
        db.resultDao().insert(
            TestResult(
                testInfo.fileName,
                testInfo.uuid,
                0,
                subTest.name!!,
                testInfo.sampleType.toString(),
                Date().time,
                subTest.getResult(),
                subTest.maxValue,
                subTest.getMarginOfError(),
                error = ErrorType.NO_ERROR,
                name2 = name2,
                value2 = value2,
                unit = subTest.unit.toString()
            )
        )

        testViewModel.setTest(testInfo)
        if (!isCalibration) {
            val testResult = db.resultDao().getResult(testInfo.fileName)
            if (testResult != null) {
                testViewModel.form = testResult
            }
        }
        testViewModel.db = db
        pageNext()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDone -> {
                val intent = Intent(baseContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return true
            }
            R.id.menuLoad -> {
                loadCalibration()
                return true
            }
            R.id.save_menu -> {
                val db = CalibrationDatabase.getDatabase(this)
                try {
                    val dao = db.calibrationDao()
                    val calibrationInfo = dao.getCalibrations(testInfo.uuid)
                    if (calibrationInfo != null) {
                        var colors = ""
                        for (calibration in calibrationInfo.calibrations) {
                            if (colors.isNotEmpty()) {
                                colors += ","
                            }
                            colors += calibration.value.toString() + ":" + calibration.color
                        }
                        val savedName = dao.getCalibrationColorString(colors)
                        if (savedName != null) {
                            SnackbarUtils.showLongSnackbar(
                                b.footerLyt,
                                String.format(
                                    getString(R.string.calibration_already_saved),
                                    savedName
                                )
                            )
                            return true
                        }
                    }

                    if (calibrationInfo != null &&
                        calibrationInfo.calibrations.size >= testInfo.subTest().colors.size
                    ) {
                        showCalibrationDetailsDialog(false)
                    } else {
                        SnackbarUtils.showLongSnackbar(
                            b.footerLyt,
                            String.format(
                                getString(R.string.error_calibration_incomplete),
                                testInfo.name!!.toLocalString()
                            )
                        )
                    }
                } finally {
                    db.close()
                }
                return true
            }
            R.id.graph_menu -> {
                val graphIntent = Intent(this, CalibrationGraphActivity::class.java)
                testViewModel.loadCalibrations()
                graphIntent.putExtra(TEST_INFO, testViewModel.test.get())
                startActivity(graphIntent)
                return true
            }
            android.R.id.home -> {
                if (isAppInLockTaskMode(this)) {
                    showLockTaskEscapeMessage()
                } else {
                    when {
                        isCalibration && b.viewPager.currentItem > 1 -> {
                            b.viewPager.setCurrentItem(1, false)
                        }
                        isCalibration && b.viewPager.currentItem > 0 -> {
                            b.viewPager.setCurrentItem(0, false)
                        }
                        else -> {
                            finish()
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val startLoadCalibration =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_NO_DATA) {
                findViewById<ConstraintLayout>(R.id.root_layout)
                    .snackBar(getString(R.string.no_calibrations_saved))
            }
        }

    private fun loadCalibration() {
//        testViewModel.loadCalibrations()
        val intent = Intent(this, CalibrationsActivity::class.java)
        intent.putExtra(TEST_INFO, testViewModel.test.get())
        startLoadCalibration.launch(intent)
    }

    private fun pageBack() {
        if (b.viewPager.currentItem == 1 && isCalibration && intent.getStringExtra(TEST_ID_KEY) != null) {
            finish()
        } else if (b.viewPager.currentItem - 1 == pageIndex.testPage) {
            b.viewPager.currentItem = b.viewPager.currentItem - 2
        } else {
            b.viewPager.currentItem = b.viewPager.currentItem - 1
        }
    }

    fun onPageBackClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (b.viewPager.currentItem > 0) {
            pageBack()
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isAppInLockTaskMode(this)) {
            showLockTaskEscapeMessage()
            return
        }
        if (b.viewPager.currentItem > 0) {
            pageBack()
        } else {
            finish()
        }
    }

    fun onPageNextClick(@Suppress("UNUSED_PARAMETER") view: View) {
        pageNext()
    }

    fun pageNext() {
        b.viewPager.currentItem = b.viewPager.currentItem + 1
    }

    override fun onDilutionSelected(dilution: Int) {
        setDilution(dilution)
    }

    override fun onCustomDilution(dilution: Int) {
        setDilution(dilution)
    }

    private fun setDilution(dilution: Int) {
        currentDilution = dilution
        testInfo.dilution = dilution
        for (r in testInfo.results) {
            r.dilution = dilution
        }

        val currentPage = b.viewPager.currentItem
        setupInstructions(
            testInfo,
            isExternalSurvey,
            instructionList,
            pageIndex,
            currentDilution,
            isCalibration,
            redoTest,
            this
        )

        val adapter = b.viewPager.adapter as TestPagerAdapter
        adapter.testInfo = testInfo
        adapter.instructions = instructionList
        adapter.pageIndex = pageIndex

        b.viewPager.adapter!!.notifyDataSetChanged()
        b.viewPager.currentItem = currentPage

        mainScope.launch {
            delay(300)
            pageNext()
        }

        val model = ViewModelProvider(this).get(
            TestInfoViewModel::class.java
        )
        model.setTest(testInfo)

        b.indicatorPgr.pageCount = pageIndex.totalPageCount
        showHideFooter(b.viewPager.currentItem)
    }

    fun onAcceptClick() {

        setTestResult()

        if (isCalibration && testInfo.subtype != TestType.CARD) {
            val db = CalibrationDatabase.getDatabase(this)
            try {
                val dao = db.calibrationDao()
                val calibrationDetail = dao.getCalibrationDetail(testInfo.uuid)
                calibrationDetail!!.date = Calendar.getInstance().timeInMillis
                calibrationDetail.name = ""
                calibrationDetail.desc = ""
                val calibration = Calibration(
                    calibrationDetail.calibrationId,
                    testViewModel.calibrationPoint, testViewModel.calibrationColor
                )
                dao.insert(calibration)
                dao.update(calibrationDetail)
                testViewModel.loadCalibrations()
            } finally {
                db.close()
            }

            b.viewPager.setCurrentItem(1, false)
            b.footerLyt.visibility = View.GONE
            stopScreenPinning()
        } else {
            stopScreenPinning()
            finish()
        }
    }

    fun onStartTimerClick(@Suppress("UNUSED_PARAMETER") view: View) {
        pageNext()
        mainScope.launch {
            delay(200)
            LocalBroadcastManager.getInstance(view.context)
                .sendBroadcast(Intent(BROADCAST_RESET_TIMER))
        }
    }

    fun onSkipTimeDelayClick(@Suppress("UNUSED_PARAMETER") view: View) {
        pageNext()
        mainScope.launch {
            delay(200)
            val intent = Intent(BROADCAST_RESET_TIMER)
            intent.putExtra(BROADCAST_SKIP_TIMER, true)
            LocalBroadcastManager.getInstance(view.context)
                .sendBroadcast(intent)
        }
    }

    fun onRetestClick(@Suppress("UNUSED_PARAMETER") view: View) {
        b.viewPager.setCurrentItem(pageIndex.dilutionPage, false)
    }

    fun onTestSelected(testInfo: TestInfo?, redo: Boolean) {

        if (testInfo == null) {
            return
        }

        this.testInfo = testInfo
        testInfo.subTest().splitRanges()
        testViewModel.setTest(testInfo)
        testViewModel.loadCalibrations()

        if (!isCalibration) {
            if (!isDiagnosticMode() && testInfo.subtype == TestType.CUVETTE &&
                !AppPreferences.useExternalSensor(this) && !isSwatchListValid(testInfo, false)
            ) {
                alertCalibrationIncomplete(
                    this, testInfo
                )
                return
            }

            val db = CalibrationDatabase.getDatabase(this)
            try {
                val calibrationDetail = db.calibrationDao().getCalibrationDetail(testInfo.uuid)
                if (calibrationDetail != null) {
                    val milliseconds = calibrationDetail.expiry
                    if (milliseconds > 0 && milliseconds <= Date().time) {
                        if (!isDiagnosticMode()) {
                            alertCalibrationExpired(this)
                            return
                        }
                    }
                }
            } finally {
                db.close()
            }
        }

        redoTest = redo

        title = testInfo.name?.toLocalString()

        setupInstructions(
            testInfo,
            isExternalSurvey,
            instructionList,
            pageIndex,
            currentDilution,
            isCalibration,
            redoTest,
            this
        )
        showHideFooter(b.viewPager.currentItem)

        b.indicatorPgr.pageCount = pageIndex.totalPageCount

        val adapter = TestPagerAdapter(this, testInfo, isExternalSurvey)
        adapter.testInfo = testInfo
        adapter.instructions = instructionList
        adapter.pageIndex = pageIndex
        b.viewPager.adapter = adapter
        adapter.notifyDataSetChanged()

        b.viewPager.currentItem = 0


        testViewModel.isCalibration = isCalibration

        pageNext()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (testViewModel.isCalibration && b.viewPager.currentItem == 1
        ) {
            if (isDiagnosticMode()) {
                menuInflater.inflate(R.menu.menu_calibrate_dev, menu)
            } else {
                menuInflater.inflate(R.menu.menu_calibrate, menu)
            }
        }
        if ((::testInfo.isInitialized && testInfo.subtype != TestType.CARD) &&
            instructionList.size > 0 && b.viewPager.currentItem > 0 &&
            instructionList[b.viewPager.currentItem].section.size > 0
        ) {
            if (b.viewPager.currentItem != pageIndex.dilutionPage &&
                b.viewPager.currentItem != pageIndex.calibrationPage &&
                b.viewPager.currentItem < pageIndex.testPage - 1 &&
                (pageIndex.startTimerPage == -1 || b.viewPager.currentItem < pageIndex.startTimerPage)
            ) {
                menuInflater.inflate(R.menu.menu_instructions, menu)
            }
        }
        return true
    }

    fun onSkipClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        if (pageIndex.testPage > 0) {
            MainScope().launch {
                for (i in b.viewPager.currentItem until pageIndex.testPage - 1) {
                    delay(50)
                    pageNext()
                }
            }
//            b.viewPager.currentItem = pageIndex.testPage - 1
        }
    }

    private fun stopScreenPinning() {
        try {
            stopLockTask()
        } catch (ignored: Exception) {
        }
    }

    override fun onCalibrationSelected(position: Int) {
        val db = CalibrationDatabase.getDatabase(this)
        try {
            val calibrationDetail = db.calibrationDao().getCalibrationDetail(testInfo.uuid)
            if (calibrationDetail == null) {
                showCalibrationExpiryDialog(true)
            } else {
                pageNext()
            }
        } finally {
            db.close()
        }
    }

    private fun showCalibrationDetailsDialog(isEdit: Boolean) {
        val ft = supportFragmentManager.beginTransaction()
        val calibrationDetailsDialog =
            CalibrationDetailsDialog.newInstance(testInfo, isEdit)
        calibrationDetailsDialog.show(ft, "calibrationDetails")
    }

    private fun showCalibrationExpiryDialog(isEdit: Boolean) {
        val ft = supportFragmentManager.beginTransaction()
        val calibrationExpiryDialog =
            CalibrationExpiryDialog.newInstance(testInfo, isEdit)
        calibrationExpiryDialog.show(ft, "calibrationExpiry")
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        registerBroadcastReceiver()
        mainScope.launch {
            val uuid = intent.getStringExtra(TEST_ID_KEY)
            if (isCalibration && uuid != null) {
                onTestSelected(getTestInfo(uuid, baseContext), false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::timerScope.isInitialized) {
            timerScope.cancel()
        }
        if (::broadcastManager.isInitialized) {
            broadcastManager.unregisterReceiver(testCompletedBroadcastReceiver)
        }
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            when {
                granted -> {
                    runCuvetteTest()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    pageBack()
                    findViewById<ConstraintLayout>(R.id.root_layout)
                        .snackBar(getString(R.string.camera_permission))
                }
                else -> {
                    pageBack()
                    findViewById<ConstraintLayout>(R.id.root_layout)
                        .snackBarAction(
                            getString(R.string.camera_permission),
                            SnackbarActionListener()
                        )
                }
            }
        }

    class SnackbarActionListener : View.OnClickListener {
        override fun onClick(v: View) {
            openAppPermissionSettings(v.context)
        }
    }

    private fun showError(message: String, bitmap: Bitmap?) {
        stopScreenPinning()
        SoundUtil.playShortResource(this, R.raw.error)
        alertDialogToBeDestroyed = AlertUtil.showError(
            this, R.string.error, message, bitmap, R.string.ok,
            { _: DialogInterface?, _: Int ->
                stopScreenPinning()
                finish()
            },
            { dialogInterface: DialogInterface, _: Int ->
                stopScreenPinning()
                dialogInterface.dismiss()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }, null
        )
    }

    override fun onCalibrationDetailsSaved() {
        SnackbarUtils.showLongSnackbar(b.footerLyt, getString(R.string.calibration_saved))
    }

    override fun onCalibrationExpirySavedListener() {
        hideSystemUI()
        pageNext()
    }

    override fun onDestroy() {
        broadcastManager.unregisterReceiver(colorCardCapturedBroadcastReceiver)
        broadcastManager.unregisterReceiver(resultBroadcastReceiver)
        if (alertDialogToBeDestroyed != null) {
            alertDialogToBeDestroyed!!.dismiss()
        }
        super.onDestroy()
        showSystemUI()
    }

    override fun onResult(calibration: Calibration?) {
        val intent = Intent(BROADCAST_TEST_COMPLETED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            intent
        )
    }

    override fun onResult() {
        val intent = Intent(BROADCAST_TEST_COMPLETED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            intent
        )
    }

    override fun onStartTest() {
        pageNext()
    }

    override fun onConfirmImage(action: Int) {
        if (action == RESULT_OK) {
            if (isCalibration) {
                val subTest = testInfo.subTest()
                for (v in subTest.values) {
                    if (v.calibrate) {
                        subTest.calibratedResult.calibratedValue = CalibrationValue(value = v.value)
                        break
                    }
                }
            }
            b.viewPager.currentItem = pageIndex.resultPage
        } else {
            val testPagerAdapter = TestPagerAdapter(this, testInfo, isExternalSurvey)
            testPagerAdapter.pageIndex = pageIndex
            b.viewPager.adapter = testPagerAdapter
        }
    }

    override fun onCalibrationOption(calibrate: Boolean) {
        setCalibration(this, calibrate)
        isCalibration = calibrate
        setupInstructions(
            testInfo,
            isExternalSurvey,
            instructionList,
            pageIndex,
            currentDilution,
            isCalibration,
            redoTest,
            this
        )
        val testPagerAdapter = TestPagerAdapter(this, testInfo, isExternalSurvey)
        testPagerAdapter.testInfo = testInfo
        testPagerAdapter.instructions = instructionList

        b.indicatorPgr.pageCount = pageIndex.totalPageCount

        testViewModel.loadCalibrations()
        testViewModel.isCalibration = isCalibration

        showHideFooter(b.viewPager.currentItem)

        testPagerAdapter.pageIndex = pageIndex
        b.viewPager.adapter = testPagerAdapter
        b.indicatorPgr.showDots = true
        b.viewPager.currentItem = 1
        pageNext()
    }

    fun submitResult() {
        val resultIntent = Intent()
        val subTest = testInfo.subTest()
        if (subTest.getResult() >= 0) {
            val db = AppDatabase.getDatabase(baseContext)
            val form = db.resultDao().getResult(testInfo.fileName)
            if (form != null) {
                sendResultToCloudDatabase(testInfo, form)
                resultIntent.putExtra(TEST_VALUE_KEY, subTest.getResultString())
                resultIntent.putExtra(
                    testInfo.name + "_Result",
                    subTest.getResultString()
                )
                resultIntent.putExtra(testInfo.name + "_Risk", subTest.getRiskEnglish(this))
                resultIntent.putExtra("meta_device", Build.BRAND + ", " + Build.MODEL)
            }
        } else {
            resultIntent.putExtra(TEST_VALUE_KEY, "")
        }
        setTestResult()
        finish()
    }

    private fun sendResultToCloudDatabase(testInfo: TestInfo, form: TestResult) {
        if (!AppPreferences.getShareData()) {
            return
        }
        if (!BuildConfig.INSTRUMENTED_TEST_RUNNING.get()) {
            val path = if (BuildConfig.DEBUG) {
                "result-debug"
            } else {
                "result"
            }

            val subTest = testInfo.subTest()
            val result = RemoteResult(
                testInfo.uuid,
                testInfo.name,
                testInfo.sampleType.toString(),
                testInfo.subtype.toString(),
                subTest.getRiskEnglish(this),
                subTest.getResultString(),
                subTest.unit,
                System.currentTimeMillis(),
                AppPreferences.getEmailAddress(),
                form.latitude,
                form.longitude,
                form.geoAccuracy,
                form.comment,
                App.getAppVersion(true),
                Build.MODEL,
                Build.VERSION.RELEASE
            )

            val db = Firebase.firestore
            db.collection("users").document(AppPreferences.getEmailAddress()).collection("results")
                .add(result)

            val filePath = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    separator + "captures" + separator
            val fileName = testInfo.name!!.replace(" ", "")

            val storageRef = FirebaseStorage.getInstance().reference
            storageRef.child(path + "/${testInfo.fileName}/swatch.jpg")
                .putFile(Uri.fromFile(File(filePath + testInfo.fileName + separator + fileName + "_swatch.jpg")))
                .addOnFailureListener {
                    Timber.e(it)
                }.addOnSuccessListener {
                }

            storageRef.child(path + "/${testInfo.fileName}/image.jpg")
                .putFile(Uri.fromFile(File(filePath + testInfo.fileName + separator + fileName + ".jpg")))
                .addOnFailureListener {
                    Timber.e(it)
                }.addOnSuccessListener {
                }

            storageRef.child(path + "/${testInfo.fileName}/result.png")
                .putFile(Uri.fromFile(File(filePath + testInfo.fileName + separator + fileName + "_result.png")))
                .addOnFailureListener {
                    Timber.e(it)
                }.addOnSuccessListener {
                }
        }
    }

    companion object {
        init {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            } catch (_: Exception) {
            }
        }
    }
}
