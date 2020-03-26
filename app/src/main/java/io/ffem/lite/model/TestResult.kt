package io.ffem.lite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "results")
@TypeConverters(ErrorTypeConverter::class)
data class TestResult(
    @PrimaryKey val id: String = "",
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "error") val error: ErrorType,
    @ColumnInfo(name = "testImageNumber") val testImageNumber: String
)