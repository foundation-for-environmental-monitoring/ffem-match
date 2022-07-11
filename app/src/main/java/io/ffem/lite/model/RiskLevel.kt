package io.ffem.lite.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import java.util.*

enum class RiskLevel {
    @SerializedName("0")
    RISK_0,

    @SerializedName("1")
    RISK_1,

    @SerializedName("2")
    RISK_2,

    @SerializedName("3")
    RISK_3,

    @SerializedName("4")
    RISK_4,

    @SerializedName("5")
    RISK_5,

    @SerializedName("6")
    RISK_6
}

fun RiskLevel.toResourceId(context: Context, riskType: RiskType): Int {
    return context.resources.getIdentifier(
        if (riskType == RiskType.NORMAL) toString().lowercase(Locale.getDefault())
        else (toString() + "_" + riskType.name).lowercase(Locale.getDefault()),
        "string", context.packageName
    )
}
