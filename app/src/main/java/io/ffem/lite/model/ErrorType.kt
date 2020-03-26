package io.ffem.lite.model

import androidx.room.TypeConverter

// Should match error_array in string resources
enum class ErrorType {
    NO_ERROR,
    NO_MATCH,
    CALIBRATION_ERROR,
    BAD_LIGHTING,
    IMAGE_TILTED,
    INVALID_BARCODE
}

class ErrorTypeConverter {
    @TypeConverter
    fun toErrorType(value: Int) = enumValues<ErrorType>()[value]

    @TypeConverter
    fun fromErrorType(value: ErrorType) = value.ordinal
}
