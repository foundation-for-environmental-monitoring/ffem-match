package io.ffem.lite.ui

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.BindingAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getVersionName
import io.ffem.lite.common.IS_CALIBRATION
import io.ffem.lite.common.RESULT_EVENT_BROADCAST
import io.ffem.lite.common.TEST_INFO_KEY
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.TestResult
import io.ffem.lite.databinding.ActivityResultListBinding
import io.ffem.lite.helper.ApkHelper.isNonStoreVersion
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.preference.useDummyImage
import io.ffem.lite.util.BarcodeColorUtil
import io.ffem.lite.util.FileUtil.getPathFromURI
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.snackBar
import io.ffem.lite.util.toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*

@BindingAdapter("android:resultSize")
fun TextView.bindTextSize(result: Double) {
    if (result < 0) {
        textSize = 14f
        setPadding(0, 10, 0, 0)
        setTextColor(Color.rgb(200, 50, 50))
    } else {
        textSize = 30f
        setPadding(0, 0, 0, 0)
        setTextColor(Color.rgb(20, 20, 20))
    }
}

@BindingAdapter("android:result")
fun getResultString(view: TextView, result: TestResult) {
    if (result.value < 0) {
        view.text = result.error.toLocalString(view.context)
    } else {
        if (result.value >= result.maxValue) {
            val value = "> " + result.value.toString()
            view.text = value
        } else {
            view.text = result.value.toString()
        }
    }
}

class ResultListActivity : AppUpdateActivity() {
    private lateinit var binding: ActivityResultListBinding
    private lateinit var db: AppDatabase
    private var appIsClosing: Boolean = false
    private lateinit var broadcastManager: LocalBroadcastManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

            val basePath = File(path)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            val testInfo = intent.getParcelableExtra<TestInfo>(TEST_INFO_KEY)

