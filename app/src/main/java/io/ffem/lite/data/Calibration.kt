package io.ffem.lite.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Calibration(
    @PrimaryKey val id: String = "",
    val value: Double = 0.0,
    val rDiff: Int = 0,
    val gDiff: Int = 0,
    val bDiff: Int = 0
) : Parcelable