package io.ffem.lite.model

import android.os.Parcelable
import io.ffem.lite.data.Calibration
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResultInfo(
    var result: Double = -1.0,
    var luminosity: Int = -1,

    val sampleColor: Int = 0,

    // the swatch that matches the sample color
    var matchedSwatch: Int = 0,

    // position of swatch that matches
    var matchedPosition: Float = 0f,

    // distance between sampleColor and matchedColor
    var distance: Double = 0.0,

    // the value chosen for calibration by the user
    var calibratedValue: CalibrationValue = CalibrationValue(),

    // color difference on calibration
    var calibration: Calibration = Calibration(),

    // swatches extracted from the color columns on the card
    var swatches: ArrayList<Swatch>? = ArrayList(),

    // for calculation of margin of error
    var swatchDistance: Double = 0.0
) : Parcelable