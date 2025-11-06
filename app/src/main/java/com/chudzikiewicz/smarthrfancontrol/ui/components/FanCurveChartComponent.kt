/*
 * SmartHR FanControl
 * Copyright (C) [2025] [Marcin Chudzikiewicz]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.chudzikiewicz.smarthrfancontrol.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chudzikiewicz.smarthrfancontrol.core.preferences.AlgorithmSettings
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt


data class ChartPoint(val hr: Int, val speed: Int)

private fun calculateNonLinearSpeedDouble(hr: Double, settings: AlgorithmSettings): Double {
    val hrRange = (settings.maxHr - settings.minHr).coerceAtLeast(1)

    if (hr <= settings.minHr) return settings.minSpeed.toDouble()
    if (hr >= settings.maxHr) return settings.maxSpeed.toDouble()

    val hrProgress = (hr - settings.minHr) / hrRange
    val nonLinearProgress = hrProgress.pow(settings.exponent.coerceAtLeast(0.01))
    val speedRange = (settings.maxSpeed - settings.minSpeed).toDouble()
    val calculatedSpeed = settings.minSpeed + (nonLinearProgress * speedRange)

    return calculatedSpeed.coerceIn(settings.minSpeed.toDouble(), settings.maxSpeed.toDouble())
}

fun generateChartPoints(settings: AlgorithmSettings): List<ChartPoint> {
    val effectiveMinHr = settings.minHr
    val effectiveMaxHr = settings.maxHr.coerceAtLeast(effectiveMinHr + 1)
    val hrRange = effectiveMaxHr - effectiveMinHr

    if (hrRange <= 0) return emptyList()

    val points = mutableListOf<ChartPoint>()
    val hrStep = (hrRange / 20.0).roundToInt().coerceIn(1, hrRange)
    val hrStartPoint = 50
    val hrEndAxis = 200

    points.add(ChartPoint(hrStartPoint, settings.minSpeed))

    if (effectiveMinHr > hrStartPoint) {
        points.add(ChartPoint(effectiveMinHr, settings.minSpeed))
    }

    val curveStartHr = max(effectiveMinHr, hrStartPoint)
    for (hr in (curveStartHr + hrStep)..effectiveMaxHr step hrStep) {
        val calculatedSpeed = calculateNonLinearSpeedDouble(hr.toDouble(), settings)
        points.add(ChartPoint(hr, calculatedSpeed.roundToInt()))
    }

    if (points.lastOrNull()?.hr != effectiveMaxHr) {
        points.add(ChartPoint(effectiveMaxHr, settings.maxSpeed))
    }

    points.add(ChartPoint(hrEndAxis, settings.maxSpeed))

    return points.distinct()
}

@Composable
fun FanCurveChart(chartPoints: List<ChartPoint>, settings: AlgorithmSettings) {

    val hrAxisMin = 50f
    val hrAxisMax = 200f
    val speedAxisMin = 0f
    val speedAxisMax = 100f

    val chartSize = 300.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = Color.LightGray.copy(alpha = 0.5f)
    val axisTextColor = Color.Gray

    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = TextStyle(color = axisTextColor, fontSize = 10.sp)
    val labelTextStyle =
        TextStyle(color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)

    val dottedLinePathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartSize)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {

                val paddingStart = 48.dp.toPx()
                val paddingBottom = 40.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingEnd = 16.dp.toPx()

                val chartWidth = size.width - paddingStart - paddingEnd
                val chartHeight = size.height - paddingBottom - paddingTop

                val hrRange = (hrAxisMax - hrAxisMin)
                val speedRange = (speedAxisMax - speedAxisMin)

                val scaleX = chartWidth / hrRange
                val scaleY = chartHeight / speedRange

                // Rysowanie Linii Siatki

                // 1. Poziome Linii Siatki
                for (speedValue in 0..100 step 10) {
                    val yPos = paddingTop + chartHeight - (speedValue * scaleY)
                    drawLine(
                        gridColor,
                        start = Offset(paddingStart, yPos),
                        end = Offset(paddingStart + chartWidth, yPos),
                        strokeWidth = 1f,
                        pathEffect = if (speedValue == 0) null else dottedLinePathEffect
                    )
                    if (speedValue > 0) {
                        val textLayout = textMeasurer.measure("$speedValue", style = axisTextStyle)
                        drawText(
                            textLayout,
                            topLeft = Offset(
                                paddingStart - textLayout.size.width - 4.dp.toPx(),
                                yPos - textLayout.size.height / 2
                            )
                        )
                    }
                }

                // 2. Pionowe Linii Siatki
                for (hrValue in 50..200 step 10) {
                    val xPos = paddingStart + ((hrValue - hrAxisMin) * scaleX)
                    drawLine(
                        gridColor,
                        start = Offset(xPos, paddingTop + chartHeight),
                        end = Offset(xPos, paddingTop),
                        strokeWidth = 1f,
                        pathEffect = if (hrValue == 50) null else dottedLinePathEffect
                    )
                    if (hrValue > 50) {
                        val textLayout = textMeasurer.measure("$hrValue", style = axisTextStyle)
                        drawText(
                            textLayout,
                            topLeft = Offset(
                                xPos - textLayout.size.width / 2,
                                paddingTop + chartHeight + 4.dp.toPx()
                            )
                        )
                    }
                }

                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.clipRect(
                        paddingStart,
                        paddingTop,
                        paddingStart + chartWidth,
                        paddingTop + chartHeight
                    )

                    val path = Path()
                    chartPoints.firstOrNull()?.let { firstPoint ->
                        val x = paddingStart + ((firstPoint.hr - hrAxisMin) * scaleX)
                        val y =
                            paddingTop + chartHeight - ((firstPoint.speed - speedAxisMin) * scaleY)
                        path.moveTo(x, y)
                    }
                    chartPoints.drop(1).forEach { point ->
                        val x = paddingStart + ((point.hr - hrAxisMin) * scaleX)
                        val y = paddingTop + chartHeight - ((point.speed - speedAxisMin) * scaleY)
                        path.lineTo(x, y)
                    }
                    drawPath(path, primaryColor, style = Stroke(width = 4f))
                    canvas.restore()
                }

                val minHrPointX = paddingStart + ((settings.minHr - hrAxisMin) * scaleX)
                val minHrPointY =
                    paddingTop + chartHeight - ((settings.minSpeed - speedAxisMin) * scaleY)
                drawCircle(primaryColor, radius = 5f, center = Offset(minHrPointX, minHrPointY))

                val maxHrPointX = paddingStart + ((settings.maxHr - hrAxisMin) * scaleX)
                val maxHrPointY =
                    paddingTop + chartHeight - ((settings.maxSpeed - speedAxisMin) * scaleY)
                drawCircle(primaryColor, radius = 5f, center = Offset(maxHrPointX, maxHrPointY))

                val yMinSpeed = paddingTop + chartHeight - (settings.minSpeed * scaleY)
                drawLine(
                    Color.Green.copy(alpha = 0.5f),
                    start = Offset(paddingStart, yMinSpeed),
                    end = Offset(paddingStart + chartWidth, yMinSpeed),
                    strokeWidth = 2f
                )

                val yMaxSpeed = paddingTop + chartHeight - (settings.maxSpeed * scaleY)
                drawLine(
                    Color.Red.copy(alpha = 0.5f),
                    start = Offset(paddingStart, yMaxSpeed),
                    end = Offset(paddingStart + chartWidth, yMaxSpeed),
                    strokeWidth = 2f
                )


                val minSpeedText =
                    textMeasurer.measure("${settings.minSpeed}", style = labelTextStyle)
                drawText(
                    minSpeedText,
                    topLeft = Offset(
                        paddingStart - minSpeedText.size.width - 4.dp.toPx(),
                        yMinSpeed - minSpeedText.size.height / 2
                    )
                )

                val maxSpeedText =
                    textMeasurer.measure("${settings.maxSpeed}", style = labelTextStyle)
                drawText(
                    maxSpeedText,
                    topLeft = Offset(
                        paddingStart - maxSpeedText.size.width - 4.dp.toPx(),
                        yMaxSpeed - maxSpeedText.size.height / 2
                    )
                )

                val minHrText = textMeasurer.measure("${settings.minHr}", style = labelTextStyle)
                drawText(
                    minHrText,
                    topLeft = Offset(
                        minHrPointX - minHrText.size.width / 2,
                        paddingTop + chartHeight + 4.dp.toPx()
                    )
                )

                val maxHrText = textMeasurer.measure("${settings.maxHr}", style = labelTextStyle)
                drawText(
                    maxHrText,
                    topLeft = Offset(
                        maxHrPointX - maxHrText.size.width / 2,
                        paddingTop + chartHeight + 4.dp.toPx()
                    )
                )

                val xAxisTitle = textMeasurer.measure(
                    "HR (BPM)",
                    style = axisTextStyle.copy(fontWeight = FontWeight.Bold)
                )
                drawText(
                    xAxisTitle,
                    topLeft = Offset(
                        paddingStart + (chartWidth / 2f) - (xAxisTitle.size.width / 2f),
                        size.height - xAxisTitle.size.height
                    )
                )

                drawIntoCanvas {
                    val yAxisPaint = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 11.sp.toPx()
                        textAlign = Paint.Align.CENTER
                    }
                    val x = 16.dp.toPx()
                    val y = paddingTop + chartHeight / 2f
                    it.nativeCanvas.save()
                    it.nativeCanvas.rotate(-90f, x, y)
                    it.nativeCanvas.drawText("Fan Speed (%)", x, y, yAxisPaint)
                    it.nativeCanvas.restore()
                }
            }
        }
    }
}