package io.ffem.lite.remote.dto

import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.Result

data class ResultDto(
    val id: Int = 0,
    val minMarginError: Double = 0.0,
    val risks: List<RiskDto> = emptyList(),
    val unit: String = "",
    val values: List<ValueDto> = emptyList()
) {
    fun toResult(): Result {
        return Result(
            id = id,
            minMarginError = minMarginError,
            unit = unit,
            values = values.map { it.toCalibrationValue() } as MutableList<CalibrationValue>
        )
    }
}