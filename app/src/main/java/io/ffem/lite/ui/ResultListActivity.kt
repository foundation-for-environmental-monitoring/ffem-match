package io.ffem.lite.ui

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.API_URL
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_NAME_KEY
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.camera.Utilities
import io.ffem.lite.camera.Utilities.bitmapToBytes
import io.ffem.lite.model.ResultResponse
import io.ffem.lite.model.TestResult
import io.ffem.lite.preference.AppPreferences.sendDummyImage
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.remote.ApiService
import io.ffem.lite.util.NetUtil
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


class ResultListActivity : BaseActivity() {

    private var callBackStarted: Boolean = false
    private lateinit var listView: RecyclerView

    private fun onResultClick(position: Int) {
        resultRequestHandler.postDelayed(
            {
                val item = adapter.getItemAt(position)
                val intent = Intent(baseContext, ResultActivity::class.java)
                intent.putExtra(TEST_ID_KEY, item.id)
                intent.putExtra(TEST_NAME_KEY, item.name)
                startActivity(intent)
            }, 300
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

        resultRequestHandler = Handler()

        runnable = Runnable {

            getResult()

            resultRequestHandler.postDelayed(
                runnable, 30000
            )
        }

        setTitle(R.string.app_name)

        db = AppDatabase.getDatabase(baseContext)
        val resultList = db.resultDao().getResults()

        adapter.setTestList(resultList)

        listView = findViewById(R.id.list_results)
        listView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        listView.adapter = adapter
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {
        super.onResume()

        if (!callBackStarted) {
            resultRequestHandler.postDelayed(
                runnable, 3000
            )
        }
    }

    override fun onPause() {
        super.onPause()
        callBackStarted = false
        resultRequestHandler.removeCallbacks(runnable)
    }

    fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (NetUtil.isInternetConnected(this)) {
            if (sendDummyImage()) {

                for (i in 0 until 2) {
                    val toast = Toast.makeText(
                        this, getString(R.string.sending_dummy_image) +
                                "\n\n" +
                                getString(R.string.wait_few_minutes) +
                                "\n",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 100)
                    toast.show()
                }

                val drawable = ContextCompat.getDrawable(this, R.drawable.dummy_card)
                val bitmap = (drawable as BitmapDrawable).bitmap

                val testId = UUID.randomUUID().toString()
                val filePath = Utilities.savePicture(
                    getString(R.string.app_name), testId,
                    "Fluoride", bitmapToBytes(bitmap)
                )
                sendToServer(testId, "Fluoride", filePath)
            } else {
                val intent: Intent? = Intent(baseContext, BarcodeActivity::class.java)
                startActivityForResult(intent, 100)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callBackStarted = true
        resultRequestHandler.removeCallbacks(runnable)
        resultRequestHandler.postDelayed(
            runnable, 30000
        )

        if (resultCode == Activity.RESULT_OK) {
            adapter.setTestList(db.resultDao().getResults())
            adapter.notifyDataSetChanged()

            Handler().postDelayed({
                for (i in 0 until 2) {
                    val toast = Toast.makeText(
                        this, getString(R.string.analyzing) +
                                "\n\n" +
                                getString(R.string.wait_few_minutes) +
                                "\n",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 100)
                    toast.show()
                }
            }, 1000)
        }
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
            okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Timber.d(e, "Upload Failed!")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.body!!.close()

                    val date = Date()
                    val db = AppDatabase.getDatabase(baseContext)
                    db.resultDao().insert(
                        TestResult(
                            testId, barcodeValue, date.time,
                            "", getString(R.string.analyzing)
                        )
                    )

                    this@ResultListActivity.runOnUiThread {
                        adapter.setTestList(db.resultDao().getResults())
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getResult() {

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val resultList = db.resultDao().getResults()
        resultList.forEach {
            if (it.message == getString(R.string.analyzing)) {
                val call = api.getResponse(it.id)

                Timber.e("Request result %s", it.id)

                call.enqueue(object : Callback<ResultResponse> {

                    override fun onResponse(call: Call<ResultResponse>?, response: Response<ResultResponse>?) {
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

                            db.resultDao()
                                .insert(TestResult(id.toString(), title.toString(), resultData.date, result, message))

                            adapter.setTestList(db.resultDao().getResults())
                            adapter.notifyDataSetChanged()

                            val toast = Toast.makeText(
                                applicationContext,
                                getString(R.string.result_received),
                                Toast.LENGTH_LONG
                            )
                            toast.setGravity(Gravity.BOTTOM, 0, 250)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val intent = Intent(baseContext, SettingsActivity::class.java)
        startActivity(intent)
    }
}
