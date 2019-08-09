package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

class ResultResponse {
    @SerializedName("TestRunId")
    var id: String? = null
    @SerializedName("result")
    var result: String? = null
    @SerializedName("title")
    var title: String? = null
    @SerializedName("message")
    var message: String? = null
}