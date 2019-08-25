package io.ffem.lite.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.API_URL
import io.ffem.lite.app.App.Companion.PERMISSIONS_MISSING_KEY
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_NAME_KEY
import io.ffem.lite.app.App.Companion.TEST_PARAMETER_NAME
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.helper.ApkHelper.isNonStoreVersion
import io.ffem.lite.model.ResultResponse
import io.ffem.lite.model.TestResult
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.preference.sendDummyImage
import io.ffem.lite.remote.ApiService
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.SoundUtil
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
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.max


const val APP_UPDATE_REQUEST = 101
const val TOAST_Y_OFFSET = 240
const val RESULT_CHECK_INTERVAL = 15000L
const val MIN_RESULT_WAIT_TIME = 80000L
const val SNACK_BAR_LINE_SPACING = 1.4f

class ResultListActivity : BaseActivity() {

    private var appIsClosing: Boolean = false
    private lateinit var listView: RecyclerView
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var toastLong: Toast
    private lateinit var toastShort: Toast
    private var isInternetConnected = false

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

        setTitle(R.string.app_name)

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
            if (isInternetConnected) {
                getResultsFromServer()
            }
        }

        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.MONTH) > 7 && calendar.get(Calendar.YEAR) > 2018
            && isNonStoreVersion(this)
        ) {
            appIsClosing = true
            val marketUrl = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
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

        } else {

            checkForUpdate()

            db = AppDatabase.getDatabase(baseContext)

//            db.resultDao().reset()

            val resultList = db.resultDao().getResults()

            adapter.setTestList(resultList)

            listView = findViewById(R.id.list_results)
            listView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

            listView.adapter = adapter

            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
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

        if (!appIsClosing) {

            monitorNetwork()

            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            IMMEDIATE,
                            this,
                            APP_UPDATE_REQUEST
                        )
                    }
                }

            sendImagesToServer()

            startResultCheckTimer(RESULT_CHECK_INTERVAL)
        }
    }

    override fun onPause() {
        super.onPause()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        resultRequestHandler.removeCallbacksAndMessages(null)
    }

    fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent: Intent? = Intent(baseContext, BarcodeActivity::class.java)
        startActivityForResult(intent, 100)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Timber.d("Connection Available")
            isInternetConnected = true
            sendImagesToServer()
            getResultsFromServer()
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Timber.d("Connection Not Available")
            isInternetConnected = false
            resultRequestHandler.removeCallbacksAndMessages(null)
            notifyNoInternet()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Timber.d("Connection Lost")
            isInternetConnected = false
            resultRequestHandler.removeCallbacksAndMessages(null)
            notifyNoInternet()
        }
    }

    private fun monitorNetwork() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (ignore: Exception) {
        }

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

        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {

                val id = data.getStringExtra(TEST_ID_KEY)
                if (id != null) {
                    AppDatabase.getDatabase(baseContext).resultDao().insert(
                        TestResult(
                            id, 0, TEST_PARAMETER_NAME,
                            Date().time, Date().time, "", getString(R.string.outbox)
                        )
                    )
                }

                refreshList()

                if (sendDummyImage()) {
                    showNewToast(getString(R.string.sending_dummy_image))
                }
            }
        } else {
            if (data != null) {
                if (data.getBooleanExtra(PERMISSIONS_MISSING_KEY, false)) {
                    showSnackbar(getString(R.string.camera_storage_permission))
                }
            }
        }
    }

    private fun startResultCheckTimer(delay: Long) {
        if (db.resultDao().getPendingResults().isNotEmpty()) {
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

        sendImagesToServer()
        getResultsFromServer()
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
        val filePath = "$path$id" + "_" + "$name.jpg"

        try {
            val imageDescription = "{\"test_type\" : $name}"
            val exif = ExifInterface(filePath)
            exif.setAttribute("ImageDescription", imageDescription)
            exif.saveAttributes()
        } catch (ignore: IOException) {
        }

        try {
            val file = File(filePath)
            val contentType = file.toURI().toURL().openConnection().contentType

            val fileBody = file.asRequestBody(contentType.toMediaTypeOrNull())
            val filename = file.name

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", "1")
                .addFormDataPart("testId", id)
                .addFormDataPart("versionCode", BuildConfig.VERSION_CODE.toString())
                .addFormDataPart("sdkVersion", Build.VERSION.SDK_INT.toString())
                .addFormDataPart("deviceModel", Build.MODEL)
                .addFormDataPart("image", filename, fileBody)
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
                    response.body!!.close()

                    db.resultDao().updateStatus(id, 1, Date().time, getString(R.string.analyzing))

                    this@ResultListActivity.runOnUiThread {
                        notifyFileSent()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                            Timber.e(t.toString())
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

    private fun showNewToast(message: String) {
        toastShort.cancel()
        toastShort = Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_SHORT
        )
        toastShort.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
        toastShort.show()
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
        startActivity(intent)
    }
}
