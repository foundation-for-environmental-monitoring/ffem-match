package io.ffem.lite.model

import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlin.math.max
import kotlin.math.round

@Parcelize
data class TestInfo(
    var name: String? = null,
    var type: String? = null,
    var uuid: String? = null,
    var unit: String? = null,
    var riskAsQty: Boolean = false,
    var minMarginError: Double = 0.0,
    var risks: List<RiskValue> = ArrayList(),
    var values: List<CalibrationValue> = ArrayList(),
    var resultDetail: ResultDetail = ResultDetail(),
    var error: ErrorType = ErrorType.NO_ERROR,
    var fileName: String = ""
) : Parcelable {

    fun getResultString(context: Context): String {
        return if (resultDetail.result < 0) {
            error.toLocalString(context)
        } else {
            resultDetail.result.toString()
        }
    }

    fun getRisk(context: Context): String {
        val riskType = getRiskType()

        return if (riskAsQty) {
            riskType.toQuantityLocalString(context)
        } else {
            riskType.toLocalString(context)
        }
    }

    fun getRiskType(): RiskType {
        var riskType = RiskType.HIGH

        // Evaluate the risk level based on the result
        for (element in risks) {
            if (resultDetail.result >= element.value) {
                if (element.risk != null) {
                    riskType = element.risk!!
                }
            } else {
                break
            }
        }

        return riskType
    }

    fun getMarginOfError(): Double {

        val margin = max(
            (resultDetail.distance + resultDetail.calibrationDistance) / 200,
            minMarginError
        )

        return (round(margin * 100) / 100.0)
    }
}