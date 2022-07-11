package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RiskValue(
    var value: Double = 0.0,
    var risk: RiskLevel? = null,
    var safety: SafetyLevel? = null,
    var sign: String = ""
) : Parcelable