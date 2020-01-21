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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import androidx.databinding.BindingAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.API_URL
import io.ffem.lite.app.App.Companion.LOCAL_RESULT_EVENT
import io.ffem.lite.app.App.Companion.PERMISSIONS_MISSING_KEY
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_NAME_KEY
import io.ffem.lite.app.App.Companion.TEST_RESULT
import io.ffem.lite.app.App.Companion.getVersionName
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.helper.ApkHelper.isNonStoreVersion
import io.ffem.lite.model.ResultResponse
import io.ffem.lite.model.TestResult
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.preference.sendDummyImage
import io.ffem.lite.remote.ApiService
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.FileUtil
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.SoundUtil
import kotlinx.android.synthetic.main.activity_result_list.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import kotlin.math.abs
import kotlin.math.max

const val APP_UPDATE_REQUEST = 101
const val READ_REQUEST_CODE = 102
const val PERMISSION_REQUEST = 103

const val TOAST_Y_OFFSET = 240
const val RESULT_CHECK_INTERVAL = 10000L
const val MIN_RESULT_WAIT_TIME = 70000L
const val SNACK_BAR_LINE_SPACING = 1.4f

@BindingAdapter("android:resultSize")
fun TextView.bindTextSize(result: String) {
    if (result.toDoubleOrNull() == null) {
        textSize = 14f
        setPadding(0, 10, 0, 0)
        setTextColor(Color.rgb(200, 50, 50))
    } else {
        textSize = 30f
        setPadding(0, 0, 0, 0)
        setTextColor(Color.rgb(20, 20, 20))
    }
}

class ResultListActivity : BaseActivity() {

    private var appIsClosing: Boolean = false
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var toastLong: Toast
    private lateinit var toastShort: Toast
    private var isInternetConnected = true
    private lateinit var broadcastManager: LocalBroadcastManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

            val basePath = File(path)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            db.resultDao().updateLocalResult(
                intent.getStringExtra(TEST_ID_KEY)!!,
                intent.getStringExtra(TEST_RESULT)!!
            )

