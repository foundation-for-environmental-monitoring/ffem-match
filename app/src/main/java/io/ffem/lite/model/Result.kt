package io.ffem.lite.model

import android.content.Context
import android.graphics.Color
import android.os.Parcelable
import io.ffem.lite.R
import io.ffem.lite.common.Constants
import io.ffem.lite.data.RecommendationDatabase
import io.ffem.lite.preference.getCalibrationType
import io.ffem.lite.util.MathUtil.applyFormula
import io.ffem.lite.util.StringUtil.getStringByLocale
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@Parcelize
data class Result(
    var id: Int = 0,
    var name: String? = null,
    var unit: String? = "",
    var formula: String? = "",
    var unitChoice: String? = "",
    var timeDelay: Int = 0,
    var ranges: String? = null,
    var rangeMin: String? = null,
    var values: MutableList<CalibrationValue> = ArrayList(),
    var risks: List<RiskValue> = ArrayList(),
    var risksIrrigation: List<RiskValue> = ArrayList(),
    var acceptable: String? = null,
    var permissible: String? = null,
    var riskType: RiskType = RiskType.NORMAL,
    private var safetyLevel: SafetyLevel = SafetyLevel.UNSAFE,
    var criticalLimit: Double = 0.0,
    var criticalLimitSign: String = "",
    var criticalMax: Double = 0.0,
    var criticalMin: Double = 0.0,
    var grayScale: Boolean = false,
    private var resultString: String? = null,
    private var noDilutionResultValue: Double = 0.0,
    var resultInfo: ResultInfo = ResultInfo(),
    var calibratedResult: ResultInfo = ResultInfo(),
    var dilution: Int = 1,
    var maxDilution: Int = 1,
    var display: Int? = 0,
    var decimalPlaces: Int = 0,
    var input: Boolean = false,
    var error: ErrorType = ErrorType.NO_ERROR,
    var colors: ArrayList<ColorInfo> = ArrayList(),
    var minMarginError: Double = 0.0,
    private var marginError: Double = 0.0,
    var parameterId: String = ""
) : Parcelable {

    fun setCalibrations(values: List<Calibration>) {
        colors.clear()
        val newCalibrations: MutableList<Calibration> = ArrayList()
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

    // if range values are defined as comma delimited text then convert to array
    fun splitRanges() {
        try {
            var range = ranges
            if (range.isNullOrEmpty()) {
                range = rangeMin
            }
            if (getCalibrationType() == 0 && rangeMin != null && rangeMin!!.isNotEmpty()) {
                range = rangeMin
            }
            if (range != null) {
                if (range.isNotEmpty()) {
                    val points = range.split(",").toTypedArray()
                    values.clear()
                    colors.clear()
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

    fun setFinalResult(resultDouble: Double) {
        resultInfo.result = round(resultDouble, 2)
        if (resultDouble == -1.0) {
            resultString = ""
        } else {
            if (values.size > 0) {
                // determine if high levels of contaminant
                resultInfo.highLevelsFound = resultDouble > (maxValue * dilution) * 0.95

                // if no more dilution can be performed then set result to highest value
                resultInfo.result = round(resultDouble, 3)
                resultString = Constants.DECIMAL_FORMAT.format(resultInfo.result)

                // Add 'greater than' symbol if result could be an unknown high value
                if (resultInfo.highLevelsFound) {
                    resultString = "> $resultString"
                }
            } else {
                resultInfo.result = round(resultDouble, 3)
                resultString = Constants.DECIMAL_FORMAT.format(resultInfo.result)
            }
            noDilutionResultValue = round(resultDouble, 2)
        }
    }

    fun setResult(resultDouble: Double) {
        dilution = max(1, dilution)
        if (resultDouble == -1.0) {
            resultString = ""
        } else {
            if (values.size > 0) {
                // determine if high levels of contaminant
                resultInfo.result = round(applyFormula(resultDouble * dilution, formula), 3)
                resultInfo.highLevelsFound = resultInfo.result > maxValue * 0.95

                // if no more dilution can be performed then set result to highest value
                if (dilution >= maxDilution && resultInfo.highLevelsFound) {
                    resultInfo.result = maxValue
                }
                resultString = Constants.DECIMAL_FORMAT.format(resultInfo.result)

                // Add 'greater than' symbol if result could be an unknown high value
                if (resultInfo.highLevelsFound) {
                    resultString = "> $resultString"
                }
            } else {
                resultInfo.result = round(applyFormula(resultDouble, formula), 3)
                resultString = Constants.DECIMAL_FORMAT.format(resultInfo.result)
            }
            noDilutionResultValue = round(applyFormula(resultDouble, formula), 2)
        }
    }

    fun getResultString(): String {
        return if (resultInfo.result < 0 && calibratedResult.result < 0) {
            error.toLocalString()
        } else {
            if (calibratedResult.result > -1) {
                if (calibratedResult.result >= maxValue * 0.99) {
                    "> $maxValue"
                } else {
                    Constants.DECIMAL_FORMAT.format(calibratedResult.result)
                }
            } else {
                if (resultInfo.result >= maxValue * 0.99) {
                    "> $maxValue"
                } else {
                    Constants.DECIMAL_FORMAT.format(resultInfo.result)
                }
            }
        }
    }

    fun getActualResult(context: Context): String {
        return if (resultInfo.result < 0 && calibratedResult.result < 0) {
            error.toLocalString()
        } else {
            Constants.DECIMAL_FORMAT.format(calibratedResult.result)
        }
    }

    fun getUncalibratedResult(context: Context): String {
        return if (resultInfo.result < 0) {
            error.toLocalString()
        } else {
            Constants.DECIMAL_FORMAT.format(resultInfo.result)
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
            try {
                for (i in 1 until values.size) {
                    if (values[i].value == values[0].value) {
                        return applyFormula(values[(values.size / 2) - 1].value, formula) * dilution
                    }
                }
                return applyFormula(colors[colors.size - 1].value, formula) * dilution
//                return colors[colors.size - 1].value
            } catch (e: Exception) {
                return Double.MAX_VALUE
            }
        }

    fun getSafetyLevel(): SafetyLevel {
        getRiskType()
        return safetyLevel
    }

    fun getCriticalRange(): String {
        return if (criticalLimit > 0) {
            if (criticalLimitSign.isNotEmpty()) {
                criticalLimitSign + " " +
                        Constants.DECIMAL_FORMAT.format(criticalLimit)
            } else {
                Constants.DECIMAL_FORMAT.format(criticalLimit)
            }
        } else if (criticalMax > 0 && criticalMin > 0) {
            Constants.DECIMAL_FORMAT.format(criticalMin) + " - " +
                    Constants.DECIMAL_FORMAT.format(criticalMax)
        } else {
            ""
        }
    }

    fun getRiskString(context: Context): String {
        val risk = getRisk(context)
        return if (risk > -1) {
            context.getString(risk)
        } else {
            ""
        }
    }

    fun getRiskEnglish(context: Context): String {
        return try {
            getStringByLocale(context, getRisk(context), Locale.US)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getRisk(context: Context): Int {
        return try {
            if (criticalLimit > 0) {
                if (criticalLimitSign == "<") {
                    if (resultInfo.result < criticalLimit) {
                        R.string.deficient
                    } else {
                        R.string.sufficient
                    }
                } else if (resultInfo.result <= criticalLimit) {
                    R.string.deficient
                } else {
                    R.string.sufficient
                }
            } else if (risks.isNotEmpty()) {
                getRiskType().toResourceId(context, riskType)
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    fun getRiskType(): RiskLevel {
        var riskLevel = RiskLevel.RISK_6

        var result = getResult()
        if (resultInfo.highLevelsFound) {
            result *= 1.05
        }

        // Evaluate the risk level based on the result
        try {
            safetyLevel = SafetyLevel.UNSAFE
            for (i in risks.indices) {
                if (risks[i].sign == "<") {
                    if (result < risks[i].value) {
                        riskLevel = risks[max(i, 0)].risk ?: RiskLevel.RISK_6
                        safetyLevel = risks[max(i, 0)].safety ?: SafetyLevel.UNSAFE
                        break
                    }
                } else {
                    if (result <= risks[i].value) {
                        riskLevel = risks[max(i, 0)].risk ?: RiskLevel.RISK_6
                        safetyLevel = risks[max(i, 0)].safety ?: SafetyLevel.UNSAFE
                        break
                    }
                }

                if (risks[i].sign == ">" || (i == risks.size - 1 && risks[i].sign.isEmpty())) {
                    if (result > risks[i].value) {
                        riskLevel = risks[max(i, 0)].risk ?: RiskLevel.RISK_6
                        safetyLevel = risks[max(i, 0)].safety ?: SafetyLevel.UNSAFE
                        break
                    }
                }
            }
        } catch (e: Exception) {
        }

        return riskLevel
    }

    fun getLowRiskLevel(): String {
        try {
            for (i in risks.indices) {
                if (risks[i].risk == RiskLevel.RISK_1) {
                    return risks[i].sign + " " + risks[i].value.toString()
                }
            }
        } catch (e: Exception) {
        }
        return ""
    }

    fun getMediumRiskLevel(): String {
        try {
            for (i in risks.indices) {
                if (risks[i].risk == RiskLevel.RISK_2) {
                    return if (risks[i - 1].sign == "<" && risks[i + 1].risk != RiskLevel.RISK_2) {
                        risks[i - 1].value.toString() + " - " + risks[i].value.toString()
                    } else {
                        risks[i].value.toString() + " - " + risks[i + 1].value.toString()
                    }
                }
            }
        } catch (e: Exception) {
        }
        return ""
    }

    fun getHighRiskLevel(): String {
        try {
            for (i in risks.indices) {
                if (risks[i].risk == RiskLevel.RISK_3) {
                    return risks[i].sign + " " + risks[i].value.toString()
                }
            }
        } catch (e: Exception) {
        }
        return ""
    }

    fun getLowIrrigationRiskLevel(): String {
        try {
            for (i in risksIrrigation.indices) {
                if (risksIrrigation[i].risk == RiskLevel.RISK_1) {
                    return risksIrrigation[i].sign + " " + risksIrrigation[i].value.toString()
                }
            }
        } catch (e: Exception) {
        }
        return ""
    }

    fun getMediumIrrigationRiskLevel(): String {
        var value = ""
        try {
            for (i in risksIrrigation.indices) {
                if (risksIrrigation[i].risk == RiskLevel.RISK_2) {
                    value =
                        if (risksIrrigation[i - 1].sign == "<" && risksIrrigation[i + 1].risk != RiskLevel.RISK_2) {
                            risksIrrigation[i - 1].value.toString() + " - " + risksIrrigation[i].value.toString()
                        } else {
                            risksIrrigation[i].value.toString() + " - " + risksIrrigation[i + 1].value.toString()
                        }
                }
            }

            if (value == "") {
                for (i in risksIrrigation.indices) {
                    if (risksIrrigation[i].risk == RiskLevel.RISK_1) {
                        value = risksIrrigation[i].value.toString()
                    }
                    if (risksIrrigation[i].risk == RiskLevel.RISK_3) {
                        value += " - " + risksIrrigation[i].value.toString()
                    }
                }
            }
        } catch (e: Exception) {
        }
        return value
    }

    fun getHighIrrigationRiskLevel(): String {
        try {
            for (i in risksIrrigation.indices) {
                if (risksIrrigation[i].risk == RiskLevel.RISK_3) {
                    return risksIrrigation[i].sign + " " + risksIrrigation[i].value.toString()
                }
            }
        } catch (e: Exception) {
        }
        return ""
    }

    fun getAcceptableLimit(): String {
        try {
            return acceptable ?: ""
        } catch (e: Exception) {
        }
        return ""
    }

    fun getPermissibleLimit(context: Context): String {
        try {
            return permissible ?: context.getString(R.string.no_relaxation)
        } catch (e: Exception) {
        }
        return ""
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

    fun getRecommendation(crop: String, soil: String, context: Context): String {
        var recommendation = ""
        val cropType = if (crop == "potato") {
            0
        } else {
            1
        }

        val soilType = if (soil.isEmpty()) {
            "black_soil"
        } else {
            soil
        }

        val db: RecommendationDatabase =
            RecommendationDatabase.getRecommendationDatabase(context)
        val dao = db.recommendationDao()
        try {
            val riskLevels =
                dao.getRecommendations(parameterId.replace("SR", "SB"), cropType, soilType)
            if (riskLevels != null) {
                val resultRiskLevel = getRiskType()
                for (riskLevel in riskLevels) {
                    recommendation = when (resultRiskLevel) {
                        RiskLevel.RISK_1 -> {
                            riskLevel?.risk2.toString()
                        }
                        RiskLevel.RISK_2 -> {
                            riskLevel?.risk3.toString()
                        }
                        else -> {
                            riskLevel?.risk4.toString()
                        }
                    }
                }
            }
        } finally {
            db.close()
        }

        return recommendation
    }
}