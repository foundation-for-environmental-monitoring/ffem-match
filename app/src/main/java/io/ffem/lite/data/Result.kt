package io.ffem.lite.data

data class Result(
    val parameterId: String,
    val name: String?,
    val sampleType: String,
    val risk: String,
    val result: String,
    val unit: String?,
    val time: Long,
    val source: String,
    val sourceType: String,
    val latitude: Double?,
    val longitude: Double?,
    val geoAccuracy: Float?,
    val comment: String?,
    val appVersion: String,
    val device: String
)
