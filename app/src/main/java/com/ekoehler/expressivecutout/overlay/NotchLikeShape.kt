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
 * The notch-like expanded design: a block flush with the top of its box, rounded by [topLeft] and
 * [topRight] at the top, whose lower corners are *concave ears* — [earLeft] and [earRight] curve
 * from the body's flat bottom edge down into the box's own sides, ending in a point on each side.
 * It is the screen's rounded corners turned inside out, so the content below reads as tucked under
 * the block.
 *
 * The ears live inside the box: its flat bottom edge sits `max(earLeft, earRight)` above the box's
 * bottom, and the island adds that much to its height and insets its content by it.
 */
internal data class NotchLikeShape(
    private val topLeft: Dp,
    private val topRight: Dp,
    private val earLeft: Dp,
    private val earRight: Dp,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(path(size, density))

    /** The outline, with every radius clamped so the path stays valid at any slider value. */
    private fun path(size: Size, density: Density): Path = with(density) {
        val width = size.width
        val height = size.height
        val maxEar = minOf(width / 2f, height / 2f)
        val el = earLeft.toPx().coerceIn(0f, maxEar)
        val er = earRight.toPx().coerceIn(0f, maxEar)
        val bodyBottom = height - maxOf(el, er)
        val maxTop = minOf(width / 2f, bodyBottom)
        val tl = topLeft.toPx().coerceIn(0f, maxTop)
        val tr = topRight.toPx().coerceIn(0f, maxTop)

        Path().apply {
            moveTo(0f, tl)
            arcTo(Rect(0f, 0f, 2f * tl, 2f * tl), 180f, 90f, false)
            lineTo(width - tr, 0f)
            arcTo(Rect(width - 2f * tr, 0f, width, 2f * tr), 270f, 90f, false)
            lineTo(width, bodyBottom + er)
            arcTo(Rect(width - 2f * er, bodyBottom, width, bodyBottom + 2f * er), 0f, -90f, false)
            lineTo(el, bodyBottom)
            arcTo(Rect(0f, bodyBottom, 2f * el, bodyBottom + 2f * el), 270f, -90f, false)
            close()
        }
    }
}
