package io.ffem.lite.model

import android.content.Context
import androidx.room.TypeConverter
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

fun ErrorType.toLocalString(context: Context): String {
    val resourceId =
        context.resources.getIdentifier(
            this.toString().toLowerCase(Locale.ROOT),
            "string", context.packageName
        )
    return context.getString(resourceId)
}

class ErrorTypeConverter {
    @TypeConverter
    fun toErrorType(value: Int) = enumValues<ErrorType>()[value]

    @TypeConverter
    fun fromErrorType(value: ErrorType) = value.ordinal
}
