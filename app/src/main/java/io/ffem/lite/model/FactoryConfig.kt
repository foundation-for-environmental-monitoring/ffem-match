package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FactoryConfig(
    var uuid: String = "",
    var calibration: List<PulseWidth> = ArrayList()
) : Parcelable