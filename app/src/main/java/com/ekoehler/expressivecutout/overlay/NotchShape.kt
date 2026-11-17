package com.ekoehler.expressivecutout.overlay

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Which screen edge a [NotchShape] hangs from. [TOP] is the ordinary portrait notch; [LEFT] and
 * [RIGHT] are the same shape turned a quarter turn, for the island stuck to the camera down the side
 * of a landscape screen.
 */
enum class NotchEdge { TOP, LEFT, RIGHT }

/**
 * A notch: a shape hanging from one edge of its box whose two corners on that edge flare *outwards*
 * into it by [flare] — the inverted, concave counterpart of a corner radius — while [bottomLeft] and
 * [bottomRight] round the far two corners as usual.
 *
 * The flare is drawn *inside* the box: the shape spans the full length of [edge] where it meets it,
 * and has narrowed to the body by `flare` further in. The island compensates by making its box
 * `2 * flare` longer along that edge and insetting its content by the same amount, so the body
 * always measures exactly what the width slider says and the ears hang off the sides.
 *
 * There is deliberately no radius on the flared corners: once a corner flares outwards the junction
 * between the edge and the body's side is already tangent, so there is nothing left to round.
 * Callers morphing between a notch and a plain pill fall back to
 * [androidx.compose.foundation.shape.RoundedCornerShape] at `flare == 0` — see `cornerShape` in
 * DynamicIsland.
 */
data class NotchShape(
    private val flare: Dp,
    private val bottomLeft: Dp,
    private val bottomRight: Dp,
    private val edge: NotchEdge = NotchEdge.TOP,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(path(size, density, includeFlatEdge = true))

    /**
     * The notch's outline. [includeFlatEdge] = false leaves out the segment lying on the screen's own
     * edge and returns an open path running ear to ear the long way round, which is what the island
     * strokes — a line along that edge would read as a stray mark rather than part of the shape.
     */
    fun path(size: Size, density: Density, includeFlatEdge: Boolean): Path {
        // Built with the flat edge always on top, then turned onto whichever edge the island is
        // actually pinned to. Laying the geometry out three times over would be the same shape three
        // chances to drift apart.
        val sideways = edge != NotchEdge.TOP
        val span = if (sideways) size.height else size.width
        val depth = if (sideways) size.width else size.height
        return topEdgedPath(span, depth, density, includeFlatEdge).apply {
            if (sideways) transform(quarterTurn(size))
        }
    }

    /** The outline with its flat edge along the top of a [span] by [depth] box. */
    private fun topEdgedPath(span: Float, depth: Float, density: Density, includeFlatEdge: Boolean) =
        with(density) {
            // An ear deeper than the box, or wide enough for the two to meet, has no silhouette left
            // to draw; clamping keeps the path valid at any slider value on any island height.
            val f = flare.toPx().coerceIn(0f, minOf(span / 2f, depth))
            val body = span - 2f * f
            val maxFar = minOf(body / 2f, depth - f).coerceAtLeast(0f)
            val bl = bottomLeft.toPx().coerceIn(0f, maxFar)
            val br = bottomRight.toPx().coerceIn(0f, maxFar)

            Path().apply {
                if (includeFlatEdge) {
                    moveTo(0f, 0f)
                    lineTo(span, 0f)
                } else {
                    moveTo(span, 0f)
                }
                arcTo(Rect(span - f, 0f, span + f, 2f * f), 270f, -90f, false)
                lineTo(span - f, depth - br)
                arcTo(Rect(span - f - 2f * br, depth - 2f * br, span - f, depth), 0f, 90f, false)
                lineTo(f + bl, depth)
                arcTo(Rect(f, depth - 2f * bl, f + 2f * bl, depth), 90f, 90f, false)
                lineTo(f, f)
                arcTo(Rect(-f, 0f, f, 2f * f), 0f, -90f, false)
                if (includeFlatEdge) close()
            }
        }

    /** Turns a top-edged path onto the left or right edge of a [size] box. */
    private fun quarterTurn(size: Size) = Matrix().apply {
        if (edge == NotchEdge.LEFT) {
            translate(0f, size.height)
            rotateZ(-90f)
        } else {
            translate(size.width, 0f)
            rotateZ(90f)
        }
    }
}
