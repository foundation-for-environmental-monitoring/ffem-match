package io.ffem.lite.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class Calibration(
    @PrimaryKey val id: String = "",
    @ColumnInfo val value: Double = 0.0,
    @ColumnInfo val rDiff: Int = 0,
    @ColumnInfo val gDiff: Int = 0,
    @ColumnInfo val bDiff: Int = 0
) : Parcelable