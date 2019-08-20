package io.ffem.lite.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.API_URL
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
import io.ffem.lite.util.NetUtil
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


const val APP_UPDATE_REQUEST = 101
const val TOAST_Y_OFFSET = 280
const val RESULT_CHECK_INTERVAL = 30000L
const val RESULT_CHECK_START_DELAY = 3000L

class ResultListActivity : BaseActivity() {

    private var appIsClosing: Boolean = false
    private var resultPollingStarted: Boolean = false
    private lateinit var listView: RecyclerView
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var appUpdateManager: AppUpdateManager

    private fun onResultClick(position: Int) {
        resultRequestHandler.postDelayed(
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

        resultRequestHandler = Handler()
        runnable = Runnable {

            getResults()

            resultRequestHandler.postDelayed(
                runnable, RESULT_CHECK_INTERVAL
            )
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
            val resultList = db.resultDao().getResults()

            adapter.setTestList(resultList)

            listView = findViewById(R.id.list_results)
            listView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

            listView.adapter = adapter

            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            monitorNetwork()
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

            if (!resultPollingStarted) {
                resultRequestHandler.postDelayed(
                    runnable, RESULT_CHECK_START_DELAY
                )
            }

            if (db.resultDao().getUnsent().isNotEmpty()) {
                if (!NetUtil.isInternetConnected(this)) {
                    notifyNoInternet()
                } else {
                    sendImagesToServer()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        resultPollingStarted = false
        resultRequestHandler.removeCallbacks(runnable)
    }

    fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent: Intent? = Intent(baseContext, BarcodeActivity::class.java)
        startActivityForResult(intent, 100)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            sendImagesToServer()
            resultRequestHandler.removeCallbacks(runnable)
            resultRequestHandler.postDelayed(
                runnable, RESULT_CHECK_START_DELAY
            )
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            resultRequestHandler.removeCallbacks(runnable)
        }
    }

    private fun monitorNetwork() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val builder = NetworkRequest.Builder()
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        resultPollingStarted = true
        resultRequestHandler.removeCallbacks(runnable)
        resultRequestHandler.postDelayed(
            runnable, RESULT_CHECK_INTERVAL
        )

        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {

                val id = data.getStringExtra(TEST_ID_KEY)
                if (id != null) {
                    AppDatabase.getDatabase(baseContext).resultDao().insert(
                        TestResult(
                            id, 0, TEST_PARAMETER_NAME,
                            Date().time, "", getString(R.string.outbox)
                        )
                    )
                }

                refreshList()

                if (sendDummyImage()) {
                    val toast = Toast.makeText(
                        this, getString(R.string.sending_dummy_image),
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
                    toast.show()
                }

                if (!NetUtil.isInternetConnected(this)) {
                    notifyNoInternet()
                } else {
                    sendImagesToServer()
                }
            }
        }
    }

    private fun notifyNoInternet() {
        val toast = Toast.makeText(
            this, getString(R.string.no_Internet_connection),
            Toast.LENGTH_LONG
        )
        toast.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
        toast.show()
    }

    private fun notifyFileSent() {

        refreshList()

        Handler().postDelayed({
            for (i in 0 until 2) {
                val toast = Toast.makeText(
                    this, getString(R.string.analyzing) +
                            "\n\n" +
                            getString(R.string.wait_few_minutes) +
                            "\n",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
                toast.show()
            }
        }, 800)

        sendImagesToServer()
    }

    private fun sendImagesToServer() {
        if (NetUtil.isInternetConnected(this)) {
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
                .addFormDataPart("image", filename, fileBody)
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build()

            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Timber.d(e, "Upload Failed!")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.body!!.close()

                    db.resultDao().updateStatus(id, 1, getString(R.string.analyzing))

                    this@ResultListActivity.runOnUiThread {
                        notifyFileSent()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getResults() {

        val resultList = db.resultDao().getResultPending()

        if (resultList.isNotEmpty()) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            resultList.forEach {
                val call = api.getResponse(it.id)

                Timber.e("Request result %s", it.id)

                call.enqueue(object : Callback<ResultResponse> {

                    override fun onResponse(
                        call: Call<ResultResponse>?,
                        response: Response<ResultResponse>?
                    ) {
                        val body = response?.body()

                        val id = body?.id
                        val title = body?.title
                        var result = ""
                        var message = "-"
                        if (body?.result != null) {
                            result = body.result?.replace(title.toString(), "").toString()
                            message = body.message.toString()
                        }

                        val resultData = db.resultDao().getResult(id)

                        if (resultData != null) {

                            SoundUtil.playShortResource(applicationContext, R.raw.triangle)

                            db.resultDao().updateResult(id.toString(), 2, message, result)

                            refreshList()

                            val toast = Toast.makeText(
                                applicationContext,
                                getString(R.string.result_received),
                                Toast.LENGTH_LONG
                            )
                            toast.setGravity(Gravity.BOTTOM, 0, TOAST_Y_OFFSET)
                            toast.show()
                        }
                    }

                    override fun onFailure(call: Call<ResultResponse>?, t: Throwable?) {
                        Timber.e(t.toString())
                        requestCount++
                    }
                })
            }
        }
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
