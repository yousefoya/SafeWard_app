package com.example.safeward.ui.patient

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.safeward.R
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Custom Canvas line chart for SafeWard vital signs (Heart Rate, SpO2, Temperature, Breathing).
 * Zero external dependencies so it works reliably offline in Android Studio.
 */
class VitalSignsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class DataPoint(val label: String, val value: Float)

    private var points: List<DataPoint> = emptyList()
    private var unit: String = "bpm"
    private var decimals: Int = 0
    private var lineColor: Int = ContextCompat.getColor(context, R.color.alert_critical_rose)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface_warm_linen)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_muted_slate)
        textSize = 28f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun setData(
        newPoints: List<DataPoint>,
        unit: String,
        colorRes: Int,
        decimals: Int = 0
    ) {
        this.points = newPoints
        this.unit = unit
        this.decimals = decimals
        this.lineColor = ContextCompat.getColor(context, colorRes)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val padLeft = 96f
        val padRight = 32f
        val padTop = 32f
        val padBottom = 56f

        val chartW = width - padLeft - padRight
        val chartH = height - padTop - padBottom
        if (chartW <= 0f || chartH <= 0f) return

        val rawMin = points.minOf { it.value }
        val rawMax = points.maxOf { it.value }
        val span = max(rawMax - rawMin, if (decimals > 0) 0.6f else 6f)
        val minVal = rawMin - span * 0.2f
        val maxVal = rawMax + span * 0.2f
        val range = max(maxVal - minVal, 0.1f)

        // Draw 3 horizontal grid lines (top, mid, bottom)
        for (i in 0..2) {
            val fraction = i / 2f
            val y = padTop + chartH * fraction
            canvas.drawLine(padLeft, y, padLeft + chartW, y, gridPaint)

            val valueAtLine = maxVal - range * fraction
            val formatted = if (decimals > 0) {
                String.format(Locale.US, "%.1f", valueAtLine)
            } else {
                valueAtLine.toInt().toString()
            }
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatted, padLeft - 14f, y + 10f, labelPaint)
        }

        // Compute coordinates
        val stepX = if (points.size > 1) chartW / (points.size - 1) else 0f
        val xs = FloatArray(points.size)
        val ys = FloatArray(points.size)
        for (i in points.indices) {
            xs[i] = if (points.size > 1) padLeft + i * stepX else padLeft + chartW / 2f
            val norm = (points[i].value - minVal) / range
            ys[i] = padTop + chartH - (norm * chartH)
        }

        // Build smooth line & fill paths
        linePath.reset()
        fillPath.reset()
        linePath.moveTo(xs[0], ys[0])
        fillPath.moveTo(xs[0], padTop + chartH)
        fillPath.lineTo(xs[0], ys[0])

        for (i in 1 until points.size) {
            val midX = (xs[i - 1] + xs[i]) / 2f
            linePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i])
            fillPath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i])
        }

        fillPath.lineTo(xs.last(), padTop + chartH)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, padTop, 0f, padTop + chartH,
            Color.argb(72, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
            Color.argb(8, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        linePaint.color = lineColor
        canvas.drawPath(linePath, linePaint)

        // Draw points & X labels
        dotPaint.color = lineColor
        labelPaint.textAlign = Paint.Align.CENTER
        val labelStep = max(1, points.size / 4)

        for (i in points.indices) {
            val radius = if (i == points.lastIndex) 11f else 7f
            canvas.drawCircle(xs[i], ys[i], radius, dotPaint)
            canvas.drawCircle(xs[i], ys[i], radius, dotBorderPaint)

            if (i % labelStep == 0 || i == points.lastIndex) {
                val clampedX = min(max(xs[i], padLeft + 24f), padLeft + chartW - 24f)
                canvas.drawText(points[i].label, clampedX, height - 14f, labelPaint)
            }
        }
    }
}
