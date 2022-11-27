package io.ffem.lite.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import io.ffem.lite.R
import io.ffem.lite.data.DataHelper
import io.ffem.lite.data.RecommendationDao
import io.ffem.lite.data.RecommendationDatabase
import io.ffem.lite.model.Recommendation
import io.ffem.lite.model.Result
import io.ffem.lite.model.RiskLevel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.io.StringReader
import java.util.*

private val DarkColors = darkColors(
    background = Color.Gray
)

private val LightColors = lightColors(
    background = Color(0xFFEDEDED)
)

val String.capitalizeWords
    get() = this.lowercase().split(" ").joinToString(" ") { it ->
        it.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.ROOT
            ) else it.toString()
        }
    }

@Suppress("FunctionName")
@Preview(showSystemUi = true)
@Composable
fun ComposablePreview() {
    RecommendationView(Recommendation(crop = "potato", soilType = "clay_alluvial"))
}

@Suppress("FunctionName")
@Composable
fun RecommendationView(info: Recommendation) = MaterialTheme {
    val scrollState = rememberScrollState()
    MaterialTheme(
        colors = if (false) DarkColors else LightColors
    ) {
        Column(
            modifier = Modifier.scrollable(
                state = scrollState,
                orientation = Orientation.Vertical
            ).verticalScroll(scrollState, true).fillMaxWidth()
                .background(MaterialTheme.colors.background).padding(vertical = 8.dp)
        ) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.padding(8.dp)) {
                        Text(text = "Crop", Modifier.defaultMinSize(100.dp))
                        Text(text = info.crop.replace("_", " ").capitalizeWords)
                    }
                    Row(Modifier.padding(8.dp)) {
                        Text(text = "Soil", Modifier.defaultMinSize(100.dp))
                        Text(text = info.soilType.replace("_", " ").capitalizeWords)
                    }
                }
            }

            Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(
                        text = "Recommendation",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(text = "Nitrogen (N)", Modifier.defaultMinSize(220.dp))
                        Text(
                            text = info.nitrogenRecommendation,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(Modifier.padding(8.dp).fillMaxWidth()) {
                        Text(text = "Phosphorus (P2O5)", Modifier.defaultMinSize(220.dp))
                        Text(
                            text = info.phosphorusRecommendation,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(Modifier.padding(8.dp).fillMaxWidth()) {
                        Text(text = "Potassium (K2O)", Modifier.defaultMinSize(220.dp))
                        Text(
                            text = info.potassiumRecommendation,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(R.string.all_values_in_kg_ha),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(
                        text = "Test Results",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(text = "Nitrogen", Modifier.defaultMinSize(220.dp))
                        Text(
                            text = info.nitrogenRisk,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(Modifier.padding(8.dp).fillMaxWidth()) {
                        Text(text = "Phosphorus", Modifier.defaultMinSize(220.dp))
                        Text(
                            text = info.phosphorousRisk,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(Modifier.padding(8.dp).fillMaxWidth()) {
                        Text(text = "Potassium", Modifier.defaultMinSize(220.dp))
                        Text(
                            text = info.potassiumRisk,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

class RecommendationFragment(private val intent: Intent) : Fragment() {
    private val recommendationInfo = Recommendation()
    private var printTemplate: String? = null
    private var date: String? = null
    private var submitResultListener: OnSubmitResultListener? = null

    private lateinit var timer: Timer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            parseXml(intent.getStringExtra("survey-data-xml"), intent)
            recommendationInfo.crop = intent.getStringExtra("Crop_Type") ?: ""
            recommendationInfo.soilType = intent.getStringExtra("Soil_Type") ?: ""
            recommendationInfo.nitrogenResult =
                (intent.getStringExtra("Available_Nitrogen") ?: "")
                    .replace(">", "")
            recommendationInfo.phosphorusResult =
                (intent.getStringExtra("Available_Phosphorous") ?: "")
                    .replace(">", "")
            recommendationInfo.potassiumResult =
                (intent.getStringExtra("Available_Potassium") ?: "")
                    .replace(">", "")

            val db: RecommendationDatabase =
                RecommendationDatabase.getRecommendationDatabase(context)
            try {
                val dao = db.recommendationDao()
                var recommendation: Pair<String, String>
                try {
                    recommendation = getRecommendation(
                        "SB-FM-N", recommendationInfo.nitrogenResult!!.toDouble(),
                        recommendationInfo.crop, recommendationInfo.soilType, dao, context
                    )
                    recommendationInfo.nitrogenRisk = recommendation.first
                    recommendationInfo.nitrogenRecommendation = recommendation.second
                } catch (e: Exception) {
                }

                try {
                    recommendation = getRecommendation(
                        "SB-FM-P", recommendationInfo.phosphorusResult!!.toDouble(),
                        recommendationInfo.crop, recommendationInfo.soilType, dao, context
                    )
                    recommendationInfo.phosphorousRisk = recommendation.first
                    recommendationInfo.phosphorusRecommendation = recommendation.second
                } catch (e: Exception) {
                }

                recommendation = getRecommendation(
                    "SB-FM-K", recommendationInfo.potassiumResult!!.toDouble(),
                    recommendationInfo.crop, recommendationInfo.soilType, dao, context
                )
                recommendationInfo.potassiumRisk = recommendation.first
                recommendationInfo.potassiumRecommendation = recommendation.second

            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                db.close()
            }

            setContent {
                RecommendationView(recommendationInfo)
            }
        }
    }

    private fun getRecommendation(
        testId: String, result: Double, crop: String, soil: String,
        dao: RecommendationDao, context: Context
    ): Pair<String, String> {
        val cropType = if (crop == "potato") {
            0
        } else {
            1
        }
        val riskLevels = dao.getRecommendations(testId, cropType, soil)
        val testInfo = DataHelper.getTestInfo(testId, context)
        var recommendation = ""
        if (riskLevels != null) {
            testInfo!!.subTest().setFinalResult(result)
            val resultRiskLevel = testInfo.subTest().getRiskType()

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

        return Pair(testInfo!!.subTest().getRiskString(requireContext()), recommendation)
    }

    private fun parseXml(xmlString: String?, intent: Intent) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xmlString))
            var eventType = xpp.eventType
            var text = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT) {
                    text = xpp.text
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (!xpp.name.contains("__") && !xpp.name.contains("instanceID") && text.isNotEmpty()) {
                        intent.putExtra(xpp.name, text)
                        Timber.e("%s : %s", xpp.name, text)
                        text = ""
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        submitResultListener = if (context is OnSubmitResultListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnSubmitResultListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        submitResultListener = null
    }

    override fun onPause() {
        super.onPause()
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    interface OnSubmitResultListener {
        fun onSubmitResult(results: ArrayList<Result>)
    }

    companion object {
        private const val DATE_TIME_FORMAT = "dd MMM yyyy HH:mm"
        private const val DATE_FORMAT = "dd MMM yyyy"

        init {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            } catch (_: Exception) {
            }
        }

//        fun isNumeric(strNum: String): Boolean {
//            try {
//                strNum.toDouble()
//            } catch (nfe: NumberFormatException) {
//                return false
//            } catch (nfe: NullPointerException) {
//                return false
//            }
//            return true
//        }
    }
}