            refreshList()
        }
    }

    private fun onResultClick(position: Int) {
        Handler().postDelayed(
            {
                val item = adapter.getItemAt(position)
                val intent = Intent(baseContext, ResultActivity::class.java)
                intent.putExtra(TEST_ID_KEY, item.id)
                intent.putExtra(TEST_NAME_KEY, item.name)
                startActivity(intent)
            }, 350
        )
    }

    private var adapter: ResultAdapter = ResultAdapter(clickListener = {
        onResultClick(it)
    })

    private var requestCount = 0
    private lateinit var db: AppDatabase
    private lateinit var resultRequestHandler: Handler
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_list)

        title = getString(R.string.app_name) + " - " + getVersionName()

        broadcastManager = LocalBroadcastManager.getInstance(this)

        appUpdateManager = AppUpdateManagerFactory.create(this)

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

        resultRequestHandler = Handler()
        runnable = Runnable {
            analyzeImage()
            if (isInternetConnected) {
//                sendImagesToServer()
//                getResultsFromServer()
            }
        }

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAndRemoveTask()
                    } else {
                        finish()
                    }
                }

                val alertDialog = builder.create()
                alertDialog.show()
                return
            }

        }


        checkForUpdate()

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

    private fun checkForUpdate() {

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    IMMEDIATE,
                    this,
                    APP_UPDATE_REQUEST
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {
        super.onResume()

        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(LOCAL_RESULT_EVENT)
        )

        if (!appIsClosing) {

            monitorNetwork()

            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            IMMEDIATE,
                            this,
                            APP_UPDATE_REQUEST
                        )
                    }
                }

            startResultCheckTimer(RESULT_CHECK_INTERVAL)
        }

        list_results.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        resultRequestHandler.removeCallbacksAndMessages(null)
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::connectivityManager.isInitialized) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (sendDummyImage()) {

            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (!isHasPermission(*permissions))
                askPermission(permissions = *permissions, requestCode = PERMISSION_REQUEST)
            else
                performFileSearch()

        } else {
            PreferencesUtil.removeKey(this, R.string.expectedValueKey)
            showInputDialog()
        }
    }

    private fun showInputDialog() {
        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.value_input_dialog, null)
        val inputValue = view.findViewById(R.id.editExpectedValue) as EditText

        val builder = AlertDialog.Builder(this)
            .setTitle("Expected result")
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                closeKeyboard(this, inputValue)
                dialog.dismiss()
            }

        val dialog = builder.setView(view).create()

        dialog.setOnShowListener { d ->
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                var value = inputValue.text.toString()

                if (!BuildConfig.DEBUG && value.isNotEmpty() &&
                    (value.toFloat() < 0 || value.toFloat() > 10)
                ) {
                    inputValue.error = "Should be between 0 and 10"
                } else {
                    closeKeyboard(this, inputValue)

                    if (value.isNotEmpty() && !value.contains(".")) {
                        value += ".0"
                    }

                    PreferencesUtil.setString(this, R.string.expectedValueKey, value)

                    d.dismiss()

                    val intent = Intent(baseContext, BarcodeActivity::class.java)
                    startActivityForResult(intent, 100)
                }
            }
        }

        inputValue.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }

        dialog.show()
        inputValue.requestFocus()
        showKeyboard(this)
    }

    private fun showKeyboard(context: Context) {
        val imm = context.getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun closeKeyboard(context: Context, input: EditText) {
        try {
            val imm = context.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            if (currentFocus != null) {
                val view: View = currentFocus!!
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        } catch (e: java.lang.Exception) {
            Timber.e(e)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Timber.d("Connection Available")
            isInternetConnected = true
            startResultCheckTimer(RESULT_CHECK_INTERVAL)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Timber.d("Connection Not Available")
//            isInternetConnected = false
            resultRequestHandler.removeCallbacksAndMessages(null)
            notifyNoInternet()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Timber.d("Connection Lost")
//            isInternetConnected = false
            resultRequestHandler.removeCallbacksAndMessages(null)
            notifyNoInternet()
        }
    }

    private fun monitorNetwork() {
        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val builder = NetworkRequest.Builder()
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }

        Handler().postDelayed({
            if (!isFinishing && !isDestroyed) {
                notifyNoInternet()
            }
        }, 5000)
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

                    //                    val testName = data.getStringExtra(TEST_NAME_KEY)
//
//                    if (testName.isNullOrEmpty()) {
//                        return
//                    }

                    val bitmapFromFile =
                        BitmapFactory.decodeFile(FileUtil.getPath(this, uri))

                    var expectedValue = uri.pathSegments[uri.pathSegments.size - 1]
                        .substringAfterLast("_", "")
                        .substringBeforeLast(".")

                    try {
                        expectedValue.toFloat()
                        if (expectedValue.isNotEmpty() && !expectedValue.contains(".")) {
                            expectedValue += ".0"
                        }
                    } catch (ignored: Exception) {
                    }

                    PreferencesUtil.setString(this, R.string.expectedValueKey, expectedValue)

                    val id = UUID.randomUUID().toString()

                    ColorUtil.extractImage(this, id, bitmapFromFile)
                }
            } else if (data != null) {
                saveImageData(data)
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

    private fun saveImageData(data: Intent) {
        val id = data.getStringExtra(TEST_ID_KEY)

        val testName = data.getStringExtra(TEST_NAME_KEY)

        if (testName.isNullOrEmpty()) {
            return
        }

        if (id != null) {

            val expectedValue = PreferencesUtil
                .getString(this, R.string.expectedValueKey, "")

            db.resultDao().insert(
                TestResult(
                    id, 0, testName, Date().time,
                    Date().time, "", "", expectedValue, getString(R.string.outbox)
                )
            )
            analyzeImage()
        }
    }

    private fun startResultCheckTimer(delay: Long) {
        if (db.resultDao().getPendingResults().isNotEmpty()
            || db.resultDao().getUnsent().isNotEmpty()
        ) {
            Timber.d("Waiting for: %s", delay)
            resultRequestHandler.removeCallbacksAndMessages(null)
            resultRequestHandler.postDelayed(runnable, delay)
        }
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

    private fun notifyFileSent() {

        refreshList()

        Handler().postDelayed({
            showToast(getString(R.string.wait_few_minutes))
        }, 800)

        startResultCheckTimer(RESULT_CHECK_INTERVAL)
    }

    private fun analyzeImage() {
        db.resultDao().getPendingLocalResults().forEach {
            val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

            val fileName = it.name.replace(" ", "")
            val filePath = "$path${it.id}" + "_" + "$fileName.jpg"

            val file = File(filePath)

            val bitmap = BitmapFactory.decodeFile(file.path)

            if (bitmap != null) {
                ColorUtil.extractImage(this, it.id, bitmap)
            }

            refreshList()
        }
    }

    private fun sendImagesToServer() {
        if (isInternetConnected) {
            db.resultDao().getUnsent().forEach {
                sendToServer(it.id, it.name)
            }
        }
    }

    private fun sendToServer(id: String, name: String) {

        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                File.separator + "captures" + File.separator

        val fileName = name.replace(" ", "")
        val filePath = "$path$id" + "_" + "$fileName.jpg"

        try {
            val file = File(filePath)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", "1")
                .addFormDataPart("testId", id)
                .addFormDataPart("versionCode", BuildConfig.VERSION_CODE.toString())
                .addFormDataPart("sdkVersion", Build.VERSION.SDK_INT.toString())
                .addFormDataPart("deviceModel", Build.MODEL)
                .addFormDataPart("md5", calculateMD5ofFile(filePath))
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build()

            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Timber.d(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        db.resultDao()
                            .updateStatus(id, 1, Date().time, getString(R.string.analyzing))

                        this@ResultListActivity.runOnUiThread {
                            notifyFileSent()
                        }
                    } else {
                        Timber.d(response.message)
                    }
                    response.body!!.close()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateMD5ofFile(location: String): String {
        val bufferSize = 8192
        val fs = FileInputStream(location)
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(bufferSize)
        var bytes: Int
        do {
            bytes = fs.read(buffer, 0, bufferSize)
            if (bytes > 0)
                md.update(buffer, 0, bytes)

        } while (bytes > 0)

        val hexString = StringBuilder()
        val byteArray = md.digest()

        for (element in byteArray) {
            val hex = Integer.toHexString(element.toInt() and 0xFF)
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)

        }
        return hexString.toString()
    }

    private fun getResultsFromServer() {

        startResultCheckTimer(RESULT_CHECK_INTERVAL)

        if (!isInternetConnected) {
            notifyNoInternet()
            return
        }

        val resultList = db.resultDao().getPendingResults()
        if (resultList.isNotEmpty()) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            var maxWaitTime: Long = MIN_RESULT_WAIT_TIME

            val api = retrofit.create(ApiService::class.java)
            resultList.forEach {
                val timeAgoSent = System.currentTimeMillis() - it.sent
                val timeToWait = max(3, abs(timeAgoSent - MIN_RESULT_WAIT_TIME))
                if (timeToWait < maxWaitTime) {
                    maxWaitTime = timeToWait
                }
                if (timeAgoSent > MIN_RESULT_WAIT_TIME) {
                    val call = api.getResponse(it.id)

                    Timber.d("Requesting result %s", it.id)

                    call.enqueue(object : Callback<ResultResponse> {

                        override fun onResponse(
                            call: Call<ResultResponse>?,
                            response: Response<ResultResponse>?
                        ) {
                            val body = response?.body()

                            val id = body?.id
                            var result = ""
                            var message = "-"
                            if (body?.result != null) {
                                result = body.result.toString()
                                message = body.message.toString()
                            }

                            if (result.isNotEmpty()) {
                                val resultData = db.resultDao().getResult(id)

                                if (resultData != null) {
                                    db.resultDao().updateResult(id.toString(), 2, message, result)

                                    this@ResultListActivity.runOnUiThread {
                                        SoundUtil.playShortResource(
                                            applicationContext,
                                            R.raw.triangle
                                        )
                                        refreshList()
                                        showToast(getString(R.string.result_received))
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<ResultResponse>?, t: Throwable?) {
                            requestCount++
                        }
                    })
                }
            }
            startResultCheckTimer(maxWaitTime)
        }
    }

    private fun showToast(message: String) {
        toastLong.cancel()
        toastLong = Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_LONG
        )
        toastLong.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
        toastLong.show()
    }

    private fun notifyNoInternet() {

        if (isInternetConnected) {
            return
        }

        if (db.resultDao().getUnsent().isEmpty() && db.resultDao().getPendingResults().isEmpty()) {
            return
        }

        val lastNotified = PreferencesUtil.getLong(this, App.CONNECTION_ERROR_NOTIFIED_KEY)
        if (System.currentTimeMillis() - lastNotified < 180000) {
            return
        }

        Handler().postDelayed({
            if (!isFinishing && !isDestroyed) {
                PreferencesUtil.setLong(
                    this,
                    App.CONNECTION_ERROR_NOTIFIED_KEY,
                    System.currentTimeMillis()
                )
                val toast = Toast.makeText(
                    applicationContext,
                    R.string.no_Internet_connection,
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
                toast.show()
            }
        }, 2000)
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
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

    private fun Activity.askPermission(vararg permissions: String, @IntRange(from = 0) requestCode: Int) =
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
