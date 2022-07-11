package io.ffem.lite.data

import android.content.Context
import android.graphics.Color
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.ffem.lite.R
import io.ffem.lite.common.ConstantJsonKey
import io.ffem.lite.common.Constants
import io.ffem.lite.model.*
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.util.FileUtil
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

private const val BIT_MASK = 0x00FFFFFF

object DataHelper {

    @JvmStatic
    fun getUnit(id: String, context: Context): String? {
        val testInfo = getTestInfo(id, context)
        return if (testInfo != null) {
            testInfo.subTest().unit
        } else {
            ""
        }
    }

    // Append a reversed list of calibration point values to the calibration values list
    // to represent the colors on the right side of color card
    internal class CalibrationValuesDeserializer(val context: Context) :
        JsonDeserializer<MutableList<CalibrationValue>> {
        override fun deserialize(
            json: JsonElement?, typeOfT: Type?, jsonContext: JsonDeserializationContext?
        ): MutableList<CalibrationValue> {
            val values = ArrayList<CalibrationValue>()
            json!!.asJsonArray.mapTo(values) {
                CalibrationValue(
                    value = it.asJsonObject.get("value").asDouble,
                    color = Color.TRANSPARENT,
                    calibrate = it.asJsonObject.get("calibrate")?.asBoolean ?: false
                )
            }
            values.addAll(values.map {
                CalibrationValue(
                    value = it.value,
                    color = Color.TRANSPARENT,
                    calibrate = it.calibrate
                )
            })

            return values
        }
    }

    internal fun checkHyphens(str: String): String {
        val code = str.replace("FM", "HD")
            .replace("TOC", "OC")
        return if (!code.contains("-")) {
            val stringBuilder = StringBuilder(str)
            stringBuilder.insert(2, '-')
            stringBuilder.insert(5, '-')
            stringBuilder.toString()
        } else {
            code
        }
    }

    @JvmStatic
    fun getTestInfo(parameterId: String, context: Context): TestInfo? {
        try {
            if (parameterId.isEmpty()) {
                return null
            }
            var id = checkHyphens(parameterId.uppercase())
            id = id.replace("CL2", "CL")
            id = id.replace("WA", "WC")
            id = id.replace("WB", "WC")
            id = id.replace("WR", "WC")
            id = id.replace("SA", "SC")
            id = id.replace("SB", "SC")
            id = id.replace("SR", "SC")

            val gson: Gson =
                GsonBuilder().registerTypeAdapter(
                    object :
                        TypeToken<MutableList<CalibrationValue>>() {}.type,
                    CalibrationValuesDeserializer(context)
                ).create()

            val input = when {
                id.uppercase().startsWith("WA") -> {
                    id = id.replace("WA", "WB")
                    context.resources.openRawResource(R.raw.water_tests)
                }
                id.uppercase().startsWith("WB") -> {
                    context.resources.openRawResource(R.raw.water_tests)
                }
                id.uppercase().startsWith("WM") -> {
                    context.resources.openRawResource(R.raw.meter_tests)
                }
                id.uppercase().startsWith("WT") -> {
                    context.resources.openRawResource(R.raw.water_tests)
                }
                id.uppercase().startsWith("ST") -> {
                    context.resources.openRawResource(R.raw.soil_tests)
                }
                id.uppercase().startsWith("SB") -> {
                    context.resources.openRawResource(R.raw.soil_tests)
                }
                else -> {
                    AppPreferences.setCurrentCardType(context, 2)
                    context.resources.openRawResource(R.raw.tests_circle)
                }
            }
            val content = FileUtil.readTextFile(input)
            val testConfig = gson.fromJson(content, TestConfig::class.java)
            if (testConfig != null) {
                val db: CalibrationDatabase = CalibrationDatabase.getDatabase(context)
                val dao = db.calibrationDao()
                try {
                    for (testInfo in testConfig.tests) {
                        if (testInfo.uuid.equals(id, ignoreCase = true)) {
                            val subTest = testInfo.subTest()
                            subTest.parameterId = testInfo.uuid
                            if (testInfo.dilutions.isNotEmpty()) {
                                subTest.maxDilution =
                                    testInfo.dilutions[testInfo.dilutions.size - 1]
                            }
                            if (testInfo.subtype === TestType.CUVETTE) {
                                subTest.splitRanges()
                                val calibrationInfo = dao.getCalibrations(testInfo.uuid)
                                if (calibrationInfo != null) {
                                    subTest.setCalibrations(calibrationInfo.calibrations)
                                }
                            }
                            for ((index, risk) in subTest.risks.withIndex()) {
                                if (risk.safety == null) {
                                    when (index) {
                                        0 -> {
                                            risk.safety = SafetyLevel.ACCEPTABLE
                                        }
                                        1 -> {
                                            risk.safety = SafetyLevel.PERMISSIBLE
                                        }
                                        else -> {
                                            risk.safety = SafetyLevel.UNSAFE
                                        }
                                    }
                                }
                            }


                            return testInfo.copy()
                        }
                    }
                } finally {
                    db.close()
                }
            }
        } catch (e: JsonSyntaxException) {
            // do nothing
        }
        return null
    }

