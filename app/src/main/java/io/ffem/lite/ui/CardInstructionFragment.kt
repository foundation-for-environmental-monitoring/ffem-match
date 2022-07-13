package io.ffem.lite.ui

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.databinding.FragmentCardInstructionBinding
import io.ffem.lite.util.snackBar
import io.ffem.lite.util.snackBarAction

class CardInstructionFragment : Fragment() {
    private var _binding: FragmentCardInstructionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardInstructionBinding.inflate(inflater, container, false)
        return binding.root
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startTestBtn.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
//        if (requireActivity().window.decorView.measuredHeight < 1300) {
//            binding.instructionTxt.visibility = GONE
//        }

        binding.exampleImg.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.circle_card_example
            )
        )
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class MyUndoListener : View.OnClickListener {
        override fun onClick(v: View) {
//            openAppPermissionSettings()
        }

//        fun openAppPermissionSettings() {
//            val i = Intent()
//            i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//            i.addCategory(Intent.CATEGORY_DEFAULT)
//            i.data = Uri.parse("package:" + app.packageName)
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//            app.startActivity(i)
//        }
    }
}