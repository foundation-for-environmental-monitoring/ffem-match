package io.ffem.lite.model

import com.google.gson.annotations.SerializedName

enum class TestType {
    @SerializedName("all")
    ALL,

    @SerializedName("api")
    API,

    @SerializedName("card")
    CARD,

    @SerializedName("cuvette")
    CUVETTE,

    @SerializedName("titration")
    TITRATION,
}