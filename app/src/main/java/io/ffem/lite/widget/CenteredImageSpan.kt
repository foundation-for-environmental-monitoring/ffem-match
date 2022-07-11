package io.ffem.lite.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import java.lang.ref.WeakReference

//ref: https://stackoverflow.com/questions/25628258/align-text-around-imagespan-center-vertical
class CenteredImageSpan(context: Context?, drawableRes: Int) : ImageSpan(context!!, drawableRes) {
    private var mDrawableRef: WeakReference<Drawable?>? = null
    override fun getSize(
        paint: Paint, text: CharSequence,
        start: Int, end: Int,
        fontMetricsInt: FontMetricsInt?
    ): Int {
        val d = cachedDrawable
        val rect = d!!.bounds
        if (fontMetricsInt != null) {
            val fmPaint = paint.fontMetricsInt
            val fontHeight = fmPaint.descent - fmPaint.ascent
            val drHeight = rect.bottom - rect.top
            val centerY = fmPaint.ascent + fontHeight / 2
            fontMetricsInt.ascent = centerY - drHeight / 2
            fontMetricsInt.top = fontMetricsInt.ascent
            fontMetricsInt.bottom = centerY + drHeight / 2
            fontMetricsInt.descent = fontMetricsInt.bottom
        }
        return rect.right
    }

    override fun draw(
        canvas: Canvas, text: CharSequence, start: Int,
        end: Int, x: Float, top: Int, y: Int, bottom: Int,
        paint: Paint
    ) {
        val drawable = cachedDrawable
        canvas.save()
        val fmPaint = paint.fontMetricsInt
        val fontHeight = fmPaint.descent - fmPaint.ascent
        val centerY = y + fmPaint.descent - fontHeight / 2
        val transY = centerY - (drawable!!.bounds.bottom - drawable.bounds.top) / 2
        canvas.translate(x, transY.toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }

    // Redefined locally because it is a private member from DynamicDrawableSpan
    private val cachedDrawable: Drawable?
        get() {
            val wr = mDrawableRef
            var d: Drawable? = null
            if (wr != null) {
                d = wr.get()
            }
            if (d == null) {
                d = drawable
                mDrawableRef = WeakReference(d)
            }
            return d
        }
}