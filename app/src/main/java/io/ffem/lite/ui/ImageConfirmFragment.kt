package io.ffem.lite.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.ffem.lite.databinding.FragmentImageConfirmBinding
import java.io.File

class ImageConfirmFragment : BaseFragment() {
    private var _binding: FragmentImageConfirmBinding? = null
    private val binding get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()

    interface OnConfirmImageListener {
        fun onConfirmImage(action: Int)
    }

    private var listener: OnConfirmImageListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnConfirmImageListener
        if (listener == null) {
            throw ClassCastException("$context must implement OnConfirmImageListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.acceptBtn.setOnClickListener {
            listener?.onConfirmImage(Activity.RESULT_OK)
        }

        binding.redoBtn.setOnClickListener {
            listener?.onConfirmImage(Activity.RESULT_CANCELED)
        }
    }

    override fun onResume() {
        super.onResume()
        if (model.test.get() != null) {
            val path =
                requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                        File.separator + "captures" + File.separator

            val fileName = model.test.get()?.name!!.replace(" ", "")
            val extractImagePath =
                File(path + model.test.get()?.fileName + File.separator + fileName + "_swatch.jpg")

            if (extractImagePath.exists()) {
                binding.colorExtractImg.setImageURI(Uri.fromFile(extractImagePath))
                binding.colorExtractImg.refreshDrawableState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}