package io.ffem.lite.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import androidx.databinding.BindingAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.LOCAL_RESULT_EVENT
import io.ffem.lite.app.App.Companion.PERMISSIONS_MISSING_KEY
import io.ffem.lite.app.App.Companion.TEST_INFO_KEY
import io.ffem.lite.app.App.Companion.getVersionName
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.helper.ApkHelper.isNonStoreVersion
import io.ffem.lite.model.ResultDetail
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.TestResult
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.preference.useDummyImage
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.FileUtil
import io.ffem.lite.util.PreferencesUtil
import kotlinx.android.synthetic.main.activity_result_list.*
import timber.log.Timber
import java.io.File
import java.util.*

const val READ_REQUEST_CODE = 102
const val PERMISSION_REQUEST = 103

const val SNACK_BAR_LINE_SPACING = 1.4f

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
        view.text = result.value.toString()
    }
}

class ResultListActivity : AppUpdateActivity() {

    private var appIsClosing: Boolean = false
    private lateinit var toastLong: Toast
    private lateinit var toastShort: Toast
    private lateinit var broadcastManager: LocalBroadcastManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

            val basePath = File(path)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            val testInfo = intent.getParcelableExtra<TestInfo>(TEST_INFO_KEY)

            db.resultDao().updateResult(
                testInfo!!.fileName,
                testInfo.name!!,
                testInfo.resultDetail.result,
                testInfo.error.ordinal
            )

            refreshList()
        }
    }

    private fun onResultClick(position: Int) {
        Handler().postDelayed(
            {
                val item = adapter.getItemAt(position)
                val intent = Intent(baseContext, ImageViewActivity::class.java)

                val result = db.resultDao().getResult(item.id)!!
                val testInfo = App.getTestInfo(item.uuid)
                testInfo!!.error = item.error
                testInfo.fileName = item.id
                testInfo.resultDetail = ResultDetail(result.value)

                intent.putExtra(TEST_INFO_KEY, testInfo)
                startActivity(intent)
            }, 350
        )
    }

    private var adapter: ResultAdapter = ResultAdapter(clickListener = {
        onResultClick(it)
    })

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_list)

        title = getString(R.string.app_name) + " - " + getVersionName()

        broadcastManager = LocalBroadcastManager.getInstance(this)

        @SuppressLint("ShowToast")
        toastLong = Toast.makeText(
            applicationContext,
            "",
            Toast.LENGTH_LONG
        )

        @SuppressLint("ShowToast")
        toastShort = Toast.makeText(
            applicationContext,
            "",
            Toast.LENGTH_SHORT
        )

        if (BuildConfig.BUILD_TYPE == "release" && isNonStoreVersion(this)) {
            val appExpiryDate = GregorianCalendar.getInstance()
            appExpiryDate.time = BuildConfig.BUILD_TIME
            appExpiryDate.add(Calendar.DAY_OF_YEAR, 15)

            if (GregorianCalendar().after(appExpiryDate)) {

                appIsClosing = true
                val marketUrl =
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                val message = String.format(
                    "%s%n%n%s", getString(R.string.thisVersionHasExpired),
                    getString(R.string.uninstallAndInstallFromStore)
                )

                val builder: AlertDialog.Builder = AlertDialog.Builder(this)

                builder.setTitle(R.string.versionExpired)
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

        db = AppDatabase.getDatabase(baseContext)

        val resultList = db.resultDao().getResults()

        adapter.setTestList(resultList)

        list_results.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        list_results.adapter = adapter
    }

//    private fun checkForUpdate() {
//
//        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
//
//        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//                && appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)
//            ) {
//                appUpdateManager.startUpdateFlowForResult(
//                    appUpdateInfo,
//                    IMMEDIATE,
//                    this,
//                    APP_UPDATE_REQUEST
//                )
//            }
//        }
//    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {

        AppPreferences.checkDiagnosticModeExpiry()

        super.onResume()

        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(LOCAL_RESULT_EVENT)
        )

//        if (!appIsClosing) {
//
//            appUpdateManager
//                .appUpdateInfo
//                .addOnSuccessListener { appUpdateInfo ->
//                    if (appUpdateInfo.updateAvailability() ==
//                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
//                    ) {
//                        appUpdateManager.startUpdateFlowForResult(
//                            appUpdateInfo,
//                            IMMEDIATE,
//                            this,
//                            APP_UPDATE_REQUEST
//                        )
//                    }
//                }
//        }

        list_results.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (useDummyImage()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (!isHasPermission(*permissions))
                askPermission(permissions = *permissions, requestCode = PERMISSION_REQUEST)
            else
                performFileSearch()

        } else {
            // Start camera preview
            val intent = Intent(baseContext, BarcodeActivity::class.java)
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        broadcastManager.unregisterReceiver(broadcastReceiver)
        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(LOCAL_RESULT_EVENT)
        )

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == READ_REQUEST_CODE) {
                data?.data?.also { uri ->

                    val fiePath = FileUtil.getPath(this, uri)
                    if (fiePath != null) {
                        val filePtah = File(fiePath)
                        if (filePtah.exists()) {
                            val bitmapFromFile =
                                BitmapFactory.decodeFile(filePtah.absolutePath)

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

                            val id = UUID.randomUUID().toString()
                            if (bitmapFromFile != null) {
                                ColorUtil.extractImage(this, id, bitmapFromFile)
                            } else {
                                Toast.makeText(
                                    baseContext, getString(R.string.invalid_image),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        } else {
            if (data != null) {
                if (data.getBooleanExtra(PERMISSIONS_MISSING_KEY, false)) {
                    showSnackbar(getString(R.string.camera_storage_permission))
                }
            }
        }

        refreshList()
    }

    private fun showSnackbar(message: String) {
        val rootView = window.decorView.rootView
        val snackbar = Snackbar
            .make(rootView, message.trim { it <= ' ' }, Snackbar.LENGTH_LONG)
            .setAction("SETTINGS") { App.openAppPermissionSettings(this) }
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById<TextView>(R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.setLineSpacing(0f, SNACK_BAR_LINE_SPACING)
        snackbar.setActionTextColor(Color.YELLOW)
        snackbar.show()
    }

    private fun refreshList() {
        adapter.setTestList(db.resultDao().getResults())
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val intent = Intent(baseContext, SettingsActivity::class.java)
        startActivityForResult(intent, 102)
    }

    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    private fun Activity.isHasPermission(vararg permissions: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            permissions.all { singlePermission ->
                applicationContext.checkSelfPermission(singlePermission) ==
                        PackageManager.PERMISSION_GRANTED
            }
        else true
    }

    private fun Activity.askPermission(
        vararg permissions: String,
        @IntRange(from = 0) requestCode: Int
    ) =
        ActivityCompat.requestPermissions(this, permissions, requestCode)

    private fun permissionsGranted(grantResults: IntArray): Boolean {
        for (element in grantResults) {
            if (element != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST -> {
                if (permissionsGranted(grantResults)) {
                    performFileSearch()
                } else {
                    showSnackbar(getString(R.string.storage_permission))
                }
                return
            }
        }
    }
}
