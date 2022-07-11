package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PulseWidth(
    var value: Double = 0.0,
    var a: List<Int> = ArrayList()
) : Parcelable