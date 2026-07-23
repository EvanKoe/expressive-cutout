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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekoehler.expressivecutout.data.IslandDimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PillColor = Color(0xFF0A0A0A)
private val PillTextColor = Color(0xFFF5F5F5)
private val PillSecondaryColor = Color(0xB3F5F5F5)

// Material 3 expressive "emphasized" easing — cubic-bezier(0.2, 0.0, 0.0, 1.0).
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The interactive overlay island. The hosting window is a fixed size; the island's size,
 * position and corners are all animated here in Compose, so expand/collapse never resizes the
 * window (which caused per-frame relayout jank). Tapping toggles expanded; [forcedExpanded]
 * locks the state (used by the settings preview).
 */
@Composable
fun DynamicIsland(
    event: IslandEvent?,
    collapsed: IslandDimensions,
    expanded: IslandDimensions,
    displayWidthDp: Int,
    forcedExpanded: Boolean?,
    autoCollapse: Boolean,
    autoCollapseMs: Long,
    onExpandedChange: (Boolean) -> Unit,
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }
    val shownEvent = lastEvent

    // Keyed on the shown event so tapping persists during that event and resets for a new one.
    var tapExpanded by remember(shownEvent?.id) { mutableStateOf(shownEvent?.initiallyExpanded ?: false) }
    val isExpanded = forcedExpanded ?: tapExpanded
    val boopScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isExpanded, event != null) {
        if (event != null) onExpandedChange(isExpanded)
    }
    // User-expanded (not the pinned preview) optionally collapses after the delay.
    LaunchedEffect(tapExpanded, forcedExpanded, autoCollapse, autoCollapseMs) {
        if (forcedExpanded == null && tapExpanded && autoCollapse) {
            delay(autoCollapseMs)
            tapExpanded = false
        }
    }

    val dims = if (isExpanded) expanded else collapsed
    val spec = tween<Dp>(durationMillis = 220, easing = EmphasizedEasing)
    val width by animateDpAsState((displayWidthDp * dims.widthPercent / 100f).dp, spec, label = "islandWidth")
    val height by animateDpAsState(dims.heightDp.dp, spec, label = "islandHeight")
    val offsetX by animateDpAsState(dims.offsetXDp.dp, spec, label = "islandOffsetX")
    val offsetY by animateDpAsState(dims.offsetYDp.dp, spec, label = "islandOffsetY")
    val topLeft by animateDpAsState(dims.cornerTopLeftDp.dp, spec, label = "cornerTL")
    val topRight by animateDpAsState(dims.cornerTopRightDp.dp, spec, label = "cornerTR")
    val bottomLeft by animateDpAsState(dims.cornerBottomLeftDp.dp, spec, label = "cornerBL")
    val bottomRight by animateDpAsState(dims.cornerBottomRightDp.dp, spec, label = "cornerBR")

    Box(modifier = Modifier.fillMaxSize()) {
        // Position the island in the full-size (non-clipping) window; then animate visibility.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = offsetX, y = offsetY),
        ) {
            AnimatedVisibility(
                visible = event != null,
                enter = fadeIn(tween(150)) + scaleIn(tween(220, easing = EmphasizedEasing), initialScale = 0.85f),
                exit = fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.9f),
            ) {
                IslandSurface(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .graphicsLayer { this.scaleX = boopScale.value }
                        .pointerInput(forcedExpanded) {
                            if (forcedExpanded != null) return@pointerInput
                            detectTapGestures {
                                tapExpanded = !tapExpanded
                                scope.launch {
                                    boopScale.animateTo(1.02f, tween(durationMillis = 120, easing = EmphasizedEasing))
                                    boopScale.animateTo(1f, tween(durationMillis = 220, easing = EmphasizedEasing))
                                }
                            }
                        },
                    shape = cornerShape(topLeft, topRight, bottomLeft, bottomRight),
                ) {
                    Crossfade(targetState = isExpanded, animationSpec = tween(150), label = "islandContent") { showExpanded ->
                        shownEvent?.let { e ->
                            if (showExpanded) ExpandedContent(e) else CollapsedContent(e, collapsed.heightDp)
                        }
                    }
                }
            }
        }
    }
}

/** A static, non-interactive pill used by the settings screen for previewing one state. */
@Composable
fun IslandPreview(
    event: IslandEvent,
    width: Dp,
    heightDp: Int,
    cornerTopLeftDp: Int,
    cornerTopRightDp: Int,
    cornerBottomLeftDp: Int,
    cornerBottomRightDp: Int,
    expanded: Boolean,
) {
    IslandSurface(
        modifier = Modifier.size(width, heightDp.dp),
        shape = cornerShape(
            topLeft = cornerTopLeftDp.dp,
            topRight = cornerTopRightDp.dp,
            bottomLeft = cornerBottomLeftDp.dp,
            bottomRight = cornerBottomRightDp.dp,
        ),
    ) {
        if (expanded) ExpandedContent(event) else CollapsedContent(event, heightDp)
    }
}

@Composable
private fun IslandSurface(
    modifier: Modifier,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = PillColor,
        contentColor = PillTextColor,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        content = content,
    )
}

/** Builds a rounded shape with each corner independently sized (LTR-mapped). */
private fun cornerShape(topLeft: Dp, topRight: Dp, bottomLeft: Dp, bottomRight: Dp) =
    RoundedCornerShape(
        topStart = topLeft,
        topEnd = topRight,
        bottomStart = bottomLeft,
        bottomEnd = bottomRight,
    )

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
