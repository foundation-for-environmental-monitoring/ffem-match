package io.ffem.lite.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import kotlinx.android.synthetic.main.fragment_result.*

class ResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val testInfo = ResultFragmentArgs.fromBundle(arguments!!).testInfo
        text_title.text = testInfo.name
        text_result.text = testInfo.getResultString(view.context)
        if (testInfo.error == ErrorType.NO_ERROR) {
            text_unit.text = testInfo.unit
            if (testInfo.riskAsQty) {
                text_risk.text = getString(R.string.risk_qty_value, testInfo.getRisk(context!!))
            } else {
                text_risk.text = getString(R.string.risk_value, testInfo.getRisk(context!!))
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}