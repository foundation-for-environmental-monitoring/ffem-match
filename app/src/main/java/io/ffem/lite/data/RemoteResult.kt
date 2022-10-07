package io.ffem.lite.data

data class RemoteResult(
    val parameterId: String,
    val name: String?,
    val sampleType: String,
    val testType: String,
    val risk: String,
    val result: String,
    val unit: String?,
    val time: Long,
    val email: String,
    val latitude: Double?,
    val longitude: Double?,
    val geoAccuracy: Float?,
    val comment: String?,
    val appVersion: String,
    val device: String,
    val androidVersion: String
)
