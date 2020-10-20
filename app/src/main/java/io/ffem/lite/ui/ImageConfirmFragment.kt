package io.ffem.lite.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import kotlinx.android.synthetic.main.fragment_image_confirm.*

class ImageConfirmFragment : Fragment() {

    interface OnConfirmImageListener {
        fun onConfirmImage(action: Int)
    }

    var listener: OnConfirmImageListener? = null

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

}