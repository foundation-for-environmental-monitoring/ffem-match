package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

enum class RiskType {
    NORMAL,

    @SerializedName("quantity")
    QUANTITY,

    @SerializedName("alkalinity")
    ALKALINITY,

    @SerializedName("safety")
    SAFETY;
}
