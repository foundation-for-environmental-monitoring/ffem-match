package io.ffem.lite.remote.dto

import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.Result
import io.ffem.lite.model.RiskValue

data class ResultDto(
    val id: Int = 0,
    val minMarginError: Double = 0.0,
    val risks: List<RiskDto> = emptyList(),
    val unit: String = "",
    val ranges: String = "",
    val rangeMin: String = "",
    val values: List<ValueDto> = emptyList()
) {
    fun toResult(): Result {
        return Result(
            id = id,
            ranges = ranges,
            rangeMin = rangeMin,
            minMarginError = minMarginError,
            unit = unit,
            values = values.map { it.toCalibrationValue() } as MutableList<CalibrationValue>,
            risks = risks.map { it.toRiskValue() } as MutableList<RiskValue>
        )
    }
}