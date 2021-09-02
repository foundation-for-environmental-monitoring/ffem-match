package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
data class TestInfo(
    var name: String? = null,
    var sampleType: String = "",
    var subtype: TestType? = null,
    var uuid: String? = null,
    var results: ArrayList<Result> = ArrayList(),
    var sampleQuantity: Float = 0f,
    var dilution: Int = 1,
    val dilutions: List<Int> = java.util.ArrayList(),
    var monthsValid: Int = 0,
    var instructions: List<Instruction>? = java.util.ArrayList(),
    var fileName: String = UUID.randomUUID().toString()
) : Parcelable {
    fun subTest(): Result {
        return results[0]
    }
}