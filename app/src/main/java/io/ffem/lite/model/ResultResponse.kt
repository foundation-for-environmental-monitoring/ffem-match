package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

class ResultResponse {
    private val date: String? = null
    @SerializedName("result")
    var result: String? = null
    @SerializedName("title")
    var title: String? = null
}