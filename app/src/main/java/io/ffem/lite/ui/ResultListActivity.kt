package io.ffem.lite.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.barcode.BarcodeCaptureActivity
import io.ffem.lite.model.ResultResponse
import io.ffem.lite.model.TestResult
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.remote.ApiService
import io.ffem.lite.util.NetUtil
import io.ffem.lite.util.PreferencesUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class ResultListActivity : BaseActivity() {

    companion object {
        private const val BASE_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000/"
    }

    private lateinit var listView: RecyclerView
    private var adapter: ResultAdapter = ResultAdapter()
    private var requestCount = 0
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_list)

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
        Handler().postDelayed({
            getResult()
        }, 50)
    }

    fun onStartClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (NetUtil.isInternetConnected(this)) {
            val intent: Intent? = Intent(baseContext, BarcodeCaptureActivity::class.java)
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val resultList = db.resultDao().getResults()

        adapter.setTestList(resultList)
        adapter.notifyDataSetChanged()
    }

    private fun getResult() {

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val testId = PreferencesUtil.getString(this, "testRunId", "")

        val call = api.getResponse(testId.toString())
        call.enqueue(object : Callback<ResultResponse> {

            override fun onResponse(call: Call<ResultResponse>?, response: Response<ResultResponse>?) {
                val body = response?.body()

                val id = body?.id
                val title = body?.title
                var result = ""
                var message = "-"
                if (body?.result == null) {
                    if (requestCount < 15) {
                        Handler().postDelayed({
                            getResult()
                        }, 5000)
                    }
                } else {
                    result = body.result?.replace(title.toString(), "").toString()
                    message = body.message.toString()
                }

                val resultData = db.resultDao().getResult(id)

                if (resultData != null) {

                    db.resultDao().insert(TestResult(id.toString(), title.toString(), resultData.date, result, message))

                    val resultList = db.resultDao().getResults()
                    adapter.setTestList(resultList)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<ResultResponse>?, t: Throwable?) {
                Timber.e(t.toString())
                requestCount++
                if (requestCount < 15) {
                    Handler().postDelayed({
                        getResult()
                    }, 5000)
                }
            }
        })
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
