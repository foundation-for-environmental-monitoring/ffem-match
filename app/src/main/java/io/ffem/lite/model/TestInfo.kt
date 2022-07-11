package io.ffem.lite.model

import android.os.Parcelable
import io.ffem.lite.common.Constants.DECIMAL_FORMAT
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.math.max

@Parcelize
data class TestInfo(
    var name: String? = null,
    var sampleType: TestSampleType = TestSampleType.WATER,
    var subtype: TestType = TestType.CARD,
    var uuid: String = "",
    var results: ArrayList<Result> = ArrayList(),
    var sampleQuantity: Float = 0f,
    var dilution: Int = 1,
    val dilutions: List<Int> = ArrayList(),
    val reagents: List<Reagent> = ArrayList(),
    var instructions: List<Instruction>? = ArrayList(),
    var monthsValid: Int = 0,
    var fileName: String = UUID.randomUUID().toString()
) : Parcelable {
    fun subTest(): Result {
        return results[0]
    }

    val minMaxRange: String
        get() {
            if (results.isNotEmpty()) {
                val minMaxRange = StringBuilder()
                for (result in results) {
                    if (result.colors.size > 0) {
                        val valueCount = result.colors.size
                        if (minMaxRange.isNotEmpty()) {
                            minMaxRange.append(", ")
                        }
                        if (result.colors.size > 0) {
                            minMaxRange.append(
                                String.format(
                                    Locale.US, "%s - %s",
                                    DECIMAL_FORMAT.format(result.colors[0].value),
                                    DECIMAL_FORMAT.format(result.colors[valueCount - 1].value)
                                )
                            )
                        }
                    } else {
                        val rangeArray = subTest().ranges!!.split(",").toTypedArray()
                        if (rangeArray.size > 1) {
                            if (minMaxRange.isNotEmpty()) {
                                minMaxRange.append(", ")
                            }
                            val maxRangeValue = result.calculateResult(getMaxRangeValue())
                            minMaxRange.append(rangeArray[0].trim { it <= ' ' }).append(" - ")
                                .append(DECIMAL_FORMAT.format(maxRangeValue))
                            minMaxRange.append(" ")
                            minMaxRange.append(result.unit)
                        }
                    }
                }
                if (dilutions.size > 1) {
                    val maxDilution = getMaxDilution()
                    val result = subTest()
                    val maxRangeValue = result.calculateResult(getMaxRangeValue())
                    val text: String = if (dilutions.size > 2) {
                        String.format(
                            Locale.US, " (<dilutionRange>%s+</dilutionRange>)",
                            DECIMAL_FORMAT.format(maxDilution * maxRangeValue)
                        )
                    } else {
                        String.format(
                            " (<dilutionRange>%s</dilutionRange>)",
                            DECIMAL_FORMAT.format(maxDilution * maxRangeValue)
                        )
                    }
                    return minMaxRange.toString() + text
                }
                return String.format("%s", minMaxRange.toString())
            }
            return ""
        }

    private fun getMaxRangeValue(): Double {
        return try {
            val array = subTest().ranges!!.split(",").toTypedArray()
            array[array.size - 1].toDouble()
        } catch (e: NumberFormatException) {
            (-1).toDouble()
        }
    }

    fun getMaxDilution(): Int {
        return if (dilutions.isEmpty()) {
            1
        } else {
            max(dilutions[dilutions.size - 1], dilutions[dilutions.size - 2])
        }
    }

    fun getReagent(i: Int): Reagent {
        return if (reagents.size > i) {
            reagents[i]
        } else {
            Reagent()
        }
    }
}