package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 expressive "emphasized" easing — cubic-bezier(0.2, 0.0, 0.0, 1.0). */
private val EMPHASIZED_EASING = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** How strongly the page behind the bottom bars is blurred; shared with [BackNavBar]. */
internal val BACKDROP_BLUR_RADIUS: Dp = 40.dp

/** Transparency for the navbar background color */
internal const val CONTAINER_ALPHA: Float = 0.2f

/** One destination in the [ExpressiveNavBar]. */
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * A Material 3 "expressive" style bottom bar: a floating rounded container whose selected
 * item animates into a filled pill that reveals its label. Purely presentational —
 * selection state and clicks are hoisted to the caller.
 */
@Composable
fun ExpressiveNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pr: Color = MaterialTheme.colorScheme.primary
    val bg: Color = Color(pr.red, pr.green, pr.blue, CONTAINER_ALPHA)
    val blurred = supportsBackdropBlur()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = if (blurred) Color.Transparent else pr,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 0.dp,
        shadowElevation = if (blurred) 0.dp else 6.dp,
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (blurred) {
                        Modifier.backdropBlur(color = bg, radius = BACKDROP_BLUR_RADIUS)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, item ->
                NavBarPill(
                    item = item,
                    selected = index == selectedIndex,
                    blurred = blurred,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

/**
 * One destination of the [ExpressiveNavBar]. Colours invert with [blurred]: a primary pill on the
 * frosted bar, or a surface pill on the solid primary bar used when backdrop blur is unavailable.
 */
@Composable
private fun NavBarPill(
    item: NavBarItem,
    selected: Boolean,
    blurred: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = when {
            !selected -> Color.Transparent
            blurred -> colors.primary
            else -> colors.surface
        },
        animationSpec = tween(durationMillis = 350, easing = EMPHASIZED_EASING),
        label = "navPillContainer",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            blurred && selected -> colors.surface
            blurred -> colors.onSurface
            selected -> colors.primary
            else -> colors.surface
        },
        animationSpec = tween(durationMillis = 350, easing = EMPHASIZED_EASING),
        label = "navPillContent",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(horizontal = if (selected) 18.dp else 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
        )

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(durationMillis = 300, easing = EMPHASIZED_EASING)) +
                expandHorizontally(tween(durationMillis = 350, easing = EMPHASIZED_EASING)),
            exit = fadeOut(tween(durationMillis = 150, easing = EMPHASIZED_EASING)) +
                shrinkHorizontally(tween(durationMillis = 250, easing = EMPHASIZED_EASING)),
        ) {
            Text(
                text = item.label,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
