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
import io.ffem.lite.app.App.Companion.API_URL
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.model.ResultResponse
import io.ffem.lite.model.TestResult
import io.ffem.lite.preference.SettingsActivity
import io.ffem.lite.remote.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class ResultListActivity : BaseActivity() {

    private var callBackStarted: Boolean = false
    private lateinit var listView: RecyclerView
    private var adapter: ResultAdapter = ResultAdapter()
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

    fun onStartClicked(@Suppress("UNUSED_PARAMETER") view: View) {
//        if (NetUtil.isInternetConnected(this)) {
        val intent: Intent? = Intent(baseContext, BarcodeActivity::class.java)
        startActivityForResult(intent, 100)
//    }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val resultList = db.resultDao().getResults()

        adapter.setTestList(resultList)
        adapter.notifyDataSetChanged()

        callBackStarted = true
        resultRequestHandler.removeCallbacks(runnable)
        resultRequestHandler.postDelayed(
            runnable, 30000
        )
    }

    private fun getResult() {

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val resultList = db.resultDao().getResults()
        resultList.forEach {
            if (it.message == "Analysing") {
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