            if (testInfo != null) {
                db.resultDao().updateResult(
                    testInfo.fileName,
                    testInfo.uuid!!,
                    testInfo.name!!,
                    testInfo.getResult(),
                    testInfo.resultInfoGrayscale.result,
                    testInfo.getMarginOfError(),
                    testInfo.error.ordinal
                )
            }
            refreshList()
        }
    }

    private fun onDeleteResultClick(position: Int): Boolean {
        val row = binding.testResultsLst.layoutManager?.findViewByPosition(position)
        row?.background = ColorDrawable(Color.LTGRAY)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.delete))
        builder.setMessage(getString(R.string.delete_selected_result))
        builder.setPositiveButton(R.string.delete) { _, _ ->
            val item = adapter.getItemAt(position)
            db.resultDao().deleteResult(item.id)
            val path =
                getExternalFilesDir(DIRECTORY_PICTURES).toString() + File.separator + "captures"
            val directory = File("$path${File.separator}${item.id}${File.separator}")
            if (directory.exists() && directory.isDirectory) {
                directory.deleteRecursively()
            }

            refreshList()

            findViewById<CoordinatorLayout>(R.id.coordinator_lyt)
                ?.snackBar(getString(R.string.deleted))
        }

        builder.setNegativeButton(android.R.string.cancel) { _, _ ->
            row?.background = ColorDrawable(Color.TRANSPARENT)
        }

        builder.setOnCancelListener {
            row?.background = ColorDrawable(Color.TRANSPARENT)
        }

        builder.show()
        return true
    }

    private fun onResultClick(position: Int) {
        MainScope().launch {
            delay(300)
            val item = adapter.getItemAt(position)
            val intent = Intent(baseContext, ResultViewActivity::class.java)

            val result = db.resultDao().getResult(item.id)!!
            val testInfo = App.getTestInfo(item.uuid)
            testInfo!!.error = item.error
            testInfo.fileName = item.id
            testInfo.resultInfo = ResultInfo(result.value, result.luminosity)
            testInfo.resultInfoGrayscale = ResultInfo(result.valueGrayscale)
            testInfo.setMarginOfError(result.marginOfError)

            intent.putExtra(TEST_INFO_KEY, testInfo)
            startActivity(intent)
        }
    }

    private var adapter: ResultAdapter = ResultAdapter(
        clickListener = { onResultClick(it) },
        longClickListener = { onDeleteResultClick(it) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        title = getString(R.string.app_name) + " - " + getVersionName()

        broadcastManager = LocalBroadcastManager.getInstance(this)

        if (BuildConfig.BUILD_TYPE == "release" && isNonStoreVersion(this)) {
            val appExpiryDate = GregorianCalendar.getInstance()
            appExpiryDate.time = BuildConfig.BUILD_TIME
            appExpiryDate.add(Calendar.DAY_OF_YEAR, 15)

            if (GregorianCalendar().after(appExpiryDate)) {

                appIsClosing = true
                val marketUrl =
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                val message = String.format(
                    "%s%n%n%s", getString(R.string.version_has_expired),
                    getString(R.string.uninstall_install_from_store)
                )

                val builder: AlertDialog.Builder = AlertDialog.Builder(this)

                builder.setTitle(R.string.version_expired)
                    .setMessage(message)
                    .setCancelable(false)

                builder.setPositiveButton(R.string.ok) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = marketUrl
                    intent.setPackage("com.android.vending")
                    startActivity(intent)
                    finishAndRemoveTask()
                }

                val alertDialog = builder.create()
                alertDialog.show()
                return
            }
        }

        binding.testResultsLst.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        db = AppDatabase.getDatabase(baseContext)
        val resultList = db.resultDao().getResults()
        adapter.setTestList(resultList)
        binding.testResultsLst.adapter = adapter

        showHideList()
    }

    private fun showHideList() {
        if (adapter.itemCount > 0) {
            binding.testResultsLst.visibility = VISIBLE
            binding.launchInfoTxt.visibility = GONE
            binding.noResultTxt.visibility = GONE
        } else {
            binding.testResultsLst.visibility = GONE
            binding.launchInfoTxt.visibility = VISIBLE
            binding.noResultTxt.visibility = VISIBLE
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {

        AppPreferences.checkDiagnosticModeExpiry()

        super.onResume()

        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(RESULT_EVENT_BROADCAST)
        )

        binding.testResultsLst.adapter = adapter

        showHideList()

        if (useDummyImage()) {
            binding.loadImageFab.visibility = VISIBLE
        } else {
            binding.loadImageFab.visibility = GONE
        }
    }

    override fun onPause() {
        super.onPause()
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

    fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        PreferencesUtil.setBoolean(this, IS_CALIBRATION, false)
        val intent = Intent(baseContext, BarcodeActivity::class.java)
        startTest.launch(intent)
    }

    private fun refreshList() {
        adapter.setTestList(db.resultDao().getResults())
        binding.testResultsLst.adapter = adapter
        adapter.notifyDataSetChanged()
        showHideList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val intent = Intent(baseContext, SettingsActivity::class.java)
        startSettings.launch(intent)
    }

    private fun performFileSearch() {
        PreferencesUtil.setBoolean(this, IS_CALIBRATION, false)
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startFileExplorer.launch(intent)
    }

    private val startTest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            reset()
        }

    private val startSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            reset()
        }

    private fun reset() {
        broadcastManager.unregisterReceiver(broadcastReceiver)
        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(RESULT_EVENT_BROADCAST)
        )
        refreshList()
    }

    private val startFileExplorer =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.data?.also { uri ->
                val fileUrl = getPathFromURI(this, uri)
                if (fileUrl != null) {
                    val filePath = File(fileUrl)
                    if (filePath.exists()) {
                        val bitmapFromFile =
                            BitmapFactory.decodeFile(filePath.absolutePath)

                        var imageNumber = uri.pathSegments[uri.pathSegments.size - 1]
                            .substringAfterLast("_", "")
                            .substringBeforeLast(".")

                        try {
                            imageNumber = imageNumber.toInt().toString()
                        } catch (ignored: Exception) {
                        }

                        PreferencesUtil.setString(
                            this,
                            R.string.testImageNumberKey,
                            imageNumber
                        )

                        try {
                            if (bitmapFromFile != null) {
                                AppPreferences.generateImageFileName()
                                binding.progressLyt.visibility = VISIBLE
                                BarcodeColorUtil.extractImage(this, bitmapFromFile)
                                MainScope().launch {
                                    delay(3500)
                                    refreshList()
                                    binding.progressLyt.visibility = GONE
                                    onResultClick(0)
                                }
                            } else {
                                toast(getString(R.string.invalid_image), Toast.LENGTH_LONG)
                            }
                        } catch (e: Exception) {
                            e.message?.let { it1 -> toast(it1, Toast.LENGTH_LONG) }
                        }
                    }
                }
            }
        }

    fun onLoadFileClick(@Suppress("UNUSED_PARAMETER") view: View) {
        performFileSearch()
    }
}
