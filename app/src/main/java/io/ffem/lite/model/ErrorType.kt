package io.ffem.lite.model

import androidx.room.TypeConverter
import io.ffem.lite.app.App
import java.util.*

enum class ErrorType {
    NO_ERROR,
    NO_MATCH,
    CALIBRATION_ERROR,
    BAD_LIGHTING,
    IMAGE_TILTED,
    INVALID_BARCODE,
    WRONG_CARD
}

fun ErrorType.toLocalString(): String {
    val resourceId =
        App.app.resources.getIdentifier(
            this.toString().lowercase(Locale.getDefault()),
            "string", App.app.packageName
        )
    return App.app.getString(resourceId)
}

class ErrorTypeConverter {
    @TypeConverter
    fun toErrorType(value: Int) = enumValues<ErrorType>()[value]

    @TypeConverter
    fun fromErrorType(value: ErrorType) = value.ordinal
}
