package io.ffem.lite.ui

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.preference.useColorCardVersion2
import io.ffem.lite.util.snackBar
import io.ffem.lite.util.snackBarAction
import kotlinx.android.synthetic.main.fragment_instruction.*

class InstructionFragment : Fragment() {

    interface OnStartTestListener {
        fun onStartTest()
    }

    private var listener: OnStartTestListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnStartTestListener
        if (listener == null) {
            throw ClassCastException("$context must implement OnStartTestListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_instruction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        start_test_btn.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
        if (requireActivity().window.decorView.measuredHeight < 1300) {
            instruction_txt.visibility = GONE
        }

        if (useColorCardVersion2()) {
            example_img.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.card_2_overlay
                )
            )
        } else {
            example_img.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.card_overlay
                )
            )
        }
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            when {
                granted -> {
                    listener?.onStartTest()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    view?.findViewById<CoordinatorLayout>(R.id.coordinator_lyt)
                        ?.snackBar(getString(R.string.camera_permission))
                }
                else -> {
                    view?.findViewById<CoordinatorLayout>(R.id.coordinator_lyt)
                        ?.snackBarAction(getString(R.string.camera_permission), MyUndoListener())
                }
            }
        }

    class MyUndoListener : View.OnClickListener {
        override fun onClick(v: View) {
            App.openAppPermissionSettings()
        }
    }
}