package io.ffem.lite.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TestConfig {
    @SerializedName("tests")
    @Expose
    var tests: List<TestInfo> = ArrayList()
}