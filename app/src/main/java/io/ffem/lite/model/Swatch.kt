package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Swatch(var value: Double, var color: Int, var distance: Double = 0.0) : Parcelable