package io.ffem.lite.model

import android.content.Context
import android.graphics.Color
import android.os.Parcelable
import io.ffem.lite.common.Constants
import io.ffem.lite.data.Calibration
import io.ffem.lite.util.MathUtil.applyFormula
import io.ffem.lite.util.getStringByLocale
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max

@Parcelize
data class Result(
    var id: Int = 0,
    var uuid: String = UUID.randomUUID().toString().replace("-", ""),
    var name: String? = null,
    var unit: String? = "",
    var formula: String? = "",
    var unitChoice: String? = "",
    private var timeDelay: Int = 0,
    var ranges: String? = null,
    var values: MutableList<CalibrationValue> = ArrayList(),
    var risks: List<RiskValue> = ArrayList(),
    var acceptable: String? = null,
    var permissible: String? = null,
    var riskType: RiskType = RiskType.NORMAL,
    private var safetyLevel: SafetyLevel = SafetyLevel.UNSAFE,
    var resultString: String? = null,
    var highLevelsFound: Boolean = false,
    private var noDilutionResultValue: Double = 0.0,
    var resultInfo: ResultInfo = ResultInfo(),
    var calibratedResult: ResultInfo = ResultInfo(),
    var dilution: Int = 1,
    var minMarginError: Double = 0.0,
    private var marginError: Double = 0.0,
    var display: Int? = 0,
    var decimalPlaces: Int = 0,
    var input: Boolean = false,
    var error: ErrorType = ErrorType.NO_ERROR,
    var colors: ArrayList<ColorInfo> = ArrayList()
) : Parcelable {

    fun setCalibrations(values: List<Calibration>) {
        colors.clear()
        val newCalibrations: MutableList<Calibration> = java.util.ArrayList()
        for (colorItem in values) {
            val newCalibration = Calibration(value = colorItem.value)
            for (i in values.indices.reversed()) {
                val calibration = values[i]
                if (calibration.value == colorItem.value) {
                    newCalibration.color = calibration.color
                    newCalibration.date = calibration.date
                }
            }
            val swatch = ColorInfo(newCalibration.value, newCalibration.color)
            colors.add(swatch)
            val text = abs(newCalibration.value).toString()
            if (newCalibration.value % 1 != 0.0) {
                decimalPlaces = max(text.length - text.indexOf('.') - 1, decimalPlaces)
            }
            newCalibrations.add(newCalibration)
        }
    }

    fun getTimeDelay(): Int {
        return timeDelay
    }

    // if range values are defined as comma delimited text then convert to array
    fun splitRanges() {
        try {
            if (ranges != null) {
                if (ranges!!.isNotEmpty()) {
                    val points = ranges!!.split(",").toTypedArray()
                    for (v in points) {
                        values.add(CalibrationValue(v.toDouble(), Color.TRANSPARENT))
                        colors.add(ColorInfo(v.toDouble(), Color.TRANSPARENT))
                    }
                }
            }
        } catch (ignored: NumberFormatException) {
            // do nothing
        }
    }

    fun calculateResult(value: Double): Double {
        return applyFormula(value, formula)
    }

    fun setResult(resultDouble: Double, diluteTimes: Int, maxDilution: Int) {
        dilution = max(1, diluteTimes)
        if (resultDouble == -1.0) {
            resultString = ""
        } else {
            if (colors.size > 0) {
                // determine if high levels of contaminant
                val maxResult: Double = colors[colors.size - 1].value
                highLevelsFound = resultDouble > maxResult * 0.95
                resultInfo.result = round(applyFormula(resultDouble * dilution, formula), 2)

                // if no more dilution can be performed then set result to highest value
                if (highLevelsFound) {
                    resultInfo.result = applyFormula(maxResult * dilution, formula)
                }
                resultString = Constants.DECIMAL_FORMAT.format(resultInfo.result)

                // Add 'greater than' symbol if result could be an unknown high value
                if (highLevelsFound) {
                    resultString = "> $resultString"
                }
            } else {
                resultString = Constants.DECIMAL_FORMAT.format(resultDouble)
            }
            noDilutionResultValue = round(resultDouble, 2)
        }
    }

    @Suppress("SameParameterValue")
    private fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd: BigDecimal = BigDecimal.valueOf(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    val maxValue: Double
        get() {
            return applyFormula(values[(values.size / 2) - 1].value, formula)
        }

    fun getResultString(context: Context): String {
        return if (resultInfo.result < 0) {
            error.toLocalString(context)
        } else {
            if (calibratedResult.result > -1) {
                if (calibratedResult.result >= maxValue) {
                    "> $maxValue"
                } else {
                    calibratedResult.result.toString()
                }
            } else {
                if (resultInfo.result >= maxValue) {
                    "> $maxValue"
                } else {
                    resultInfo.result.toString()
                }
            }
        }
    }

    fun getRiskType(): RiskLevel {
        var riskType = RiskLevel.HIGH

        val result = if (calibratedResult.result > -1) {
            calibratedResult.result
        } else {
            resultInfo.result
        }

        // Evaluate the risk level based on the result
        try {
            for (i in risks.indices) {
                if (result < risks[i].value) {
                    riskType = risks[max(i - 1, 0)].risk!!
                    break
                }
            }
        } catch (e: Exception) {
        }

        return riskType
    }

    fun getResult(): Double {
        return if (calibratedResult.result > -1) {
            calibratedResult.result
        } else {
            resultInfo.result
        }
    }

    fun getMarginOfError(): Double {
        if (marginError < minMarginError) {
            marginError =
                max((resultInfo.distance + resultInfo.swatchDistance) / 200, minMarginError)
            marginError = (kotlin.math.round(marginError * 100) / 100.0)
        }
        return marginError
    }

    fun setMarginOfError(value: Double) {
        marginError = value
    }

    fun getRisk(context: Context): String {
        return context.getString(getRiskType().toResourceId(context, riskType))
    }

    fun getRiskEnglish(context: Context): String {
        return getStringByLocale(context, getRiskType().toResourceId(context, riskType), Locale.US)
    }

//    fun getSafetyLevel(): SafetyLevel {
//        getRiskType()
//        return safetyLevel
//    }

//    fun getRisk(context: Context): String {
//        return try {
//            if (criticalLimit > 0) {
//                if (resultInfo.result < criticalLimit) {
//                    context.getString(R.string.deficient)
//                } else {
//                    context.getString(R.string.sufficient)
//                }
//            } else {
//                context.getString(getRiskType().toResourceId(context, riskType))
//            }
//        } catch (e: Exception) {
//            ""
//        }
//    }

//    fun getRiskType(): RiskLevel {
//        var riskLevel = RiskLevel.RISK_6
//
//        // Evaluate the risk level based on the result
//        try {
//            safetyLevel = SafetyLevel.UNSAFE
//            for (i in risks.indices) {
//                if (risks[i].sign == "<") {
//                    if (resultInfo.result < risks[i].value) {
//                        riskLevel = risks[max(i, 0)].risk ?: RiskLevel.RISK_6
//                        safetyLevel = risks[max(i, 0)].safety!!
//                        break
//                    }
//                } else {
//                    if (resultInfo.result <= risks[i].value) {
//                        riskLevel = risks[max(i, 0)].risk ?: RiskLevel.RISK_6
//                        safetyLevel = risks[max(i, 0)].safety!!
//                        break
//                    }
//                }
//
//                if (risks[i].sign == ">") {
//                    if (resultInfo.result > risks[i].value) {
//                        riskLevel = risks[max(i, 0)].risk ?: RiskLevel.RISK_6
//                        safetyLevel = risks[max(i, 0)].safety!!
//                        break
//                    }
//                }
//            }
//        } catch (e: Exception) {
//        }
//
//        return riskLevel
//    }

//    fun getLowRiskLevel(): String {
//        try {
//            for (i in risks.indices) {
//                if (risks[i].risk == RiskLevel.RISK_1) {
//                    return risks[i].sign + " " + risks[i].value.toString()
//                }
//            }
//        } catch (e: Exception) {
//        }
//        return ""
//    }
//
//    fun getMediumRiskLevel(): String {
//        try {
//            for (i in risks.indices) {
//                if (risks[i].risk == RiskLevel.RISK_2) {
//                    return if (risks[i - 1].sign == "<" && risks[i + 1].risk != RiskLevel.RISK_2) {
//                        risks[i - 1].value.toString() + " - " + risks[i].value.toString()
//                    } else {
//                        risks[i].value.toString() + " - " + risks[i + 1].value.toString()
//                    }
//                }
//            }
//        } catch (e: Exception) {
//        }
//        return ""
//    }
//
//    fun getHighRiskLevel(): String {
//        try {
//            for (i in risks.indices) {
//                if (risks[i].risk == RiskLevel.RISK_3) {
//                    return risks[i].sign + " " + risks[i].value.toString()
//                }
//            }
//        } catch (e: Exception) {
//        }
//        return ""
//    }
//
//    fun getAcceptableLimit(): String {
//        try {
//            return acceptable ?: ""
//        } catch (e: Exception) {
//        }
//        return ""
//    }
//
//    fun getPermissibleLimit(context: Context): String {
//        try {
//            return permissible ?: context.getString(R.string.no_relaxation)
//        } catch (e: Exception) {
//        }
//        return ""
//    }
}