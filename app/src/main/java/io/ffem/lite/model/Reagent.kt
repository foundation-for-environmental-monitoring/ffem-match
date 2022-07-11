package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reagent(
    var name: String? = "",
    var code: String? = "",
    var reactionTime: Int? = null
) : Parcelable