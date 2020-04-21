package io.ffem.lite.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskType
import io.ffem.lite.model.toLocalString
import io.ffem.lite.util.PreferencesUtil
import kotlinx.android.synthetic.main.fragment_result.*

class ResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val testInfo = ResultFragmentArgs.fromBundle(requireArguments()).testInfo

        if (testInfo.error == ErrorType.NO_ERROR) {
            text_name.text = testInfo.name
            text_result.text = testInfo.getResultString(view.context)
            text_unit.text = testInfo.unit
            text_risk.text = testInfo.getRisk(requireContext())
            when {
                testInfo.getRiskType() == RiskType.HIGH -> {
                    text_risk.setTextColor(resources.getColor(R.color.high_risk, null))
                }
                testInfo.getRiskType() == RiskType.MEDIUM -> {
                    text_risk.setTextColor(resources.getColor(R.color.medium_risk, null))
                }
                testInfo.getRiskType() == RiskType.LOW -> {
                    text_risk.setTextColor(resources.getColor(R.color.low_risk, null))
                }
            }
            text_error_margin.text = String.format("%.2f", testInfo.getMarginOfError())
            lyt_error_message.visibility = GONE
        } else {
            val requestedTestId = PreferencesUtil.getString(context, App.TEST_ID_KEY, "")
            if (testInfo.uuid != requestedTestId) {
                val requestedTest = getTestInfo(requestedTestId!!)
                if (requestedTest != null) {
                    text_name2.text = requestedTest.name
                } else {
                    text_name2.text = testInfo.name
                }
            } else {
                text_name2.text = testInfo.name
            }

            text_error.text = testInfo.error.toLocalString(view.context)
            lyt_result.visibility = GONE
            lyt_result_details.visibility = GONE
            button_submit.setText(R.string.close)
        }

        super.onViewCreated(view, savedInstanceState)
    }
}
