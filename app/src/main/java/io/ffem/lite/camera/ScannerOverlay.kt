package io.ffem.lite.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.ffem.lite.zxing.qrcode.detector.FinderPatternInfo


class ScannerOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var previewHeight: Int = 0

    private var patternInfo: FinderPatternInfo? = null

    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        try {
            patternInfo?.apply {
                val top = (width - previewHeight) / 2
                canvas.drawCircle(height - topLeft.y, topLeft.x - top.toFloat(), 10f, greenPaint)
                canvas.drawCircle(
                    height - bottomLeft.y,
                    bottomLeft.x - top.toFloat(),
                    10f,
                    greenPaint
                )
                canvas.drawCircle(height - topRight.y, topRight.x - top.toFloat(), 10f, greenPaint)
                canvas.drawCircle(
                    height - bottomRight.y,
                    bottomRight.x - top.toFloat(),
                    10f,
                    greenPaint
                )
            }
        } catch (e: Exception) {
        }
    }

    fun refreshOverlay(pattern: FinderPatternInfo?, height: Int) {
        patternInfo = pattern
        previewHeight = height
        invalidate()
    }
}