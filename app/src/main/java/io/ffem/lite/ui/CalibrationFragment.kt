package io.ffem.lite.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.ffem.lite.R
import io.ffem.lite.common.BROADCAST_CALIBRATION_CHANGED
import io.ffem.lite.data.CalibrationDatabase
import io.ffem.lite.databinding.FragmentCalibrationBinding
import io.ffem.lite.helper.SwatchHelper
import io.ffem.lite.model.TestInfo
import java.text.DateFormat
import java.util.*

class CalibrationFragment : BaseFragment(),
    CalibrationExpiryDialog.OnCalibrationExpirySavedListener {
    private var _binding: FragmentCalibrationBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()
    private var rowSelectedListener: OnCalibrationSelected? = null
    private var editDetailsListener: OnCalibrationDetailsEdit? = null
    private lateinit var broadcastManager: LocalBroadcastManager

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(BROADCAST_CALIBRATION_CHANGED)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibrationBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(b.calibrationLst) {
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
        b.calibrationLst.layoutManager = LinearLayoutManager(context)
        b.dateFab.setOnClickListener {
            val ft = childFragmentManager.beginTransaction()
            val calibrationExpiryDialog =
                CalibrationExpiryDialog.newInstance(model.test.get(), true)
            calibrationExpiryDialog.show(ft, "calibrationExpiry")
        }
    }

    override fun onResume() {
        super.onResume()
        loadInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    private fun loadInfo() {
        model.loadCalibrations()

        with(b.calibrationLst) {
            adapter = CalibrationAdapter(
                this@CalibrationFragment::onCalibrationClick,
                model.test.get()!!
            )
            adapter!!.notifyDataSetChanged()
        }

        loadDetails(model.test.get()!!.uuid)
    }

    private fun loadDetails(uuid: String) {
        val db: CalibrationDatabase = CalibrationDatabase.getDatabase(requireContext())
        try {
            val calibrationDetail = db.calibrationDao().getCalibrationDetail(uuid)
            if (calibrationDetail != null) {
                if (calibrationDetail.name.isNotEmpty()) {
                    b.nameText.text = calibrationDetail.name
                    b.nameText.visibility = VISIBLE
                } else {
                    b.nameText.visibility = GONE
                }

                if (calibrationDetail.desc.isNotEmpty()) {
                    b.descText.text = calibrationDetail.desc
                    b.descText.visibility = VISIBLE
                } else {
                    b.descText.visibility = GONE
                }

                b.subtitle1Text.text = calibrationDetail.cuvetteType
                if (calibrationDetail.date > 0) {
                    b.subtitle1Text.text = DateFormat
                        .getDateInstance(DateFormat.MEDIUM).format(Date(calibrationDetail.date))
                } else {
                    b.subtitle1Text.text = ""
                }
                if (calibrationDetail.expiry > 0) {
                    b.subtitle2Text.text = String.format(
                        "%s: %s", getString(R.string.expires),
                        DateFormat.getDateInstance(DateFormat.MEDIUM)
                            .format(Date(calibrationDetail.expiry))
                    )
                } else {
                    b.subtitle2Text.text = ""
                }

                if (calibrationDetail.expiry > 0 && calibrationDetail.expiry <= Date().time) {
                    b.errorText.text = String.format(
                        "%s. %s", getString(R.string.expired),
                        getString(R.string.calibrate_with_new_reagent)
                    )
                    b.errorText.visibility = VISIBLE
                } else {
                    if (SwatchHelper.isSwatchListValid(model.test.get(), true)
                    ) {
                        b.errorText.visibility = GONE
                    } else {
                        b.errorText.visibility = VISIBLE
                    }
                }
            }
        } finally {
            db.close()
        }
    }

    private fun onCalibrationClick(position: Int) {
        model.calibrationPoint = (model.test.get() as TestInfo).subTest().colors[position].value
        if (rowSelectedListener != null) {
            rowSelectedListener!!.onCalibrationSelected(position)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        rowSelectedListener = if (context is OnCalibrationSelected) {
            context
        } else {
            throw IllegalArgumentException(
                "$context must implement Listener"
            )
        }

        if (context is OnCalibrationDetailsEdit) {
            editDetailsListener = context
        }
    }

    interface OnCalibrationSelected {
        fun onCalibrationSelected(position: Int)
    }

    interface OnCalibrationDetailsEdit {
        fun onCalibrationDetailsEdit()
    }

    override fun onCalibrationExpirySavedListener() {
        loadDetails(model.test.get()!!.uuid)
        if (editDetailsListener != null) {
            editDetailsListener!!.onCalibrationDetailsEdit()
        }
    }
}