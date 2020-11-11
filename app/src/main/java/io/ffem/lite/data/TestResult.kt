package io.ffem.lite.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.ErrorTypeConverter

@Entity
@TypeConverters(ErrorTypeConverter::class)
data class TestResult(
    @PrimaryKey val id: String = "",
    val uuid: String,
    val status: Int,
    val name: String,
    val date: Long,
    val value: Double,
    val valueGrayscale: Double,
    val marginOfError: Double,
    val luminosity: Int = -1,
    val error: ErrorType,
    val testImageNumber: Int = -1
)