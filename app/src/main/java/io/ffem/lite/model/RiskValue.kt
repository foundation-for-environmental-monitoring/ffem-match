package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RiskValue(
    var value: Double = 0.0,
    var risk: RiskType? = null
) : Parcelable