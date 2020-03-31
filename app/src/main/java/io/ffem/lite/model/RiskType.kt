package io.ffem.lite.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import java.util.*

@Suppress("unused")
enum class RiskType {
    @SerializedName("0")
    LOW,

    @SerializedName("1")
    MEDIUM,

    @SerializedName("2")
    HIGH;
}

fun RiskType.toLocalString(context: Context): String {
    val resourceId =
        context.resources.getIdentifier(
            this.toString().toLowerCase(Locale.ROOT),
            "string", context.packageName
        )
    return context.getString(resourceId)
}