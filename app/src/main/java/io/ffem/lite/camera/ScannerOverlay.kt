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

    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var topMargin: Int = 0

    private var patternInfo: FinderPatternInfo? = null

    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        try {
            patternInfo?.apply {

                // left point
                canvas.drawCircle(
                    (previewWidth * (height - bottomLeft.y)) / height,
                    (previewHeight * bottomLeft.x / width) - topMargin,
                    10f,
                    greenPaint
                )

                // top right
                canvas.drawCircle(
                    (previewWidth * (height - topLeft.y)) / height,
                    (previewHeight * topLeft.x / width) - topMargin,
                    10f,
                    greenPaint
                )

                // bottom right
                canvas.drawCircle(
                    (previewWidth * (height - topRight.y)) / height,
                    (previewHeight * topRight.x / width) - topMargin,
                    10f,
                    greenPaint
                )

                // bottom left
                canvas.drawCircle(
                    (previewWidth * (height - bottomRight.y)) / height,
                    (previewHeight * bottomRight.x / width) - topMargin,
                    10f,
                    greenPaint
                )
            }
        } catch (e: Exception) {
        }
    }

    fun refreshOverlay(pattern: FinderPatternInfo?, width: Int, height: Int, marginTop: Int) {
        patternInfo = pattern
        previewWidth = width
        previewHeight = height
        topMargin = marginTop
        invalidate()
    }
}