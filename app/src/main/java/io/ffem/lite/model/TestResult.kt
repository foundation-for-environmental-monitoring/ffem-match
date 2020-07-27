package io.ffem.lite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity
@TypeConverters(ErrorTypeConverter::class)
data class TestResult(
    @PrimaryKey val id: String = "",
    @ColumnInfo val uuid: String,
    @ColumnInfo val status: Int,
    @ColumnInfo val name: String,
    @ColumnInfo val date: Long,
    @ColumnInfo val value: Double,
    @ColumnInfo val valueGrayscale: Double,
    @ColumnInfo val marginOfError: Double,
    @ColumnInfo val error: ErrorType,
    @ColumnInfo val testImageNumber: String
)