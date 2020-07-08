package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ResultInfo(
    var result: Double = -1.0,
    var distance: Double = 0.0,
    var calibrationDistance: Double = 0.0,
    val color: Int = 0,
    var matchedColor: Int = 0,
    var calibrationColor: Int = 0,
    var calibration: Calibration = Calibration(),
    var swatches: ArrayList<Swatch>? = ArrayList()
) : Parcelable