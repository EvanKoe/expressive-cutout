package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val PillColor = Color(0xFF0A0A0A)
private val PillTextColor = Color(0xFFF5F5F5)
private val PillSecondaryColor = Color(0xB3F5F5F5)

private val ExpandedWidth = 324.dp
private val ExpandedHeight = 108.dp

// Material 3 expressive "emphasized" easing — cubic-bezier(0.2, 0.0, 0.0, 1.0).
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The interactive overlay island. Collapsed it is an icon-only pill sized to the user's
 * dimensions; tapping it expands into a larger card that reveals the label and detail, and
 * tapping again collapses it. [onExpandedChange] lets the host pause its auto-dismiss while
 * expanded.
 */
@Composable
fun DynamicIsland(
    event: IslandEvent?,
    widthDp: Int,
    heightDp: Int,
    onExpandedChange: (Boolean) -> Unit,
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }

    Box(contentAlignment = Alignment.TopCenter) {
        // Draw-only enter/exit (alpha + graphicsLayer scale). Crucially there is no
        // layout-size animation here, so the wrap-content overlay window is relaid out only
        // when the island appears/disappears — not on every animation frame.
        AnimatedVisibility(
            visible = event != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(220, easing = EmphasizedEasing), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.9f),
        ) {
            // Margin (only present while shown) so the shadow and tap "boop" aren't clipped
            // by the wrap-content overlay window.
            lastEvent?.let {
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    IslandContent(it, widthDp, heightDp, onExpandedChange)
                }
            }
        }
    }
}

/** A static, non-interactive collapsed pill used by the settings screen for previewing. */
@Composable
fun IslandPreview(event: IslandEvent, widthDp: Int, heightDp: Int) {
    IslandSurface(
        modifier = Modifier.size(widthDp.dp, heightDp.dp),
        cornerRadius = (heightDp / 2).dp,
    ) {
        CollapsedContent(event, heightDp)
    }
}

@Composable
private fun IslandContent(
    event: IslandEvent,
    collapsedWidthDp: Int,
    collapsedHeightDp: Int,
    onExpandedChange: (Boolean) -> Unit,
) {
    var expanded by remember(event.id) { mutableStateOf(false) }
    val boopScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(expanded) { onExpandedChange(expanded) }

    // A short, non-bouncy tween keeps the number of per-frame window relayouts during the
    // (occasional, user-initiated) expand/collapse small.
    val sizeSpec = tween<Dp>(durationMillis = 220, easing = EmphasizedEasing)
    val width by animateDpAsState(if (expanded) ExpandedWidth else collapsedWidthDp.dp, sizeSpec, label = "islandWidth")
    val height by animateDpAsState(if (expanded) ExpandedHeight else collapsedHeightDp.dp, sizeSpec, label = "islandHeight")
    val corner by animateDpAsState(if (expanded) 30.dp else (collapsedHeightDp / 2).dp, sizeSpec, label = "islandCorner")

    IslandSurface(
        modifier = Modifier
            .size(width, height)
            .graphicsLayer { this.scaleX = boopScale.value }
            .pointerInput(Unit) {
                detectTapGestures {
                    expanded = !expanded
                    scope.launch {
                        // A small "boop" acknowledges the tap using the emphasized easing.
                        boopScale.animateTo(1.02f, tween(durationMillis = 120, easing = EmphasizedEasing))
                        boopScale.animateTo(1f, tween(durationMillis = 220, easing = EmphasizedEasing))
                    }
                }
            },
        cornerRadius = corner,
    ) {
        Crossfade(targetState = expanded, animationSpec = tween(150), label = "islandContent") { isExpanded ->
            if (isExpanded) {
                ExpandedContent(event)
            } else {
                CollapsedContent(event, collapsedHeightDp)
            }
        }
    }
}

@Composable
private fun IslandSurface(
    modifier: Modifier,
    cornerRadius: Dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = PillColor,
        contentColor = PillTextColor,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
private fun CollapsedContent(event: IslandEvent, heightDp: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconBadge(
            event = event,
            badgeSize = (heightDp * 0.72f).dp,
            iconSize = (heightDp * 0.46f).dp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (heightDp * 0.16f).dp),
        )
    }
}

@Composable
private fun ExpandedContent(event: IslandEvent) {
    // Content sits in the lower part of the card, leaving the top clear of the camera hole.
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.label,
                    color = PillTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.detail?.let { detail ->
                    Text(
                        text = detail,
                        color = PillSecondaryColor,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconBadge(
    event: IslandEvent,
    badgeSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(event.accent.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        when (val icon = event.icon) {
            is IslandIcon.Vector -> Icon(
                imageVector = icon.image,
                contentDescription = null,
                tint = event.accent,
                modifier = Modifier.size(iconSize),
            )

            is IslandIcon.Raster -> androidx.compose.foundation.Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(badgeSize * 0.78f).clip(CircleShape),
            )
        }
    }
}
