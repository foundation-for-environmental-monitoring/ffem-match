package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Input(
    var id: Int = 0,
    var name: String? = null
) : Parcelable