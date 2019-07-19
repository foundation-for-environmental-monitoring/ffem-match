package io.ffem.lite.ui

import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.model.ResultResponse
import io.ffem.lite.model.TestResult
import io.ffem.lite.remote.ApiService
import io.ffem.lite.util.PreferencesUtil
import kotlinx.android.synthetic.main.fragment_result.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

/**
 * A placeholder fragment containing a simple view.
 */
class ResultFragment : Fragment() {

    companion object {
        private const val BASE_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000/"
    }

    private lateinit var progressDialog: ProgressDialog
    private var requestCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onResume() {
        super.onResume()

        progressDialog = ProgressDialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog)

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.isIndeterminate = true
        progressDialog.setTitle("Processing...")
        progressDialog.setMessage("Please wait. This could take a few minutes...")
        progressDialog.setCancelable(false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && progressDialog.window != null) {
            progressDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        progressDialog.show()

        Handler().postDelayed({
            getResult()
        }, 40000)
    }

    private fun getResult() {

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val testId = PreferencesUtil.getString(activity!!, "testRunId", "")

        val call = api.getResponse(testId.toString())
        call.enqueue(object : Callback<ResultResponse> {

            override fun onResponse(call: Call<ResultResponse>?, response: Response<ResultResponse>?) {
                val message = response?.body()
                progressDialog.dismiss()

                val id = message?.id
                val title = message?.title
                val result = message?.result?.replace(title.toString(), "")
                textTitle.text = title
                textResult.text = result

                val db = AppDatabase.getDatabase(context!!)

                val resultData = db.resultDao().getResult(id)

                db.resultDao().insert(TestResult(id.toString(), title.toString(), resultData.date, result!!))
            }

            override fun onFailure(call: Call<ResultResponse>?, t: Throwable?) {
                Timber.e(t.toString())
                requestCount++
                if (requestCount < 7) {
                    Handler().postDelayed({
                        getResult()
                    }, 10000)
                } else {
                    progressDialog.dismiss()
                }
            }
        })
    }
}
