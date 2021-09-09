package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CalibrationValue(
    var value: Double = 0.0,
    var color: Int = 0,
    var calibrate: Boolean = false,
) : Parcelable