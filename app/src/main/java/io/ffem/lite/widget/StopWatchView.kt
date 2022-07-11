package io.ffem.lite.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import io.ffem.lite.R
import io.ffem.lite.common.Constants.GET_READY_SECONDS
import kotlin.math.abs

/**
 * Countdown timer view.
 * based on: https://github.com/maxwellforest/blog_android_timer
 */
class StopWatchView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    View(context, attrs, defStyleAttr) {
    private val circlePaint: Paint
    private val arcPaint: Paint
    private val rectangle = Rect()
    private val eraserPaint: Paint
    private val textPaint: Paint
    private val circleBackgroundPaint: Paint
    private val subTextPaint: Paint
    private var bitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var circleOuterBounds: RectF? = null
    private var circleInnerBounds: RectF? = null
    private var circleSweepAngle = -1f
    private var circleFinishAngle = -1f
    private var mProgress = 100

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec) // Trick to make the view square
    }

    override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        if (w != oldWidth || h != oldHeight) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap!!.eraseColor(Color.TRANSPARENT)
            mCanvas = Canvas(bitmap!!)
        }
        super.onSizeChanged(w, h, oldWidth, oldHeight)
        updateBounds()
    }

    override fun onDraw(canvas: Canvas) {
        mCanvas!!.drawColor(0, PorterDuff.Mode.CLEAR)
        if (circleSweepAngle > -1) {
            val text: String = mProgress.toString()
            textPaint.getTextBounds(text, 0, text.length, rectangle)
            mCanvas!!.drawArc(
                circleOuterBounds!!,
                ARC_START_ANGLE.toFloat(),
                360f,
                true,
                circleBackgroundPaint
            )

            if (circleSweepAngle < 361) {
                if (circleSweepAngle > circleFinishAngle) {
                    mCanvas!!.drawArc(
                        circleOuterBounds!!,
                        ARC_START_ANGLE.toFloat(),
                        circleSweepAngle,
                        true,
                        circlePaint
                    )
                    mCanvas!!.drawArc(
                        circleOuterBounds!!,
                        ARC_START_ANGLE.toFloat(),
                        circleFinishAngle,
                        true,
                        arcPaint
                    )
                } else {
                    mCanvas!!.drawArc(
                        circleOuterBounds!!,
                        ARC_START_ANGLE.toFloat(),
                        circleSweepAngle,
                        true,
                        arcPaint
                    )
                }
            }

            mCanvas!!.drawOval(circleInnerBounds!!, eraserPaint)
            var w = textPaint.measureText(text)
            mCanvas!!.drawText(
                text, (width - w) / 2f,
                (height + abs(rectangle.height())) / 2f - 10, textPaint
            )
            val mainTextHeight = rectangle.height()
            val subText = context.getString(R.string.seconds)
            w = subTextPaint.measureText(subText)
            subTextPaint.getTextBounds(subText, 0, subText.length, rectangle)
            mCanvas!!.drawText(
                subText,
                (width - w) / 2f,
                (height + abs(rectangle.height())) / 2f + mainTextHeight - 10,
                subTextPaint
            )
        }
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    fun setProgress(progress: Int, max: Int) {
        mProgress = progress
        drawProgress(progress.toFloat(), max.toFloat())
    }

    private fun drawProgress(progress: Float, max: Float) {
        circleSweepAngle = progress * 360 / max
        circleFinishAngle = GET_READY_SECONDS * 360 / max
        invalidate()
    }

    private fun updateBounds() {
        val thickness = width * THICKNESS_SCALE
        circleOuterBounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        circleInnerBounds = RectF(
            circleOuterBounds!!.left + thickness,
            circleOuterBounds!!.top + thickness,
            circleOuterBounds!!.right - thickness,
            circleOuterBounds!!.bottom - thickness
        )
        invalidate()
    }

    companion object {
        private val BACKGROUND_COLOR = Color.argb(120, 180, 180, 200)
        private val ERASES_COLOR = Color.argb(180, 40, 40, 40)
        private val FINISH_ARC_COLOR = Color.argb(255, 0, 245, 120)
        private const val ARC_START_ANGLE = 270 // 12 o'clock
        private const val THICKNESS_SCALE = 0.1f
    }

    init {
        var circleColor = Color.RED
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.StopWatchView)
            circleColor = ta.getColor(R.styleable.StopWatchView_circleColor, circleColor)
            ta.recycle()
        }
        textPaint = Paint()
        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        val typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.typeface = typeface
        textPaint.textSize = resources.getDimensionPixelSize(R.dimen.progressTextSize).toFloat()
        subTextPaint = Paint()
        subTextPaint.isAntiAlias = true
        subTextPaint.color = Color.LTGRAY
        val subTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        subTextPaint.typeface = subTypeface
        subTextPaint.textSize =
            resources.getDimensionPixelSize(R.dimen.progressSubTextSize).toFloat()
        circleBackgroundPaint = Paint()
        circleBackgroundPaint.isAntiAlias = true
        circleBackgroundPaint.color = BACKGROUND_COLOR
        circlePaint = Paint()
        circlePaint.isAntiAlias = true
        circlePaint.color = circleColor
        arcPaint = Paint()
        arcPaint.isAntiAlias = true
        arcPaint.color = FINISH_ARC_COLOR
        eraserPaint = Paint()
        eraserPaint.isAntiAlias = true
        eraserPaint.color = ERASES_COLOR
    }
}