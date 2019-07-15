package io.ffem.lite.remote

import io.ffem.lite.model.ResultResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("result")
    fun getResponse(@Query("id") testId: String): Call<ResultResponse>

}