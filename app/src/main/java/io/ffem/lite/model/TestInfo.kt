package io.ffem.lite.model

import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TestInfo(
    var name: String? = null,
    var type: String? = null,
    var uuid: String? = null,
    var unit: String? = null,
    var values: List<CalibrationValue> = ArrayList(),
    var result: Double = -1.0,
    var error: ErrorType = ErrorType.NO_ERROR,
    var fileName: String = ""
) : Parcelable {

    fun getResultString(context: Context): String {
        return if (result < 0) {
            error.toLocalString(context)
        } else {
            result.toString()
        }
    }

    fun getRisk(context: Context): String {
        var riskValue = RiskType.LOW
        val count = values.size / 2

        // Evaluate the risk level based on the result
        if (result >= values[count].value + 1) {
            riskValue = RiskType.HIGH
        } else {
            for (i in 0 until count) {
                val calibrationValue = values[i]
                if (result >= calibrationValue.value) {
                    if (calibrationValue.risk != null) {
                        riskValue = calibrationValue.risk!!
                    }
                } else {
                    break
                }
            }
        }

        return riskValue.toLocalString(context)
    }
}