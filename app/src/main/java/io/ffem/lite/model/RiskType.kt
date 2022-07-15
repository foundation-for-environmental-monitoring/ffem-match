package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

enum class RiskType {
    NORMAL,

    @SerializedName("L3")
    L3,

    @SerializedName("quantity")
    QUANTITY,

    @SerializedName("alkalinity")
    ALKALINITY,

    @SerializedName("safety")
    SAFETY;
}
