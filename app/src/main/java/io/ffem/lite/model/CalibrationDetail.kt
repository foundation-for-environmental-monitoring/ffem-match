package io.ffem.lite.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(indices = [Index(value = ["calibrationId", "testId"], unique = true)])
data class CalibrationDetail(
    @PrimaryKey(autoGenerate = true)
    var calibrationId: Long = 0,
    var testId: String = "",
    var date: Long = 0,
    var expiry: Long = 0,
    var name: String = "",
    var desc: String = "",
    var cuvetteType: String? = null,
    var isCurrent: Boolean = false,
    var colors: String = ""
) : Parcelable