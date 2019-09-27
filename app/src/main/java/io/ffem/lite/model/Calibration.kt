package io.ffem.lite.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Calibration {
    @SerializedName("width")
    @Expose
    var width: Int = 0
    @SerializedName("height")
    @Expose
    var height: Int = 0
    @SerializedName("values")
    @Expose
    var values: List<CalibrationValue> = ArrayList()
}