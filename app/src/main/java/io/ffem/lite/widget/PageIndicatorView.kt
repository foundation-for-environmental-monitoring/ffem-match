package io.ffem.lite.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.ffem.lite.R
import kotlin.math.ceil

class PageIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val fillPaint: Paint = Paint()
    private val strokePaint: Paint
    private var distanceBetweenBullets = 34f
    private var bulletRadius = 6
    private var activeBulletRadius = 0f

    var pageCount = 0
        set(value) {
            field = value
            distanceBetweenBullets = 34f
            val scale = resources.displayMetrics.density
            if (scale <= 1.5) {
                distanceBetweenBullets = 24f
                bulletRadius = 4
            } else if (scale >= 3) {
                distanceBetweenBullets = 44f
                bulletRadius = 6
            }
            setActiveBulletSize(false, scale)
            if (pageCount > 13) {
                distanceBetweenBullets -= 14f
            } else if (pageCount > 11) {
                distanceBetweenBullets -= 10f
            }
            invalidate()
        }

    var activePage = 0
        set(value) {
            field = value
            invalidate()
        }

    var showDots = false
        set(value) {
            field = value
            val scale = resources.displayMetrics.density
            setActiveBulletSize(value, scale)
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            ceil(distanceBetweenBullets * pageCount.toDouble()).toInt(), heightMeasureSpec
        )
    }

    public override fun onDraw(canvas: Canvas) {
        if (pageCount > 1) {
            for (i in 0 until pageCount) {
                if (activePage == i) {
                    canvas.drawCircle(
                        distanceBetweenBullets * i + bulletRadius * 2, height / 2f,
                        activeBulletRadius, fillPaint
                    )
                } else {
                    if (showDots) {
                        canvas.drawCircle(
                            distanceBetweenBullets * i + bulletRadius * 2, height / 2f,
                            bulletRadius / 2f, fillPaint
                        )
                    } else {
                        canvas.drawCircle(
                            distanceBetweenBullets * i + bulletRadius * 2, height / 2f,
                            bulletRadius.toFloat(), strokePaint
                        )
                    }
                }
            }
        }
    }

    private fun setActiveBulletSize(dots: Boolean, scale: Float) {
        activeBulletRadius = if (scale <= 1.5) {
            bulletRadius * 1.7f
        } else if (scale >= 3) {
            bulletRadius * 2f
        } else {
            if (dots) {
                bulletRadius * 1.4f
            } else {
                bulletRadius * 1.8f
            }
        }
    }

    init {
        fillPaint.style = Paint.Style.FILL_AND_STROKE
        fillPaint.strokeWidth = 2f
        fillPaint.color = ContextCompat.getColor(context, R.color.white)
        fillPaint.isAntiAlias = true
        strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2f
        strokePaint.color = ContextCompat.getColor(context, R.color.white)
        strokePaint.isAntiAlias = true
    }
}