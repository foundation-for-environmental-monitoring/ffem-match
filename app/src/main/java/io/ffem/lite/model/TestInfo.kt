package io.ffem.lite.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TestInfo {
    @SerializedName("name")
    @Expose
    var name: String? = null
    @SerializedName("type")
    @Expose
    var type: String? = null
    @SerializedName("uuid")
    @Expose
    var uuid: String? = null
    @SerializedName("values")
    @Expose
    var values: List<CalibrationValue> = ArrayList()
}