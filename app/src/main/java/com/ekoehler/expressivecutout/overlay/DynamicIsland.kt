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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * The interactive overlay island. Collapsed it shows just the icon at the user's collapsed
 * size; tapping expands it to the user's expanded size (revealing label + detail) and tapping
 * again collapses it. Size and corner radius animate between the two states.
 *
 * When [forcedExpanded] is non-null the island is locked to that state and ignores taps — used
 * by the settings preview so the tab selection drives which state is shown.
 * [onExpandedChange] lets the host switch window position and pause auto-dismiss.
 */
@Composable
fun DynamicIsland(
    event: IslandEvent?,
    collapsed: IslandDimensions,
    expanded: IslandDimensions,
    forcedExpanded: Boolean?,
    autoCollapse: Boolean,
    autoCollapseMs: Long,
    onExpandedChange: (Boolean) -> Unit,
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }

    // The overlay window's width is set explicitly by the host, so the pill fills that width;
    // this composable only drives height, corner and content.
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        // Draw-only enter/exit (alpha + scale) so the window is relaid out only when the island
        // appears/disappears, not on every animation frame.
        AnimatedVisibility(
            visible = event != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(220, easing = EmphasizedEasing), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.9f),
        ) {
            lastEvent?.let {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    IslandContent(it, collapsed, expanded, forcedExpanded, autoCollapse, autoCollapseMs, onExpandedChange)
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
private fun IslandContent(
    event: IslandEvent,
    collapsed: IslandDimensions,
    expanded: IslandDimensions,
    forcedExpanded: Boolean?,
    autoCollapse: Boolean,
    autoCollapseMs: Long,
    onExpandedChange: (Boolean) -> Unit,
) {
    var tapExpanded by remember(event.id) { mutableStateOf(event.initiallyExpanded) }
    val isExpanded = forcedExpanded ?: tapExpanded
    val boopScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isExpanded) { onExpandedChange(isExpanded) }

    // When the user expands it (not the pinned settings preview), optionally collapse back to
    // the normal island after the configured delay. If auto-collapse is off it stays until the
    // user taps again.
    LaunchedEffect(tapExpanded, forcedExpanded, autoCollapse, autoCollapseMs) {
        if (forcedExpanded == null && tapExpanded && autoCollapse) {
            delay(autoCollapseMs)
            tapExpanded = false
        }
    }

    val dims = if (isExpanded) expanded else collapsed
    // Width is owned by the window (a percentage of the real display); the pill just fills it.
    val sizeSpec = tween<Dp>(durationMillis = 220, easing = EmphasizedEasing)
    val height by animateDpAsState(dims.heightDp.dp, sizeSpec, label = "islandHeight")
    val topLeft by animateDpAsState(dims.cornerTopLeftDp.dp, sizeSpec, label = "cornerTL")
    val topRight by animateDpAsState(dims.cornerTopRightDp.dp, sizeSpec, label = "cornerTR")
    val bottomLeft by animateDpAsState(dims.cornerBottomLeftDp.dp, sizeSpec, label = "cornerBL")
    val bottomRight by animateDpAsState(dims.cornerBottomRightDp.dp, sizeSpec, label = "cornerBR")

    IslandSurface(
        modifier = Modifier
            .fillMaxWidth()
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
            if (showExpanded) ExpandedContent(event) else CollapsedContent(event, collapsed.heightDp)
        }
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
