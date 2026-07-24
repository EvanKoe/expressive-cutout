package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Text colours for a dark fill; on a light fill we swap in a dark text colour (see contentColorFor).
private val PillTextColor = Color(0xFFF5F5F5)
private val PillTextColorDark = Color(0xFF0A0A0A)

// Material 3 expressive "emphasized" easing — cubic-bezier(0.2, 0.0, 0.0, 1.0).
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// Vertical spacing added around the action row on top of the chip height itself.
private const val ACTIONS_ROW_SPACING_DP = 14

/**
 * Extra height added to the expanded island when it shows action buttons, so the added row grows
 * downward instead of pushing the content up into the camera cutout: one chip row (at its configured
 * height) plus its spacing. The controller grows the host window by the same amount so it never clips.
 */
internal fun expandedActionsExtraDp(buttonHeightDp: Int): Int = buttonHeightDp + ACTIONS_ROW_SPACING_DP

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
    appearance: AppearanceSettings,
    showActions: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onActivate: () -> Unit,
    onAction: (IslandAction) -> Unit,
    onReply: (IslandAction, String) -> Unit,
    onReplyActiveChange: (Boolean) -> Unit,
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }
    val shownEvent = lastEvent

    // Keyed on the shown event so tapping persists during that event and resets for a new one.
    var tapExpanded by remember(shownEvent?.id) { mutableStateOf(shownEvent?.initiallyExpanded ?: false) }
    // The reply action currently being typed for, if any. Reset when the event changes.
    var replyingTo by remember(shownEvent?.id) { mutableStateOf<IslandAction?>(null) }
    val replying = replyingTo != null
    val isExpanded = forcedExpanded ?: tapExpanded
    val boopScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Tell the controller to make the window focusable (for the keyboard) and pause dismissal.
    LaunchedEffect(replying) { onReplyActiveChange(replying) }
    LaunchedEffect(isExpanded, event != null) {
        if (event != null) onExpandedChange(isExpanded)
    }
    // User-expanded (not the pinned preview) optionally collapses after the delay — never while
    // a reply is being typed.
    LaunchedEffect(tapExpanded, forcedExpanded, autoCollapse, autoCollapseMs, replying) {
        if (forcedExpanded == null && tapExpanded && autoCollapse && !replying) {
            delay(autoCollapseMs)
            tapExpanded = false
        }
    }

    // Only pad the height when the expanded island will actually render action chips.
    val hasActions = showActions && (shownEvent?.actions?.isNotEmpty() == true)
    val dims = if (isExpanded) expanded else collapsed
    val heightBonus = if (isExpanded && hasActions) {
        expandedActionsExtraDp(appearance.actionButtonHeightDp)
    } else {
        0
    }
    val spec = tween<Dp>(durationMillis = 220, easing = EmphasizedEasing)
    val width by animateDpAsState((displayWidthDp * dims.widthPercent / 100f).dp, spec, label = "islandWidth")
    val height by animateDpAsState((dims.heightDp + heightBonus).dp, spec, label = "islandHeight")
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
                        .pointerInput(forcedExpanded, isExpanded, replying, shownEvent?.id) {
                            if (forcedExpanded != null) return@pointerInput
                            detectTapGestures {
                                // While typing a reply, ignore taps on the surface itself.
                                if (replying) return@detectTapGestures
                                // Once expanded, tapping a notification opens its app (like tapping
                                // the real notification); anything else just toggles expand/collapse.
                                if (isExpanded && shownEvent?.contentIntent != null) {
                                    onActivate()
                                } else {
                                    tapExpanded = !tapExpanded
                                    scope.launch {
                                        boopScale.animateTo(1.02f, tween(durationMillis = 120, easing = EmphasizedEasing))
                                        boopScale.animateTo(1f, tween(durationMillis = 220, easing = EmphasizedEasing))
                                    }
                                }
                            }
                        },
                    shape = cornerShape(topLeft, topRight, bottomLeft, bottomRight),
                    appearance = appearance,
                ) {
                    Crossfade(targetState = isExpanded, animationSpec = tween(150), label = "islandContent") { showExpanded ->
                        shownEvent?.let { e ->
                            if (showExpanded) {
                                ExpandedContent(
                                    event = e,
                                    showActions = showActions,
                                    appearance = appearance,
                                    replyingTo = replyingTo,
                                    onAction = onAction,
                                    onStartReply = { replyingTo = it },
                                    onCancelReply = { replyingTo = null },
                                    onSendReply = { text ->
                                        replyingTo?.let { onReply(it, text) }
                                        replyingTo = null
                                    },
                                )
                            } else {
                                CollapsedContent(e, collapsed.heightDp)
                            }
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
    appearance: AppearanceSettings = AppearanceSettings(),
) {
    IslandSurface(
        modifier = Modifier.size(width, heightDp.dp),
        shape = cornerShape(
            topLeft = cornerTopLeftDp.dp,
            topRight = cornerTopRightDp.dp,
            bottomLeft = cornerBottomLeftDp.dp,
            bottomRight = cornerBottomRightDp.dp,
        ),
        appearance = appearance,
    ) {
        if (expanded) {
            ExpandedContent(
                event = event,
                showActions = true,
                appearance = appearance,
                replyingTo = null,
                onAction = {},
                onStartReply = {},
                onCancelReply = {},
                onSendReply = {},
            )
        } else {
            CollapsedContent(event, heightDp)
        }
    }
}

@Composable
private fun IslandSurface(
    modifier: Modifier,
    shape: Shape,
    appearance: AppearanceSettings,
    content: @Composable () -> Unit,
) {
    val background = appearance.backgroundColor.resolve()
    // Keep text/icons legible: dark ink on a light fill, otherwise the near-white default.
    val contentColor = if (background.luminance() > 0.5f) PillTextColorDark else PillTextColor
    val border = if (appearance.strokeEnabled) {
        BorderStroke(appearance.strokeWidthDp.dp, appearance.strokeColor.resolve())
    } else {
        null
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = background,
        contentColor = contentColor,
        shadowElevation = if (appearance.shadowEnabled) 6.dp else 0.dp,
        tonalElevation = 0.dp,
        border = border,
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
private fun ExpandedContent(
    event: IslandEvent,
    showActions: Boolean,
    appearance: AppearanceSettings,
    replyingTo: IslandAction?,
    onAction: (IslandAction) -> Unit,
    onStartReply: (IslandAction) -> Unit,
    onCancelReply: () -> Unit,
    onSendReply: (String) -> Unit,
) {
    // Content sits in the lower part of the card, leaving the top clear of the camera hole.
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.label,
                        color = LocalContentColor.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    event.detail?.let { detail ->
                        Text(
                            text = detail,
                            color = LocalContentColor.current.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            when {
                // Typing a reply: the input field replaces the chips until sent or cancelled.
                replyingTo != null -> ReplyRow(
                    hint = replyingTo.reply?.hint,
                    accent = event.accent,
                    // Fall back to the notification's own accent / a neutral tint when unset.
                    sendColor = appearance.sendButtonColor?.resolve() ?: event.accent,
                    cancelColor = appearance.cancelButtonColor?.resolve(),
                    inputStyle = appearance.replyInputStyle,
                    cancelOnLeft = appearance.cancelButtonOnLeft,
                    heightDp = appearance.actionButtonHeightDp,
                    onSend = onSendReply,
                    onCancel = onCancelReply,
                )
                // Action chips (at most three fit comfortably); a reply chip opens the input,
                // any other chip fires its action.
                showActions && event.actions.isNotEmpty() -> {
                    // Chip fill follows the configured colour, or the notification's accent when unset.
                    val chipFill = appearance.actionButtonColor?.resolve() ?: event.accent
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        event.actions.take(3).forEach { action ->
                            ActionChip(
                                action = action,
                                style = appearance.actionButtonStyle,
                                fill = chipFill,
                                heightDp = appearance.actionButtonHeightDp,
                                onClick = {
                                    if (action.reply != null) onStartReply(action) else onAction(action)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single action chip. [style] selects between the Material 3 Expressive and Material You looks;
 * [fill] is the base colour those looks derive their container/outline from.
 */
@Composable
private fun ActionChip(
    action: IslandAction,
    style: ActionButtonStyle,
    fill: Color,
    heightDp: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = when (style) {
        ActionButtonStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        else -> CircleShape
    }
    val container = when (style) {
        ActionButtonStyle.EXPRESSIVE_TONAL -> fill.copy(alpha = 0.22f)
        ActionButtonStyle.EXPRESSIVE_FILLED -> fill
        ActionButtonStyle.MATERIAL_YOU -> fill.copy(alpha = 0.16f)
        ActionButtonStyle.OUTLINED -> Color.Transparent
    }
    val content = when (style) {
        // A solid fill needs ink that contrasts with it; the rest sit on a translucent tint.
        ActionButtonStyle.EXPRESSIVE_FILLED -> if (fill.luminance() > 0.5f) PillTextColorDark else PillTextColor
        ActionButtonStyle.OUTLINED -> fill
        else -> LocalContentColor.current
    }
    val border = if (style == ActionButtonStyle.OUTLINED) {
        BorderStroke(1.5.dp, fill.copy(alpha = 0.7f))
    } else {
        null
    }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = shape,
        color = container,
        contentColor = content,
        border = border,
        modifier = Modifier
            .height(heightDp.dp)
            .pressScale(interaction),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = action.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
        }
    }
}

/**
 * The expressive "squish" on press: a springy scale-down that settles back with a little
 * bounce when released. Shared by the action chips and the reply buttons so every tap on the
 * island feels the same.
 */
@Composable
private fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    pressedScale: Float = 0.88f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * An inline reply field with send/cancel affordances; requests focus so the keyboard appears.
 * [inputStyle] shapes the text field (Expressive pill / Material You / Material 2), [cancelOnLeft]
 * moves the cancel button to the leading edge, and [heightDp] sizes the field and buttons.
 */
@Composable
private fun ReplyRow(
    hint: String?,
    accent: Color,
    sendColor: Color,
    cancelColor: Color?,
    inputStyle: ReplyInputStyle,
    cancelOnLeft: Boolean,
    heightDp: Int,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val send = { if (text.isNotBlank()) onSend(text.trim()) }

    val cancelInteraction = remember { MutableInteractionSource() }
    val sendInteraction = remember { MutableInteractionSource() }
    val fieldShape = when (inputStyle) {
        ReplyInputStyle.EXPRESSIVE -> CircleShape
        ReplyInputStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        ReplyInputStyle.MATERIAL_2 -> RoundedCornerShape(4.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Cancel can sit before the field (leading) or between the field and send (trailing).
        if (cancelOnLeft) {
            ReplyCancelButton(cancelColor, heightDp, cancelInteraction, onCancel)
        }
        ReplyField(
            modifier = Modifier.weight(1f),
            text = text,
            onValueChange = { text = it },
            hint = hint,
            accent = accent,
            shape = fieldShape,
            heightDp = heightDp,
            focusRequester = focusRequester,
            onSend = send,
        )
        if (!cancelOnLeft) {
            ReplyCancelButton(cancelColor, heightDp, cancelInteraction, onCancel)
        }
        ReplySendButton(sendColor, text.isNotBlank(), heightDp, sendInteraction, send)
    }
}

@Composable
private fun ReplyField(
    modifier: Modifier,
    text: String,
    onValueChange: (String) -> Unit,
    hint: String?,
    accent: Color,
    shape: Shape,
    heightDp: Int,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .clip(shape)
            .background(LocalContentColor.current.copy(alpha = 0.12f))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = LocalContentColor.current, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            decorationBox = { inner ->
                if (text.isEmpty() && !hint.isNullOrBlank()) {
                    Text(
                        text = hint,
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun ReplyCancelButton(
    cancelColor: Color?,
    heightDp: Int,
    interaction: MutableInteractionSource,
    onCancel: () -> Unit,
) {
    IconButton(
        onClick = onCancel,
        interactionSource = interaction,
        modifier = Modifier
            .size(heightDp.dp)
            .pressScale(interaction),
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Cancel reply",
            tint = cancelColor ?: LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ReplySendButton(
    sendColor: Color,
    enabled: Boolean,
    heightDp: Int,
    interaction: MutableInteractionSource,
    onSend: () -> Unit,
) {
    FilledIconButton(
        onClick = onSend,
        enabled = enabled,
        interactionSource = interaction,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = sendColor,
            contentColor = if (sendColor.luminance() > 0.5f) PillTextColorDark else PillTextColor,
            disabledContainerColor = LocalContentColor.current.copy(alpha = 0.12f),
            disabledContentColor = LocalContentColor.current.copy(alpha = 0.4f),
        ),
        modifier = Modifier
            .size(heightDp.dp)
            .pressScale(interaction),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Send reply",
            modifier = Modifier.size(22.dp),
        )
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
