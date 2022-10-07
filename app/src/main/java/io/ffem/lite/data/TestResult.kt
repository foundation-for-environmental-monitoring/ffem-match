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
    val sampleType: String,
    val date: Long,
    val value: Double,
    val maxValue: Double,
    val marginOfError: Double,
    val luminosity: Int = -1,
    val error: ErrorType,
    val testImageNumber: Int = -1,
    var email: String = "",
    var latitude: Double? = Double.NaN,
    var longitude: Double? = Double.NaN,
    var geoAccuracy: Float? = Float.NaN,
    var comment: String? = null
)