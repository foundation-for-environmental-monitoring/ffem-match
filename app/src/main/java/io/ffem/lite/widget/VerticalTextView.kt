package io.ffem.lite.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.ffem.lite.R

class VerticalTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(ContextCompat.getColor(context, R.color.colorAccent))
        canvas.translate(width.toFloat(), 0f)
        canvas.rotate(90f)
        canvas.translate(compoundPaddingLeft.toFloat(), extendedPaddingTop.toFloat())
        layout.draw(canvas)
    }
}