package io.ffem.lite.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import io.ffem.lite.model.Calibration

/**
 * Displays the swatches for the calibrated colors of the test.
 */
class SwatchView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var blockWidth = 0f
    private var lineHeight = 50f
    private val paintColor: Paint
    private var totalWidth = 0f
    private var calibrations: List<Calibration> = ArrayList()
    private val blackText: Paint = Paint()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var index = -1
        for (resultIndex in calibrations.indices) {
            if (calibrations.isNotEmpty()) {
                index += 1
                val colorCount = calibrations.size
                for (i in 0 until colorCount) {
                    val colorItem = calibrations[i]
                    paintColor.color = colorItem.color
                    var left = i * totalWidth
                    if (i > 0) {
                        left += MARGIN
                    }
                    canvas.drawRect(
                        left, MARGIN + index * lineHeight,
                        i * totalWidth + blockWidth, index * lineHeight + blockWidth / 2, paintColor
                    )
//                    if (index == calibrations.size - 1) {
//                        canvas.drawText(createValueString(colorItem.value.toFloat()),
//                                MARGIN + (i * totalWidth + blockWidth / 2),
//                                index * lineHeight + VAL_BAR_HEIGHT, blackText)
//                    }
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        if (measuredWidth != 0) {
            val width = measuredWidth - MARGIN * 2
            for (resultIndex in calibrations.indices) {
                if (calibrations.isNotEmpty()) {
                    val colorCount = calibrations.size.toFloat()
                    if (blockWidth == 0f) {
                        blockWidth = (width - (colorCount - 4) * gutterSize) / colorCount
                        lineHeight = blockWidth / 2 + VAL_BAR_HEIGHT
                    }
                }
            }
            totalWidth = gutterSize + blockWidth
        }
        val extraHeight = 0
        val lineCount = 1
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                (lineCount * lineHeight + extraHeight).toInt(),
                MeasureSpec.EXACTLY
            )
        )
    }

    fun setCalibrations(values: List<Calibration>) {
        calibrations = values
    }

    companion object {
        private const val VAL_BAR_HEIGHT = 10
        private const val TEXT_SIZE = 20
        private const val MARGIN = 10f
        private const val gutterSize = 5f
        fun createValueString(value: Float): String {
            return value.toString()
        }
    }

    /**
     * Displays the swatches for the calibrated colors of the test.
     */
    init {
        blackText.color = Color.BLACK
        blackText.textSize = TEXT_SIZE.toFloat()
        blackText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        blackText.textAlign = Paint.Align.CENTER
        paintColor = Paint()
        paintColor.style = Paint.Style.FILL
    }
}