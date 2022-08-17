package io.ffem.lite.remote.dto

import io.ffem.lite.model.Result
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.TestSampleType
import io.ffem.lite.model.TestType

data class ParameterInfoDto(
    val dilutions: List<Int> = emptyList(),
    val name: String = "",
    val results: ArrayList<ResultDto> = arrayListOf(),
    val sampleType: String = "",
    val subtype: String = "",
    val uuid: String = ""
) {
    fun toTestInfo(): TestInfo {
        return TestInfo(
            sampleType = TestSampleType.valueOf(sampleType.uppercase()),
            subtype = TestType.valueOf(subtype.uppercase()),
            dilutions = dilutions,
            name = name,
            uuid = uuid,
            results = results.map { it.toResult() } as ArrayList<Result>
        )
    }
}