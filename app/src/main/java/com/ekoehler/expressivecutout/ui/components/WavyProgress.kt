package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin


/**
 * This is a wavy progress bar like in Google's media player.
 * The wavy part covers the elapsed portion, the flat track covers the remaining
 * portion, and a small rounded vertical bar separates the two.
 */
@Composable
fun WavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    waveColor: Color = Color(0xFF4DB6AC),
    trackColor: Color = Color(0xFF90A4AE),
    isWavy: Boolean = true,
    waveFrequency: Float = 0.04f,
    strokeWidth: Dp = 4.dp,
    separatorWidth: Dp = 4.dp,
    separatorHeight: Dp = 16.dp,
    separatorGap: Dp = 4.dp,
    separatorColor: Color = waveColor
) {
    val amplitude: Dp = if (isWavy) 4.dp else 0.dp
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseShift"
    )

    val barHeight = maxOf(amplitude * 2 + strokeWidth, separatorHeight)

    Canvas(modifier = modifier.fillMaxWidth().height(barHeight)) {
        val width = size.width
        val centerY = size.height / 2f
        val strokePx = strokeWidth.toPx()
        val amplitudePx = amplitude.toPx()
        val sepPx = separatorWidth.toPx()
        val gapPx = separatorGap.toPx()
        val sepHalfHeight = separatorHeight.toPx() / 2f

        val separatorX = (width * progress.coerceIn(0f, 1f))
            .coerceIn(sepPx / 2f, width - sepPx / 2f)
        val waveEnd = separatorX - sepPx / 2f - gapPx
        val trackStart = separatorX + sepPx / 2f + gapPx

        // Remaining track
        if (trackStart < width) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }

        // Elapsed wavy progress
        if (waveEnd > 0f) {
            val path = Path()
            path.moveTo(0f, centerY + sin(-phaseShift) * amplitudePx)

            // Draw points across the width, stepping by 5px to save CPU cycles
            for (x in 5..waveEnd.toInt() step 5) {
                val y = centerY + sin(x * waveFrequency - phaseShift) * amplitudePx
                path.lineTo(x.toFloat(), y)
            }

            val endY = centerY + sin(waveEnd * waveFrequency - phaseShift) * amplitudePx
            path.lineTo(waveEnd, endY)

            drawPath(
                path = path,
                color = waveColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Rounded separator
        drawLine(
            color = separatorColor,
            start = Offset(separatorX, centerY - sepHalfHeight + sepPx / 2f),
            end = Offset(separatorX, centerY + sepHalfHeight - sepPx / 2f),
            strokeWidth = sepPx,
            cap = StrokeCap.Round
        )
    }
}
