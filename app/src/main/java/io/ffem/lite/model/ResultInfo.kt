package io.ffem.lite.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResultInfo(

    var result: Double = -1.0,
    val sampleColor: Int = 0,

    // the swatch that matches the sample color
    var matchedSwatch: Int = 0,

    // position of swatch that matches
    var matchedPosition: Float = 0f,

    // distance between sampleColor and matchedColor
    var distance: Double = 0.0,

    var swatches: ArrayList<ColorInfo>? = ArrayList(),

    // for calculation of margin of error
    var swatchDistance: Double = 0.0,

    var calibrationSteps: Int = 0,

    var dilution: Int = 1,

    var sampleBitmap: Bitmap? = null,
    var luminosity: Int = -1,
    var highLevelsFound: Boolean = false,

    // the value chosen for calibration by the user
    var calibratedValue: CalibrationValue = CalibrationValue(),

    // color difference on calibration
    var calibration: CardCalibration = CardCalibration()
) : Parcelable