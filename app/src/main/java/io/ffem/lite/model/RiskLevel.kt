package io.ffem.lite.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import java.util.*

enum class RiskLevel {
    @SerializedName("0")
    LOW,

    @SerializedName("1")
    MEDIUM,

    @SerializedName("2")
    HIGH;
}

fun RiskLevel.toResourceId(context: Context, riskType: RiskType): Int {
    return context.resources.getIdentifier(
        if (riskType == RiskType.NORMAL) toString().lowercase(Locale.getDefault())
        else (toString() + "_" + riskType.name).lowercase(Locale.getDefault()),
        "string", context.packageName
    )
}
