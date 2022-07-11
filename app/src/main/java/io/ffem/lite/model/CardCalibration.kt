package io.ffem.lite.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class CardCalibration(
    @PrimaryKey var id: String = "",
    var value: Double = 0.0,
    var color: Int = 0,
    var rDiff: Int = 0,
    var gDiff: Int = 0,
    var bDiff: Int = 0,
    var date: Long = 0
) : Parcelable