package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
 * The room [PermissionDotRow] needs on the trailing edge, so collapsed content that also sits there
 * can be shifted clear of it. Zero when there is nothing to draw.
 */
internal fun permissionDotRowWidthDp(usage: PermissionUsage, heightDp: Int): Int {
    if (usage.count == 0) return 0
    val dots = usage.count * heightDp * DOT_SIZE_FRACTION
    // Each dot carries half a gap on each side, so one gap per dot.
    val gaps = usage.count * heightDp * DOT_GAP_FRACTION
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
 * Each dot takes its colour from [colors], the user's own pick per resource.
 */
@Composable
internal fun PermissionDotRow(
    usage: PermissionUsage,
    colors: PermissionDotColors,
    heightDp: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Microphone
        PermissionDot(visible = usage.microphone, color = colors.microphone.resolve(), heightDp = heightDp)

        // Camera
        PermissionDot(visible = usage.camera, color = colors.camera.resolve(), heightDp = heightDp)

        // Location
        PermissionDot(visible = usage.location, color = colors.location.resolve(), heightDp = heightDp)
    }
}

/**
 * One dot, popping in and out with the resource it stands for while its slot — the dot plus half a
 * gap on each side — widens and narrows, which is what slides its neighbours.
 */
@Composable
private fun PermissionDot(visible: Boolean, color: Color, heightDp: Int) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.4f) + expandHorizontally(clip = false),
        exit = fadeOut() + scaleOut(targetScale = 0.4f) + shrinkHorizontally(clip = false),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = (heightDp * DOT_GAP_FRACTION / 2f).dp)
                .size((heightDp * DOT_SIZE_FRACTION).dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
