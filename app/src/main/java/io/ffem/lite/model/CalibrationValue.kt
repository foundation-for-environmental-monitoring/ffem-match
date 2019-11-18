package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

class CalibrationValue {
    @SerializedName("value")
    var value: Float = 0.0f

    var color: Int = 0
}