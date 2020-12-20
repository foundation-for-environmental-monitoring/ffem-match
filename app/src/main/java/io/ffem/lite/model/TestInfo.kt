package io.ffem.lite.model

import android.content.Context
import android.os.Parcelable
import io.ffem.lite.util.getStringByLocale
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.round

@Parcelize
data class TestInfo(
    var name: String? = null,
    var type: String? = null,
    var uuid: String? = null,
    var unit: String? = null,
    var riskType: RiskType = RiskType.NORMAL,
    var minMarginError: Double = 0.0,
    private var marginError: Double = 0.0,
    var risks: List<RiskValue> = ArrayList(),
    var values: List<CalibrationValue> = ArrayList(),
    var resultInfo: ResultInfo = ResultInfo(),
    var calibratedResultInfo: ResultInfo = ResultInfo(),
    var resultInfoGrayscale: ResultInfo = ResultInfo(),
    var error: ErrorType = ErrorType.NO_ERROR,
    var fileName: String = UUID.randomUUID().toString()
) : Parcelable {

    fun getResultString(context: Context): String {
        return if (resultInfo.result < 0) {
            error.toLocalString(context)
        } else {
            if (calibratedResultInfo.result > -1) {
                calibratedResultInfo.result.toString()
            } else {
                resultInfo.result.toString()
            }
        }
    }

    fun getResult(): Double {
        return if (calibratedResultInfo.result > -1) {
            calibratedResultInfo.result
        } else {
            resultInfo.result
        }
    }

    fun getResultGrayscaleString(): String {
        return if (resultInfoGrayscale.result < 0) {
            ""
        } else {
            resultInfoGrayscale.result.toString()
        }
    }

    fun getRiskEnglish(context: Context): String {
        return getStringByLocale(context, getRiskType().toResourceId(context, riskType), Locale.US)
    }

    fun getRisk(context: Context): String {
        return context.getString(getRiskType().toResourceId(context, riskType))
    }

    fun getRiskType(): RiskLevel {
        var riskType = RiskLevel.HIGH

        val result = if (calibratedResultInfo.result > -1) {
            calibratedResultInfo.result
        } else {
            resultInfo.result
        }

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

        return riskType
    }

    fun getMarginOfError(): Double {
        if (marginError < minMarginError) {
            marginError =
                max((resultInfo.distance + resultInfo.swatchDistance) / 200, minMarginError)
            marginError = (round(marginError * 100) / 100.0)
        }
        return marginError
    }

    fun setMarginOfError(value: Double) {
        marginError = value
    }
}