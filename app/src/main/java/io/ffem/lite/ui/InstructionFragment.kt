package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import kotlinx.android.synthetic.main.fragment_instruction.*

class InstructionFragment : Fragment() {

    interface OnStartTestListener {
        fun onStartTest()
    }

    var listener: OnStartTestListener? = null

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
            listener?.onStartTest()
        }
    }

}