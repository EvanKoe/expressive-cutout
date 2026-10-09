package com.ekoehler.expressivecutout.overlay

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A notch: a shape hanging from the top edge of its box whose top corners flare *outwards* into
 * that edge by [flare] — the inverted, concave counterpart of a corner radius — while [bottomLeft]
 * and [bottomRight] round the bottom two corners as usual.
 *
 * The flare is drawn *inside* the box: the shape spans the full width at the very top and has
 * narrowed to the body by `flare` below it. The island compensates by making its box `2 * flare`
 * wider than the width the user configured and insetting its content by the same amount, so the
 * body always measures exactly what the width slider says and the ears hang off the sides.
 *
 * There is deliberately no top radius: once a corner flares outwards the junction between the top
 * edge and the body's side is already tangent, so there is nothing left to round. Callers morphing
 * between a notch and a plain pill fall back to [androidx.compose.foundation.shape.RoundedCornerShape]
 * at `flare == 0` — see `cornerShape` in DynamicIsland.
 */
data class NotchShape(
    private val flare: Dp,
    private val bottomLeft: Dp,
    private val bottomRight: Dp,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(path(size, density, includeTopEdge = true))

    /**
     * The notch's outline. [includeTopEdge] = false leaves out the segment lying on the screen's own
     * top edge and returns an open path running ear to ear the long way round, which is what the
     * island strokes — a line along that edge would read as a stray mark rather than part of the
     * shape.
     */
    fun path(size: Size, density: Density, includeTopEdge: Boolean): Path = with(density) {
        val w = size.width
        val h = size.height
        // An ear taller than the box, or wide enough for the two to meet, has no silhouette left to
        // draw; clamping keeps the path valid at any slider value on any island height.
        val f = flare.toPx().coerceIn(0f, minOf(w / 2f, h))
        val body = w - 2f * f
        val maxBottom = minOf(body / 2f, h - f).coerceAtLeast(0f)
        val bl = bottomLeft.toPx().coerceIn(0f, maxBottom)
        val br = bottomRight.toPx().coerceIn(0f, maxBottom)

        Path().apply {
            if (includeTopEdge) {
                moveTo(0f, 0f)
                lineTo(w, 0f)
            } else {
                moveTo(w, 0f)
            }
            arcTo(Rect(w - f, 0f, w + f, 2f * f), 270f, -90f, false)
            lineTo(w - f, h - br)
            arcTo(Rect(w - f - 2f * br, h - 2f * br, w - f, h), 0f, 90f, false)
            lineTo(f + bl, h)
            arcTo(Rect(f, h - 2f * bl, f + 2f * bl, h), 90f, 90f, false)
            lineTo(f, f)
            arcTo(Rect(-f, 0f, f, 2f * f), 0f, -90f, false)
            if (includeTopEdge) close()
        }
    }
}
