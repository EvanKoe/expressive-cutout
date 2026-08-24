package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.data.PermissionDotColors
import com.ekoehler.expressivecutout.system.PermissionUsage
import kotlin.math.roundToInt

/** A dot's diameter and the gap between two of them, both as a fraction of the pill's height. */
private const val DOT_SIZE_FRACTION = 0.20f
private const val DOT_GAP_FRACTION = 0.14f

/**
 * The same two, for the stacked layout: three dots and their gaps have to share the pill's height
 * rather than its width, so both shrink to fit (3 × 0.15 + 3 × 0.06 = 0.63 of the height).
 */
private const val VERTICAL_DOT_SIZE_FRACTION = 0.15f
private const val VERTICAL_DOT_GAP_FRACTION = 0.06f

/**
 * The gap between the pill's icon and the first dot, as a fraction of the pill's height. Sized so
 * the dots sit clear of the icon but still well inside the camera hole when placed on the left.
 */
private const val DOT_LEADING_GAP_FRACTION = 0.25f

/**
 * Inset from the pill's trailing edge, as a fraction of its height. A little deeper than the inset
 * the timer's remaining-time text and the progress ring use, so a dot — much smaller than either —
 * doesn't read as crowding the edge.
 */
private const val DOT_TRAILING_INSET_FRACTION = 0.36f

/**
 * How far from the pill's leading edge the dots start when placed on the left: past the icon badge
 * ([CollapsedContent] insets it by 0.16 and sizes it at 0.72 of the height) plus a gap.
 */
internal fun permissionDotStartInsetDp(heightDp: Int): Float =
    heightDp * (0.16f + 0.72f + DOT_LEADING_GAP_FRACTION)

/** How far from the pill's trailing edge the dots start when placed on the right. */
internal fun permissionDotEndInsetDp(heightDp: Int): Float = heightDp * DOT_TRAILING_INSET_FRACTION

/**
 * The inset collapsed content (the timer's remaining time, the progress ring) already keeps from the
 * pill's trailing edge. Shared with [permissionDotTrailingInsetDp] so shifting that content clear of
 * the dots is measured against the same edge the dots are.
 */
internal const val COLLAPSED_TRAILING_INSET_FRACTION = 0.24f

/**
 * How much wider the collapsed pill has to be to hold the dots on its trailing edge: their own inset
 * plus the room they occupy plus a gap, less the inset trailing content already keeps. Zero when
 * there is nothing to draw.
 *
 * The pill grows to the right by this much rather than moving its content, so a tile that writes on
 * that edge (the timer's remaining time, a progress ring) stays exactly where it was and the dots
 * land in the new space. Collapsed content is inset by the same amount, which is what keeps it put
 * inside the wider pill.
 */
internal fun permissionDotTrailingInsetDp(
    usage: PermissionUsage,
    heightDp: Int,
    vertical: Boolean = false,
): Int {
    if (usage.count == 0) return 0
    val room = permissionDotEndInsetDp(heightDp) +
        permissionDotRowWidthDp(usage, heightDp, vertical) +
        heightDp * DOT_GAP_FRACTION
    return (room - heightDp * COLLAPSED_TRAILING_INSET_FRACTION).coerceAtLeast(0f).roundToInt()
}

/**
 * The room [PermissionDotRow] needs on the trailing edge, so collapsed content that also sits there
 * can be shifted clear of it. Zero when there is nothing to draw.
 */
internal fun permissionDotRowWidthDp(usage: PermissionUsage, heightDp: Int, vertical: Boolean = false): Int {
    if (usage.count == 0) return 0
    // Stacked, the dots are only ever one wide however many are lit — and a smaller one at that.
    val count = if (vertical) 1 else usage.count
    val sizeFraction = if (vertical) VERTICAL_DOT_SIZE_FRACTION else DOT_SIZE_FRACTION
    val gapFraction = if (vertical) VERTICAL_DOT_GAP_FRACTION else DOT_GAP_FRACTION
    val dots = count * heightDp * sizeFraction
    // Each dot carries half a gap on each side, so one gap per dot.
    val gaps = count * heightDp * gapFraction
    return (dots + gaps).roundToInt()
}

/**
 * The microphone / camera / location dots, drawn on the collapsed pill while an app is using that
 * resource. Sized to [heightDp] so they follow the user's own geometry, and each fades and scales in
 * on its own so a second resource lighting up doesn't restart the first dot's animation.
 *
 * A dot also expands the row's width as it appears, which slides the dots already there along
 * instead of jumping them aside. The gap between dots is carried as each dot's own padding rather
 * than by the row's arrangement, so the space a dot needs grows with it — an arrangement gap would
 * pop into place the moment the dot became visible.
 *
 * Each dot takes its colour from [colors], the user's own pick per resource. With [vertical] the
 * same dots stack into a column instead, which keeps the row's footprint one dot wide however many
 * resources are lit — worth it on a narrow pill, at the cost of the dots being smaller than the
 * height they share.
 */
@Composable
internal fun PermissionDotRow(
    usage: PermissionUsage,
    colors: PermissionDotColors,
    heightDp: Int,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    // Microphone, camera, location — declared once so both directions draw the same three dots.
    val dots: @Composable () -> Unit = {
        PermissionDot(usage.microphone, colors.microphone.resolve(), heightDp, vertical)
        PermissionDot(usage.camera, colors.camera.resolve(), heightDp, vertical)
        PermissionDot(usage.location, colors.location.resolve(), heightDp, vertical)
    }

    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            dots()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dots()
        }
    }
}

/**
 * One dot, popping in and out with the resource it stands for while its slot — the dot plus half a
 * gap on either side — grows and shrinks along the stack's own axis, which is what slides its
 * neighbours instead of jumping them aside.
 *
 * Stacked dots are scaled down to [VERTICAL_DOT_SIZE_FRACTION] so three of them plus their gaps
 * still fit inside the pill's height.
 */
@Composable
private fun PermissionDot(visible: Boolean, color: Color, heightDp: Int, vertical: Boolean) {
    val sizeFraction = if (vertical) VERTICAL_DOT_SIZE_FRACTION else DOT_SIZE_FRACTION
    val gapFraction = if (vertical) VERTICAL_DOT_GAP_FRACTION else DOT_GAP_FRACTION
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.4f) +
            if (vertical) expandVertically(clip = false) else expandHorizontally(clip = false),
        exit = fadeOut() + scaleOut(targetScale = 0.4f) +
            if (vertical) shrinkVertically(clip = false) else shrinkHorizontally(clip = false),
    ) {
        val gap = (heightDp * gapFraction / 2f).dp
        Box(
            modifier = Modifier
                .then(
                    if (vertical) Modifier.padding(vertical = gap)
                    else Modifier.padding(horizontal = gap)
                )
                .size((heightDp * sizeFraction).dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
