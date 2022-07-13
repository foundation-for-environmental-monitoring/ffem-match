package io.ffem.lite.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CircleView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        canvas.drawCircle(w / 2f, h / 2f, RADIUS.toFloat(), circlePaint)
        super.onDraw(canvas)
    }

    companion object {
        private const val RADIUS = 40
    }

    init {
        circlePaint.color = Color.YELLOW
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeWidth = 5f
    }
}