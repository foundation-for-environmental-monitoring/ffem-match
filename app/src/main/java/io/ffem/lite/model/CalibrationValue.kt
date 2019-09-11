package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

class CalibrationValue {
    @SerializedName("value")
    var value: Float = 0.0f
    @SerializedName("x")
    var x: Int = 0
    @SerializedName("y")
    var y: Int = 0

    var color: Int = 0
}