    fun getParameterValues(id: String, context: Context): List<CalibrationValue> {
        val test = getTestInfo(id, context)
        return test?.subTest()?.values ?: emptyList()
    }

    suspend fun getCalibrationFromTheCloud(deviceId: String, testId: String): FactoryConfig? {
        val ref = FirebaseDatabase.getInstance().getReference(deviceId)
        return try {
            var factoryConfig: FactoryConfig? = null
            val calibrationConfig = ref.get().await().children.map { snapShot ->
                snapShot.getValue(FactoryConfig::class.java)!!
            }
            for (calibration in calibrationConfig) {
                if (calibration.uuid.equals(testId, ignoreCase = true)) {
                    factoryConfig = calibration.copy()
                }
            }
            return factoryConfig
        } catch (exception: Exception) {
            Timber.e(exception)
            FactoryConfig("", ArrayList())
        }
    }

    fun getJsonResult(
        testInfo: TestInfo, results: ArrayList<Result>, color: Int,
        resultImageUrl: String?, context: Context
    ): JSONObject {
        val resultJson = JSONObject()
//        try {
        resultJson.put(ConstantJsonKey.TEST_TYPE,
            testInfo.subtype.toString().lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        resultJson.put(ConstantJsonKey.SAMPLE_TYPE,
            testInfo.sampleType.toString().lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        resultJson.put(ConstantJsonKey.NAME, testInfo.name)
        resultJson.put(ConstantJsonKey.PARAMETER_ID, testInfo.uuid)
        val resultsJsonArray = JSONArray()
        for (subTest in testInfo.results) {
            if (subTest.input) {
                continue
            }

            val subTestJson = JSONObject()
            subTestJson.put(ConstantJsonKey.DILUTION, subTest.dilution)
            subTestJson.put(ConstantJsonKey.NAME, subTest.name)
            subTestJson.put(ConstantJsonKey.UNIT, subTest.unit)
            subTestJson.put(ConstantJsonKey.ID, subTest.id)
            // If a result exists for the sub test id then add it
            for (i in 1..10) {
                for (result in results) {
                    if (result.id == i) {
                        if (result.getResult() > -1) {
                            subTestJson.put(
                                ConstantJsonKey.VALUE,
                                result.getResultString()
                            )
                        }
                    }
                }
            }
            if (color > -1) {
                subTestJson.put("resultColor", Integer.toHexString(color and BIT_MASK))
//                    val calibrationDetail =
//                        db?.calibrationDao()!!.getCalibrationDetails(testInfo.uuid)
//                    // Add calibration details to result
//                    subTestJson.put(
//                        "calibratedDate",
//                        SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.US)
//                            .format(calibrationDetail!!.date)
//                    )
//                    subTestJson.put("reagentExpiry", calibrationDetail.expiry)
//                    subTestJson.put("cuvetteType", calibrationDetail.cuvetteType)
                val calibrationSwatches = JSONArray()
                for (calibration in subTest.colors) {
                    calibrationSwatches.put(Integer.toHexString(calibration.color and BIT_MASK))
                }
                subTestJson.put("calibration", calibrationSwatches)
            }
            resultsJsonArray.put(subTestJson)
        }
        resultJson.put(ConstantJsonKey.RESULT, resultsJsonArray)
        if (resultImageUrl != null && resultImageUrl.isNotEmpty()) {
            resultJson.put(ConstantJsonKey.IMAGE, resultImageUrl)
        }
        // Add current date to result
        resultJson.put(
            ConstantJsonKey.TEST_DATE, SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.US)
                .format(Calendar.getInstance().time)
        )
//        } catch (e: JSONException) {
//            Timber.e(e)
//        }
        return resultJson
    }

//    fun getFactoryCalibration(id: String, testId: String, context: Context): FactoryConfig? {
//        try {
////            Toast.makeText(context, id, Toast.LENGTH_LONG).show()
//            val gson: Gson = GsonBuilder().create()
//            val input = if (id.contains("d=")) {
//                context.resources.openRawResource(R.raw.prototype_1)
//            } else {
//                null
//            }
//            if (input != null) {
//                val testInfo = getTestInfo(testId, context)
//                if (testInfo != null) {
//                    val content = FileUtil.readTextFile(input)
//                    val calibrationConfig = gson.fromJson(content, CalibrationConfig::class.java)
//                    if (calibrationConfig != null) {
//                        for (calibration in calibrationConfig.calibrations) {
//                            if (calibration.uuid.equals(testId, ignoreCase = true)) {
//                                return calibration.copy()
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (e: JsonSyntaxException) {
//            return null
//            // do nothing
//        }
//        return null
//    }
}
