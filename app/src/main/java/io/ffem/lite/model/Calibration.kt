package io.ffem.lite.model

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(primaryKeys = ["calibrationId", "value"])
data class Calibration(
    var calibrationId: Long = 0,
    var value: Double = 0.0,
    var color: Int = 0,
    var rDiff: Int = 0,
    var gDiff: Int = 0,
    var bDiff: Int = 0,
    var date: Long = 0
) : Parcelable