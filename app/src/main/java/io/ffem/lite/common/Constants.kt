package io.ffem.lite.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object Constants {

    val DECIMAL_FORMAT = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

    const val MAX_TILT_PERCENTAGE_ALLOWED = 0.03
    const val MAX_COLOR_DISTANCE_RGB = 55
    const val MAX_COLOR_DISTANCE_CALIBRATION = 80
    const val DEFAULT_MINIMUM_BRIGHTNESS = 100
    const val DEFAULT_MAXIMUM_BRIGHTNESS = 240
    const val DEFAULT_SHADOW_TOLERANCE = 40
    const val INTERPOLATION_COUNT = 100.0
    const val MAX_DISTANCE = 999

    // Color card common settings
    const val CARD_DEFAULT_WIDTH = 575
    const val CARD_DEFAULT_HEIGHT = 500

    // Rectangle swatch card settings
    const val SWATCH_AREA_HEIGHT_PERCENTAGE = 0.23
    const val SWATCH_AREA_WIDTH_PERCENTAGE = 0.48
}