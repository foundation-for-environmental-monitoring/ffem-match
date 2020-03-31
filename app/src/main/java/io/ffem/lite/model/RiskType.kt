package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class RiskType {
    @SerializedName("0")
    LOW,

    @SerializedName("1")
    MEDIUM,

    @SerializedName("2")
    HIGH;
}
