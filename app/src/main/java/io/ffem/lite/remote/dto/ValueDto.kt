package io.ffem.lite.remote.dto

import io.ffem.lite.model.CalibrationValue

data class ValueDto(
    val calibrate: Boolean = false,
    val color: String = "",
    val value: Double = 0.0,
    val range: String = ""
) {
    fun toCalibrationValue(): CalibrationValue {
        return CalibrationValue(
            calibrate = calibrate,
            value = value,
            range = range
        )
    }
}