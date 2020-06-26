package io.ffem.lite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Calibration(
    @PrimaryKey val id: String = "",
    @ColumnInfo val red: Int,
    @ColumnInfo val green: Int,
    @ColumnInfo val blue: Int
)