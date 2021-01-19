package io.ffem.lite.data

data class Result(
    val parameterId: String,
    val name: String,
    val risk: String,
    val result: String,
    val unit: String,
    val time: Long,
    val appVersion: String,
    val device: String
)
