package io.ffem.lite.widget

import android.content.Context
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import io.ffem.lite.R

/**
 * A single numbered row for a list.
 */
class InstructionRowView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : TableRow(context, attrs) {
    private val textNumber: TextView
    private val textPara: TextView
    fun setNumber(s: String?) {
        textNumber.text = s
    }

    fun append(s: Spanned?) {
        textPara.append(s)
    }

    val string: String
        get() = textPara.text.toString()

    fun enableLinks() {
        textPara.movementMethod = LinkMovementMethod.getInstance()
    }

    init {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.instruction_item, this, true)
        val tableRow = getChildAt(0) as TableRow
        textNumber = tableRow.getChildAt(0) as TextView
        textPara = tableRow.getChildAt(1) as TextView
    }
}