package com.example.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var currentSpeed = 0f
    private var displayedSpeed = 0f
    private val maxSpeed = 180f

    private val startAngle = 135f
    private val sweepAngle = 270f

    private val arcRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 36f
        color = Color.parseColor("#22303A")
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 36f
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30")
        style = Paint.Style.FILL
    }

    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A2128")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8899AA")
        textAlign = Paint.Align.CENTER
    }

    private var animator: ValueAnimator? = null

    fun setSpeed(newSpeed: Float) {
        currentSpeed = newSpeed.coerceIn(0f, maxSpeed)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(displayedSpeed, currentSpeed).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                displayedSpeed = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 60f
        arcRect.set(padding, padding, w - padding, h - padding)
        textPaint.textSize = w * 0.16f
        unitPaint.textSize = w * 0.05f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f


        canvas.drawArc(arcRect, startAngle, sweepAngle, false, trackPaint)


        val speedpercent = displayedSpeed / maxSpeed
        progressPaint.color = when {
            speedpercent < 0.5f -> Color.parseColor("#34C759")
            speedpercent < 0.8f -> Color.parseColor("#FFCC00")
            else -> Color.parseColor("#FF3B30")
        }
        canvas.drawArc(arcRect, startAngle, sweepAngle * speedpercent, false, progressPaint)


        val needleAngle = startAngle + sweepAngle * speedpercent
        val needleAngleRad = Math.toRadians(needleAngle.toDouble())
        val needleLength = arcRect.width() / 2f - 20f
        val needleX = cx + needleLength * Math.cos(needleAngleRad).toFloat()
        val needleY = cy + needleLength * Math.sin(needleAngleRad).toFloat()
        canvas.drawLine(cx, cy, needleX, needleY, needlePaint.apply { strokeWidth = 8f; style = Paint.Style.STROKE })
        canvas.drawCircle(cx, cy, 18f, centerDotPaint)





        canvas.drawText(displayedSpeed.toInt().toString(), cx, cy + textPaint.textSize * 0.35f, textPaint)
        canvas.drawText("km/h", cx, cy + textPaint.textSize * 0.35f + unitPaint.textSize + 12f, unitPaint)
    }
}