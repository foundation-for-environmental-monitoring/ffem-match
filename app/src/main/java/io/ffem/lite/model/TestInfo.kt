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
    var riskAsQty: Boolean = false,
    var risks: List<RiskValue> = ArrayList(),
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
        var riskType = RiskType.HIGH

        // Evaluate the risk level based on the result
        for (element in risks) {
            if (result >= element.value) {
                if (element.risk != null) {
                    riskType = element.risk!!
                }
            } else {
                break
            }
        }

        return if (riskAsQty) {
            riskType.toQuantityLocalString(context)
        } else {
            riskType.toLocalString(context)
        }
    }
}