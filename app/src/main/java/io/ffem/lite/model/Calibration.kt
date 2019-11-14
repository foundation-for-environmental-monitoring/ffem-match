package io.ffem.lite.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Calibration {
    @SerializedName("values")
    @Expose
    var values: List<CalibrationValue> = ArrayList()
}