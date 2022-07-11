package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

enum class TestSampleType {
    @SerializedName("all")
    ALL,

    @SerializedName("compost")
    COMPOST,

    @SerializedName("irrigation")
    IRRIGATION,

    @SerializedName("soil")
    SOIL,

    @SerializedName("water")
    WATER
}