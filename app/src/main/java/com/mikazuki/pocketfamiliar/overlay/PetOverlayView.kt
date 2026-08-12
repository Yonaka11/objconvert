package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

class PetOverlayView(context: Context) : View(context) {
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(184, 167, 255)
        style = Paint.Style.FILL
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(141, 204, 255)
        style = Paint.Style.FILL
    }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(50, 33, 95)
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 159, 190)
        style = Paint.Style.FILL
    }
    private val smilePaint = Paint(facePaint).apply {
        style = Paint.Style.STROKE
    }
    private val smilePath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val minDimension = min(width, height)
        smilePaint.strokeWidth = minDimension * 0.025f

        canvas.drawOval(
            width * 0.18f,
            height * 0.24f,
            width * 0.82f,
            height * 0.86f,
            bodyPaint,
        )
        canvas.drawCircle(width * 0.36f, height * 0.28f, minDimension * 0.16f, accentPaint)
        canvas.drawCircle(width * 0.64f, height * 0.28f, minDimension * 0.16f, accentPaint)
        canvas.drawCircle(width * 0.4f, height * 0.5f, minDimension * 0.035f, facePaint)
        canvas.drawCircle(width * 0.6f, height * 0.5f, minDimension * 0.035f, facePaint)
        canvas.drawCircle(width * 0.32f, height * 0.6f, minDimension * 0.045f, blushPaint)
        canvas.drawCircle(width * 0.68f, height * 0.6f, minDimension * 0.045f, blushPaint)

        smilePath.reset()
        smilePath.apply {
            moveTo(width * 0.42f, height * 0.61f)
            quadTo(width * 0.5f, height * 0.68f, width * 0.58f, height * 0.61f)
        }
        canvas.drawPath(smilePath, smilePaint)
    }
}
