package io.ffem.lite.remote.dto

import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskValue

data class RiskDto(
    val risk: Int = 0,
    val sign: String = "",
    val value: Double = 0.0
) {
    fun toRiskValue(): RiskValue {
        return RiskValue(
            value = value,
            risk = RiskLevel.valueOf("RISK_$risk"),
            sign = sign
        )
    }
}