package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

enum class SafetyLevel {
    @SerializedName("0")
    ACCEPTABLE,

    @SerializedName("1")
    PERMISSIBLE,

    @SerializedName("2")
    UNSAFE
}
