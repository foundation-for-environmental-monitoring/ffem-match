package io.ffem.lite.remote.dto

import io.ffem.lite.model.*

data class ParameterInfoDto(
    val dilutions: List<Int> = emptyList(),
    val name: String = "",
    val inputs: ArrayList<InputDto> = arrayListOf(),
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
            results = results.map { it.toResult() } as ArrayList<Result>,
            inputs = inputs.map { it.toInput() } as ArrayList<Input>
        )
    }
}