package com.ekoehler.expressivecutout.swe

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Ink on a light fill. Same value the rest of the overlay uses; the pill is colour-exempt. */
private val INK_DARK = Color(0xFF0A0A0A)

/** Ink on a dark fill. */
private val INK_LIGHT = Color(0xFFF5F5F5)

/** Divisor that turns a percentage setting into a fraction. */
internal const val PERCENT = 100f

/** Extra room on a measured string so layout rounding and the real font never clip it. */
private const val MEASURE_SAFETY = 1.06f

/** Measures [text] at [sizeDp] in [typeface], in dp, with [MEASURE_SAFETY] on top. */
internal fun measureDp(text: String, sizeDp: Float, density: Float, typeface: Typeface, scaleX: Float = 1f): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizeDp * density
        this.typeface = typeface
        textScaleX = scaleX
    }
    return paint.measureText(text) / density * MEASURE_SAFETY
}

/** Dark ink on a light [fill], light ink on a dark one. */
internal fun inkOn(fill: Color): Color = if (fill.luminance() > 0.5f) INK_DARK else INK_LIGHT
