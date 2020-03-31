package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CalibrationValue(
    var value: Float = 0.0f,
    var color: Int = 0,
    var risk: RiskType? = null
) : Parcelable