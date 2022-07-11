package io.ffem.lite.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object Constants {

    const val APP_FOLDER = "ffem Lite"
    const val SURVEY_APP = "io.ffem.collect"

    val DECIMAL_FORMAT = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm"

    const val GET_READY_SECONDS = 12

    /**
     * The number of photo samples to be skipped during analysis.
     */
    const val SKIP_SAMPLING_COUNT = 2

    /**
     * The number of photos to take during analysis.
     */
    const val SAMPLING_COUNT_DEFAULT = 5

    const val SAMPLE_CROP_LENGTH_DEFAULT = 50

    /**
     * Max distance between colors at which the colors are considered to be similar.
     */
    const val MAX_COLOR_DISTANCE_RGB = 50

    /**
     * Max distance between colors for calibration.
     */
    const val MAX_COLOR_DISTANCE_CALIBRATION = 20

    /**
     * The number of interpolations to generate between range values.
     */
    const val INTERPOLATION_COUNT = 250.0

    const val SENSOR_INTERPOLATION_COUNT = 15

    const val MESSAGE_TWO_LINE_FORMAT = "%s%n%n%s"

    const val FLUORIDE_ID = "WB-HD-F"

    const val DEGREES_90 = 90
    const val DEGREES_270 = 270
    const val DEGREES_180 = 180

    /**
     * The delay before starting the test
     */
    const val DELAY_INITIAL = 2

    /**
     * The delay seconds between each photo taken by the camera during the analysis.
     */
    const val DELAY_BETWEEN_SAMPLING = 2

    const val SHORT_DELAY = 1

    const val MAX_TILT_PERCENTAGE_ALLOWED = 0.03
    const val MAX_CARD_COLOR_DISTANCE_CALIBRATION = 80
    const val DEFAULT_MINIMUM_BRIGHTNESS = 100
    const val DEFAULT_MAXIMUM_BRIGHTNESS = 250
    const val DEFAULT_SHADOW_TOLERANCE = 40
    const val MAX_DISTANCE = 999

    // Color card common settings
    const val CARD_DEFAULT_WIDTH = 575
    const val CARD_DEFAULT_HEIGHT = 500

    // Circle swatch card settings
    const val SWATCH_CIRCLE_SIZE_PERCENTAGE = 0.50
    const val SWATCH_RADIUS = 0.39

    // Circle cuvette area settings
    const val CIRCLE_CUVETTE_AREA_PERCENTAGE = 0.07
    const val CIRCLE_CUVETTE_Y_OFFSET = 5

    // Rectangle swatch card settings
    const val SWATCH_AREA_HEIGHT_PERCENTAGE = 0.23
    const val SWATCH_AREA_WIDTH_PERCENTAGE = 0.48
}