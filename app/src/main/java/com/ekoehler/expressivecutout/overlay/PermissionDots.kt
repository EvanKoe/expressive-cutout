package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.system.PermissionUsage
import kotlin.math.roundToInt

/**
 * The dot colours, one per watched resource. Fixed rather than themed for the same reason the pill's
 * own text is: these read against the user's chosen island background, not against the app's
 * surfaces, and their meaning is the colour — the system's own indicator trains people to read green
 * as the microphone.
 */
private val MICROPHONE_DOT_COLOR = Color(0xFF19C337)
private val CAMERA_DOT_COLOR = Color(0xFFE5484D)
private val LOCATION_DOT_COLOR = Color(0xFF3B82F6)

/** A dot's diameter and the gap between two of them, both as a fraction of the pill's height. */
private const val DOT_SIZE_FRACTION = 0.20f
private const val DOT_GAP_FRACTION = 0.14f

/**
 * The gap between the pill's icon and the first dot, as a fraction of the pill's height. Sized so
 * the dots sit clear of the icon but still well inside the camera hole when placed on the left.
 */
private const val DOT_LEADING_GAP_FRACTION = 0.25f

/**
 * Inset from the pill's trailing edge, as a fraction of its height. Matches the inset the timer's
 * remaining-time text and the progress ring already use on that edge.
 */
private const val DOT_TRAILING_INSET_FRACTION = 0.24f

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
    val gaps = (usage.count - 1) * heightDp * DOT_GAP_FRACTION
    return (dots + gaps + heightDp * DOT_GAP_FRACTION).roundToInt()
}

/**
 * The microphone / camera / location dots, drawn on the collapsed pill while an app is using that
 * resource. Sized to [heightDp] so they follow the user's own geometry, and each fades and scales in
 * on its own so a second resource lighting up doesn't restart the first dot's animation.
 */
@Composable
internal fun PermissionDotRow(usage: PermissionUsage, heightDp: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((heightDp * DOT_GAP_FRACTION).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Microphone
        PermissionDot(visible = usage.microphone, color = MICROPHONE_DOT_COLOR, heightDp = heightDp)

        // Camera
        PermissionDot(visible = usage.camera, color = CAMERA_DOT_COLOR, heightDp = heightDp)

        // Location
        PermissionDot(visible = usage.location, color = LOCATION_DOT_COLOR, heightDp = heightDp)
    }
}

/** One dot, popping in and out with the resource it stands for. */
@Composable
private fun PermissionDot(visible: Boolean, color: Color, heightDp: Int) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.4f),
        exit = fadeOut() + scaleOut(targetScale = 0.4f),
    ) {
        Box(
            modifier = Modifier
                .size((heightDp * DOT_SIZE_FRACTION).dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
