package io.ffem.lite.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.fragment_image_confirm.*
import java.io.File

class ImageConfirmFragment : Fragment() {
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accept_btn.setOnClickListener {
            listener?.onConfirmImage(Activity.RESULT_OK)
        }

        redo_btn.setOnClickListener {
            listener?.onConfirmImage(Activity.RESULT_CANCELED)
        }
    }

    override fun onResume() {
        super.onResume()
        toolbar.setTitle(R.string.confirm)

        if (model.test.get() != null) {
            val path =
                requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                        File.separator + "captures" + File.separator

            val fileName = model.test.get()?.name!!.replace(" ", "")
            val extractImagePath =
                File(path + model.test.get()?.fileName + File.separator + fileName + "_swatch.jpg")

            if (extractImagePath.exists()) {
                color_extract_img.setImageURI(Uri.fromFile(extractImagePath))
                color_extract_img.refreshDrawableState()
            }
        }
    }
}