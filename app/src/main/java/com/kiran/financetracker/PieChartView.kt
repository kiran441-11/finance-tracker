package com.kiran.financetracker

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.roundToInt

class PieChartView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var values = listOf<Double>()
    private var labels = listOf<String>()
    private var colors = listOf<Int>()
    private var centerText = ""

    fun setChartData(
        newValues: List<Double>,
        newLabels: List<String>,
        newColors: List<Int>,
        text: String
    ) {
        values = newValues
        labels = newLabels
        colors = newColors
        centerText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (values.isEmpty() || values.sum() <= 0) {
            paint.color = Color.GRAY
            paint.textSize = 32f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("No data", width / 2f, height / 2f, paint)
            return
        }

        val total = values.sum()
        val size = minOf(width, height) * 0.62f
        val left = width / 2f - size / 2f
        val top = height / 2f - size / 2f
        val right = width / 2f + size / 2f
        val bottom = height / 2f + size / 2f

        val rectangle = RectF(left, top, right, bottom)
        var startAngle = -90f

        values.forEachIndexed { index, value ->
            val sweep = ((value / total) * 360f).toFloat()
            paint.style = Paint.Style.FILL
            paint.color = colors[index % colors.size]
            canvas.drawArc(rectangle, startAngle, sweep, true, paint)
            startAngle += sweep
        }

        paint.color = Color.WHITE
        canvas.drawCircle(width / 2f, height / 2f, size * 0.28f, paint)

        paint.color = Color.rgb(45, 55, 70)
        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(centerText, width / 2f, height / 2f + 10f, paint)
        paint.typeface = Typeface.DEFAULT
    }
}
