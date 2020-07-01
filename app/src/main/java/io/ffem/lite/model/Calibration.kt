package io.ffem.lite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Calibration(
    @PrimaryKey val id: String = "",
    @ColumnInfo val rDiff: Int,
    @ColumnInfo val gDiff: Int,
    @ColumnInfo val bDiff: Int
)