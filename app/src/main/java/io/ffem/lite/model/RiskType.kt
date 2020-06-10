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

fun RiskType.toResourceId(context: Context, riskAsQty: Boolean): Int {
    var riskTypeString = toString().toLowerCase(Locale.ROOT)

    if (riskAsQty) {
        riskTypeString = toString().toLowerCase(Locale.ROOT) + "_qty"
    }

    return context.resources.getIdentifier(
        riskTypeString, "string", context.packageName
    )
}
