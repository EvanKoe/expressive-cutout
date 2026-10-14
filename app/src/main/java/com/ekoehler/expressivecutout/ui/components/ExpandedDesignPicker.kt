package com.ekoehler.expressivecutout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.ExpandedDesign

/**
 * Two side-by-side choices for how the expanded cutout looks: the floating island or the notch-like
 * block merged into the top edge. Stateless — selection is hoisted to the caller.
 */
@Composable
fun ExpandedDesignPicker(
    selected: ExpandedDesign,
    onSelect: (ExpandedDesign) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Island design
        DesignOption(
            design = ExpandedDesign.ISLAND,
            label = stringResource(R.string.expanded_design_island),
            selected = selected == ExpandedDesign.ISLAND,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )

        // Notch-like design
        DesignOption(
            design = ExpandedDesign.NOTCH_LIKE,
            label = stringResource(R.string.expanded_design_notch),
            selected = selected == ExpandedDesign.NOTCH_LIKE,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One radio-style choice: an illustration of [design] above its [label], filled when [selected]. */
@Composable
private fun DesignOption(
    design: ExpandedDesign,
    label: String,
    selected: Boolean,
    onSelect: (ExpandedDesign) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val body = scheme.onSecondaryContainer
    val inner = scheme.primary
    val surface = scheme.secondaryContainer
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OPTION_CORNER_DP.dp))
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(design) })
            .padding(OPTION_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Illustration
        Canvas(
            modifier = Modifier
                .fillMaxWidth(ILLUSTRATION_WIDTH_FRACTION)
                .aspectRatio(VIEW_WIDTH / VIEW_HEIGHT),
        ) {
            val scale = size.width / VIEW_WIDTH
            if (design == ExpandedDesign.ISLAND) drawIsland(scale, body, inner, surface)
            else drawNotchLike(scale, body, inner, surface)
        }

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) scheme.primary else scheme.onSurface,
        )
    }
}

/** The phone-top silhouette shared by both SVGs, in viewBox units scaled by [scale]. */
private fun phoneTopPath(scale: Float, bottom: Float): Path = Path().apply {
    moveTo(0f, PHONE_CORNER * scale)
    cubicTo(0f, 77f * scale, 77f * scale, 0f, PHONE_CORNER * scale, 0f)
    lineTo((VIEW_WIDTH - PHONE_CORNER) * scale, 0f)
    cubicTo(821f * scale, 0f, VIEW_WIDTH * scale, 77f * scale, VIEW_WIDTH * scale, PHONE_CORNER * scale)
    lineTo(VIEW_WIDTH * scale, bottom * scale)
    lineTo(0f, bottom * scale)
    close()
}

/** Draws the island variant: an arch outline with a floating card above a row of app squares. */
private fun DrawScope.drawIsland(scale: Float, body: Color, inner: Color, surface: Color) {
    val phone = phoneTopPath(scale, VIEW_HEIGHT)
    drawPath(phone, surface)
    clipPath(phone) {
        drawPath(phone, body, style = Stroke(width = OUTLINE_STROKE * scale))
    }
    fillRoundRect(111f, 126f, 675f, 351f, 57f, scale, body)
    drawCardContent(scale, inner)
    drawAppSquares(scale, inner)
}

/** Draws the notch-like variant: a full-width block flush with the top and concave ears below it. */
private fun DrawScope.drawNotchLike(scale: Float, body: Color, inner: Color, surface: Color) {
    val phone = phoneTopPath(scale, VIEW_HEIGHT)
    drawPath(phone, surface)
    clipPath(phone) {
        drawPath(phone, body, style = Stroke(width = OUTLINE_STROKE * scale))
        drawPath(phoneTopPath(scale, BLOCK_BOTTOM), body)
    }
    drawEar(scale, body, leftSide = true)
    drawEar(scale, body, leftSide = false)
    drawCardContent(scale, inner)
    drawAppSquares(scale, inner)
}

/** Draws one concave ear hanging below the notch block, on the left or right edge. */
private fun DrawScope.drawEar(scale: Float, color: Color, leftSide: Boolean) {
    val edge = if (leftSide) EAR_EDGE else VIEW_WIDTH - EAR_EDGE
    val reach = if (leftSide) EAR_REACH else -EAR_REACH
    val path = Path().apply {
        moveTo(edge * scale, BLOCK_BOTTOM * scale)
        lineTo(edge * scale, EAR_BOTTOM * scale)
        cubicTo(
            (edge + reach * 0.05f) * scale, 532f * scale,
            (edge + reach * 0.45f) * scale, (BLOCK_BOTTOM + 5f) * scale,
            (edge + reach) * scale, BLOCK_BOTTOM * scale,
        )
        close()
    }
    drawPath(path, color)
}

/** Draws the avatar, two text bars and two pill buttons that both illustrations share. */
private fun DrawScope.drawCardContent(scale: Float, color: Color) {
    fillRoundRect(142f, 204f, 107f, 107f, 53.5f, scale, color)
    fillRoundRect(273f, 225f, 288f, 18f, 9f, scale, color)
    fillRoundRect(273f, 269f, 231f, 18f, 9f, scale, color)
    fillRoundRect(142f, 374f, 288f, 72f, 36f, scale, color)
    fillRoundRect(465f, 374f, 288f, 72f, 36f, scale, color)
}

/** Draws the row of four rounded app squares below the card. */
private fun DrawScope.drawAppSquares(scale: Float, color: Color) {
    listOf(111f, 296f, 480f, 665f).forEach { x ->
        fillRoundRect(x, 569f, 121f, 115f, 30f, scale, color)
    }
}

/** Fills a rounded rectangle given in viewBox units, scaled by [scale]. */
private fun DrawScope.fillRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, scale: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x * scale, y * scale),
        size = Size(w * scale, h * scale),
        cornerRadius = CornerRadius(r * scale),
    )
}

private const val VIEW_WIDTH = 898f
private const val VIEW_HEIGHT = 748f
private const val PHONE_CORNER = 172f
private const val OUTLINE_STROKE = 54f
private const val BLOCK_BOTTOM = 505f
private const val EAR_EDGE = 25f
private const val EAR_REACH = 56f
private const val EAR_BOTTOM = 561f
private const val OPTION_CORNER_DP = 28
private const val OPTION_PADDING_DP = 12

/** How much of an option's width its illustration takes, so the pictures stay small beside the label. */
private const val ILLUSTRATION_WIDTH_FRACTION = 0.55f
