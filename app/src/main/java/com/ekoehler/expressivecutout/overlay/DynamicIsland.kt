package com.ekoehler.expressivecutout.overlay

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import com.ekoehler.expressivecutout.core.DynamicTile
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.MediaArtBus
import com.ekoehler.expressivecutout.core.MediaProgress
import com.ekoehler.expressivecutout.core.NowPlaying
import com.ekoehler.expressivecutout.core.NowPlayingBus
import com.ekoehler.expressivecutout.core.OnCall
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimerBus
import com.ekoehler.expressivecutout.core.TorchStateBus
import com.ekoehler.expressivecutout.data.ActionButtonAlignment
import com.ekoehler.expressivecutout.data.ActionButtonAnimation
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.SentAlignment
import com.ekoehler.expressivecutout.data.AnimationBounce
import com.ekoehler.expressivecutout.data.AnimationSpeed
import com.ekoehler.expressivecutout.data.AnimationStyle
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.CenterShortcut
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.CALL_MAX_WIDTH_PERCENT
import com.ekoehler.expressivecutout.data.CALL_MIN_WIDTH_PERCENT
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.asCallCutout
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.data.SwipeDismissDirection
import com.ekoehler.expressivecutout.data.SwipeDismissTarget
import com.ekoehler.expressivecutout.service.ProgressData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Text colours for a dark fill; on a light fill we swap in a dark text colour (see contentColorFor).
private val PillTextColor = Color(0xFFF5F5F5)
private val PillTextColorDark = Color(0xFF0A0A0A)

/** Fallback fill for a button asked to be [MusicButtonStyle.filled] before the user picks a colour. */
private val MusicButtonFilledDefault = Color(0xFFE0E0E0)

// Vertical spacing added around the action row on top of the chip height itself.
private const val ACTIONS_ROW_SPACING_DP = 14

// How far the island must be dragged upward before a swipe-up collapses it.
private const val SWIPE_UP_SHRINK_THRESHOLD_DP = 24

// How far the island must be dragged sideways before releasing dismisses it.
private const val SWIPE_DISMISS_THRESHOLD_DP = 90

// How long the "reply sent" confirmation stays on screen before the reply is dispatched.
private const val REPLY_SENT_FEEDBACK_MS = 900L

// Time for the rotating album art to complete one full turn.
private const val ALBUM_SPIN_MS = 8000

// The optional ring around the album cover, as fractions of the cover's own footprint: the stroke
// itself, then the breathing space between it and the artwork. Kept proportional so the ring reads
// the same on the collapsed pill and in the (larger) expanded layout.
private const val ALBUM_STROKE_FRACTION = 0.055f
private const val ALBUM_STROKE_GAP_FRACTION = 0.06f

// How often the music progress bar re-reads its own clock. The media session pushes nothing between
// real changes, so this is the bar's only motion; a whole bar width is a track long, which makes
// even a half-second step sub-pixel.
private const val PROGRESS_TICK_MS = 500L

// The tuned baseline for the island's primary expand/collapse transition. Every tween-based
// animation is expressed relative to this, so the user's single "animation duration" knob scales
// them all in proportion (see `animScale` in DynamicIsland). Its default equals this value.
private const val BASE_TRANSITION_MS = IslandMotion.BASE_TRANSITION_MS

/**
 * Extra height added to the expanded island when it shows action buttons, so the added row grows
 * downward instead of pushing the content up into the camera cutout: one chip row (at its configured
 * height) plus its spacing. The controller grows the host window by the same amount so it never clips.
 */
internal fun expandedActionsExtraDp(buttonHeightDp: Int): Int = buttonHeightDp + ACTIONS_ROW_SPACING_DP

/**
 * A safe upper bound on the height the expanded "center" claims below the base expanded cutout. The
 * visible island fits its measured content exactly (see the height-bonus logic in [DynamicIsland]);
 * the controller reserves this for the host window and touchable region so they never clip the
 * tallest (labels-on) layout — the window being a touch taller than the content is invisible.
 */
internal const val CENTER_SHORTCUTS_EXTRA_DP = 135

// Gap between the camera cutout (cleared by a collapsed-pill-height band at the top) and the center's
// content, used when fitting the island height to its measured shortcut row.
private const val CENTER_TOP_GAP_DP = 8

/**
 * Maps the configured chip placement onto the [Row] arrangement that positions the chip row.
 * [ActionButtonAlignment.FULL] stretches the chips with weight rather than positioning them, so it
 * falls back to leading here (the arrangement is irrelevant once the chips fill the whole width).
 */
internal fun ActionButtonAlignment.toHorizontal(): Alignment.Horizontal = when (this) {
    ActionButtonAlignment.LEFT, ActionButtonAlignment.FULL -> Alignment.Start
    ActionButtonAlignment.CENTER -> Alignment.CenterHorizontally
    ActionButtonAlignment.RIGHT -> Alignment.End
}

/** Maps the configured "Sent" confirmation placement onto its [Row] arrangement. */
internal fun SentAlignment.toHorizontal(): Alignment.Horizontal = when (this) {
    SentAlignment.LEFT -> Alignment.Start
    SentAlignment.CENTER -> Alignment.CenterHorizontally
    SentAlignment.RIGHT -> Alignment.End
}

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
    isStickToCamera: Boolean = false,
    isRotation270: Boolean = false,
    offsetYDp: Int = 6,
    animationStyle: AnimationStyle,
    animationSpeed: AnimationSpeed,
    animationBounce: AnimationBounce,
    actionButtonAnimation: ActionButtonAnimation,
    animationDurationMs: Int,
    autoCollapse: Boolean,
    autoCollapseMs: Long,
    appearance: AppearanceSettings,
    showActions: Boolean,
    shrinkOnSwipeUp: Boolean,
    swipeToDismiss: Boolean,
    swipeDismissDirection: SwipeDismissDirection,
    swipeDismissTarget: SwipeDismissTarget,
    showsWhenEmpty: Boolean,
    emptyIcon: IconSource? = null,
    emptyIconColor: CutoutColor? = null,
    emptyOpensCenter: Boolean = false,
    centerShortcuts: List<CenterShortcut> = emptyList(),
    centerShowLabels: Boolean = true,
    centerFillContainers: Boolean = false,
    centerThemedIcons: Boolean = false,
    vibrateOnTap: Boolean = true,
    onEmptyClick: () -> Unit = {},
    onCenterShortcut: (CenterShortcut) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit,
    onActivate: () -> Unit,
    onAction: (IslandAction) -> Unit,
    onReply: (IslandAction, String) -> Unit,
    onReplyActiveChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }

    val shownEvent = lastEvent
    val emptyPill = event == null && showsWhenEmpty

    val initialExpandedState = if (forcedExpanded == false) false else (shownEvent?.initiallyExpanded ?: false)
    var tapExpanded by remember(shownEvent?.id, forcedExpanded) { mutableStateOf(initialExpandedState) }
    var centerInteraction by remember { mutableStateOf(0) }
    var replyingTo by remember(shownEvent?.id) { mutableStateOf<IslandAction?>(null) }
    val replying = replyingTo != null
    var sentReply by remember(shownEvent?.id) { mutableStateOf<Pair<IslandAction, String>?>(null) }
    val confirmingSent = sentReply != null
    val isCall = shownEvent?.call != null
    val isAssistantNormalOnly = shownEvent?.assistant != null && !shownEvent.assistant.displayAnswerInCutout
    val isNormalOnly = isCall || isAssistantNormalOnly || shownEvent?.normalOnly == true
    val centerExpanded = emptyPill && emptyOpensCenter && tapExpanded
    val isExpanded = when {
        forcedExpanded == false -> false
        emptyPill -> centerExpanded
        isNormalOnly -> false
        else -> forcedExpanded ?: tapExpanded
    }

    val boopScale = remember { Animatable(1f) }
    val pressExpand = remember { Animatable(0f) }
    val pressWidens = actionButtonAnimation == ActionButtonAnimation.EXPAND
    val dismissOffsetX = remember(shownEvent?.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val animScale = animationDurationMs / BASE_TRANSITION_MS.toFloat()
    fun scaled(baseMs: Int) = (baseMs * animScale).roundToInt()

    val motion = remember(animationStyle, animationSpeed, animationBounce, animationDurationMs) {
        IslandMotion(animationStyle, animationSpeed, animationBounce, animationDurationMs)
    }

    LaunchedEffect(replying) { onReplyActiveChange(replying) }
    LaunchedEffect(isExpanded, event != null, emptyPill, emptyOpensCenter) {
        if (event != null || (emptyPill && emptyOpensCenter)) onExpandedChange(isExpanded)
    }

    LaunchedEffect(tapExpanded, forcedExpanded, autoCollapse, autoCollapseMs, replying, confirmingSent, centerInteraction) {
        if (forcedExpanded == null && tapExpanded && autoCollapse && !replying && !confirmingSent) {
            delay(autoCollapseMs)
            tapExpanded = false
        }
    }

    val hasActions = showActions && (shownEvent?.actions?.isNotEmpty() == true)
    val hasMediaControls = shownEvent?.media?.showControls == true
    val hasCallActions = shownEvent?.call?.showActions == true && (shownEvent?.actions?.isNotEmpty() == true)
    val hasTimerActions = shownEvent?.timer?.showActions == true && (shownEvent?.actions?.isNotEmpty() == true)
    val liveCall by OnCallBus.state.collectAsStateWithLifecycle()
    val callIncoming = isCall && liveCall?.ongoing == false
    val callTwoRow = callIncoming && shownEvent?.call?.incomingExpandedLayout == true && hasCallActions
    val callTrailingButtons = when {
        !isCall || !hasCallActions -> 0
        callTwoRow -> 0
        callIncoming -> 2
        else -> 1
    }

    val density = LocalDensity.current.density
    val callWidthPercent = remember(isCall, shownEvent?.label, callTrailingButtons, callIncoming, displayWidthDp, density) {
        if (isCall) {
            callCutoutWidthPercent(shownEvent.label, callTrailingButtons, callIncoming, displayWidthDp, density)
        } else {
            CALL_MIN_WIDTH_PERCENT
        }
    }

    val dims = when {
        emptyPill && !isExpanded -> collapsed
        callTwoRow -> expanded
        isCall -> collapsed.asCallCutout(callWidthPercent)
        isExpanded -> expanded
        else -> collapsed
    }

    var assistantContentHeightDp by remember(shownEvent?.assistant != null) { mutableStateOf(0) }
    var centerContentHeightDp by remember { mutableStateOf(0) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    val heightBonus = when {
        emptyPill && isExpanded -> {
            if (centerContentHeightDp > 0) {
                collapsed.heightDp + CENTER_TOP_GAP_DP + centerContentHeightDp - dims.heightDp
            } else {
                CENTER_SHORTCUTS_EXTRA_DP
            }
        }
        emptyPill -> 0
        isExpanded && shownEvent?.assistant != null && shownEvent.assistant.displayAnswerInCutout -> {
            val maxCutoutHeightDp = (screenHeightDp * shownEvent.assistant.maxCutoutHeightPercent / 100)
            val fitHeightDp = if (assistantContentHeightDp > 0) assistantContentHeightDp else 110
            val targetHeightDp = fitHeightDp.coerceIn(110, maxCutoutHeightDp)
            (targetHeightDp - dims.heightDp)
        }
        isExpanded && (hasActions || hasMediaControls || hasCallActions || hasTimerActions) ->
            expandedActionsExtraDp(appearance.actionButtonHeightDp)
        callTwoRow -> callIncomingExtraDp()
        else -> 0
    }

    val present = event != null || showsWhenEmpty
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(present) {
        reveal.animateTo(
            targetValue = if (present) 1f else 0f,
            animationSpec = motion.float(baseMs = if (present) 320 else 200),
        )
    }

    LaunchedEffect(emptyPill) {
        if (emptyPill) {
            tapExpanded = false
            if (dismissOffsetX.value != 0f) {
                reveal.snapTo(0f)
                dismissOffsetX.snapTo(0f)
                reveal.animateTo(1f, animationSpec = motion.float(baseMs = 320))
            }
        }
    }

    val spec: AnimationSpec<Dp> = if (reveal.value == 0f) snap() else motion.dp()
    val isAssistantAnswer = isExpanded && shownEvent?.assistant?.displayAnswerInCutout == true
    val heightSpec: AnimationSpec<Dp> = when {
        reveal.value == 0f -> snap()
        isAssistantAnswer -> motion.dpSmooth()
        else -> spec
    }

    val width by animateDpAsState(
        if (isStickToCamera) collapsed.heightDp.dp else (displayWidthDp * dims.widthPercent / 100f).dp,
        spec, label = "islandWidth"
    )

    val height by animateDpAsState(
        if (isStickToCamera) (displayWidthDp * dims.widthPercent / 100f).dp else (dims.heightDp + heightBonus).dp,
        heightSpec, label = "islandHeight"
    )

    val cornerRadius = (collapsed.heightDp / 2f).dp
    val offsetX by animateDpAsState(if (isStickToCamera) 0.dp else dims.offsetXDp.dp, spec, label = "islandOffsetX")
    val offsetY by animateDpAsState(if (isStickToCamera) 0.dp else dims.offsetYDp.dp, spec, label = "islandOffsetY")
    val topLeft by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerTopLeftDp.dp, spec, label = "cornerTL")
    val topRight by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerTopRightDp.dp, spec, label = "cornerTR")
    val bottomLeft by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerBottomLeftDp.dp, spec, label = "cornerBL")
    val bottomRight by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerBottomRightDp.dp, spec, label = "cornerBR")
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = motion.fade(),
        label = "islandBackgroundFade",
    )

    val dotDp = collapsed.heightDp.dp
    val revealWidth = lerpDp(dotDp, width, reveal.value)
    val revealHeight = lerpDp(dotDp, height, reveal.value)
    val dotCorner = dotDp / 2
    val revealTopLeft = lerpDp(dotCorner, topLeft, reveal.value)
    val revealTopRight = lerpDp(dotCorner, topRight, reveal.value)
    val revealBottomLeft = lerpDp(dotCorner, bottomLeft, reveal.value)
    val revealBottomRight = lerpDp(dotCorner, bottomRight, reveal.value)

    val haptic = LocalHapticFeedback.current

    CompositionLocalProvider(LocalActionButtonAnimation provides actionButtonAnimation) {
    Box(modifier = Modifier.fillMaxSize()) {
        val stickAlignment = if (isRotation270) Alignment.CenterEnd else Alignment.CenterStart
        val stickPaddingStart = if (isStickToCamera && !isRotation270) offsetYDp.dp else 0.dp
        val stickPaddingEnd = if (isStickToCamera && isRotation270) offsetYDp.dp else 0.dp

        Box(
            modifier = Modifier
                .align(if (isStickToCamera) stickAlignment else Alignment.TopCenter)
                .padding(start = stickPaddingStart, end = stickPaddingEnd)
                .offset(x = if (isStickToCamera) 0.dp else offsetX, y = if (isStickToCamera) 0.dp else offsetY),
        ) {
            if (present || reveal.value > 0f) {
                IslandSurface(
                    modifier = Modifier
                        .width(revealWidth)
                        .height(revealHeight)
                        .graphicsLayer {
                            val extraPx = PressExpandDp.toPx() * 2f * pressExpand.value
                            val widen = if (size.width > 0f) (size.width + extraPx) / size.width else 1f
                            scaleX = boopScale.value * widen
                            scaleY = boopScale.value
                            translationX = dismissOffsetX.value
                            val travel = abs(dismissOffsetX.value) / size.width.coerceAtLeast(1f)
                            val revealAlpha = (reveal.value / 0.2f).coerceIn(0f, 1f)
                            alpha = (1f - travel).coerceIn(0.25f, 1f) * revealAlpha
                        }
                        .pointerInput(forcedExpanded, isExpanded, replying, emptyPill, pressWidens, shownEvent?.id) {
                            if (forcedExpanded == true) {
                                return@pointerInput
                            }

                            detectTapGestures(
                                onPress = {
                                    if (replying) {
                                        return@detectTapGestures
                                    }

                                    if (!isExpanded) {
                                        scope.launch {
                                            if (pressWidens) {
                                                pressExpand.animateTo(1f, motion.boop())
                                            } else {
                                                // Empty cutout scale tap animation
                                                boopScale.animateTo(0.96f, motion.boop())
                                            }
                                        }
                                    }

                                    tryAwaitRelease()

                                    if (!isExpanded) {
                                        scope.launch {
                                            if (pressWidens) {
                                                pressExpand.animateTo(0f, motion.boop())
                                            } else {
                                                boopScale.animateTo(1f, motion.boop())
                                            }
                                        }
                                    }
                                },
                                onTap = {
                                    if (vibrateOnTap) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (emptyPill) {
                                        // "Open center" expands the resting pill into the shortcut
                                        // grid (a second tap toggles it closed); every other "On
                                        // click" action (e.g. open an app) runs via onEmptyClick.
                                        if (emptyOpensCenter) {
                                            if (forcedExpanded == null) {
                                                tapExpanded = !tapExpanded
                                                if (tapExpanded) {
                                                    scope.launch { motion.pop(boopScale, peak = 1.03f) }
                                                }
                                            }
                                        } else {
                                            onEmptyClick()
                                        }
                                        return@detectTapGestures
                                    }

                                    // While typing a reply, ignore taps on the surface itself.
                                    if (replying) return@detectTapGestures

                                    // The phone tile is normal-only, so a tap never toggles it open;
                                    // instead it opens the dialer's in-call screen (its content intent).
                                    if (isNormalOnly) {
                                        if (shownEvent.contentIntent != null) onActivate()
                                        return@detectTapGestures
                                    }

                                    // Tap to open the app
                                    if ((isExpanded || forcedExpanded == false) && shownEvent?.contentIntent != null) {
                                        tapExpanded = false
                                        onActivate()
                                    } else if (forcedExpanded == null) {
                                        tapExpanded = !tapExpanded
                                        if (isExpanded) {
                                            scope.launch {
                                                motion.pop(boopScale, peak = 1.02f)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        // Swipe up on the expanded island to shrink it back to the normal cutout.
                        .pointerInput(forcedExpanded, isExpanded, replying, shrinkOnSwipeUp, emptyPill, shownEvent?.id) {
                            // The resting empty cutout has no expanded state to shrink back from, so
                            // don't install the detector at all — it would only swallow vertical drags.
                            if (forcedExpanded != null || !shrinkOnSwipeUp || emptyPill) return@pointerInput
                            val threshold = SWIPE_UP_SHRINK_THRESHOLD_DP.dp.toPx()
                            var dragTotal = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onDragEnd = {
                                    if (isExpanded && !replying && dragTotal <= -threshold) {
                                        tapExpanded = false
                                    }
                                },
                            ) { change, dragAmount ->
                                dragTotal += dragAmount
                                change.consume()
                            }
                        }
                        // Swipe sideways to dismiss the cutout (and, for a notification, clear it from
                        // the system). Only the direction(s) and cutout state(s) the user allows let go.
                        .pointerInput(forcedExpanded, swipeToDismiss, swipeDismissDirection, swipeDismissTarget, isExpanded, replying, emptyPill, shownEvent?.id) {
                            val targetAllows = when (swipeDismissTarget) {
                                SwipeDismissTarget.BOTH -> true
                                SwipeDismissTarget.EXPANDED -> isExpanded
                                SwipeDismissTarget.NORMAL -> !isExpanded
                            }
                            // The resting empty cutout is meant to stay: a swipe must neither slide it
                            // away nor clear the departed notification it still remembers.
                            if (forcedExpanded != null || !swipeToDismiss || replying || emptyPill || !targetAllows) return@pointerInput
                            val allowLeft = swipeDismissDirection != SwipeDismissDirection.RIGHT
                            val allowRight = swipeDismissDirection != SwipeDismissDirection.LEFT
                            val threshold = SWIPE_DISMISS_THRESHOLD_DP.dp.toPx()
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val x = dismissOffsetX.value
                                    val dismiss = (x <= -threshold && allowLeft) || (x >= threshold && allowRight)
                                    if (dismiss) {
                                        onDismiss()
                                    } else {
                                        scope.launch {
                                            dismissOffsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                                            )
                                        }
                                    }
                                },
                                onDragCancel = { scope.launch { dismissOffsetX.animateTo(0f) } },
                            ) { change, dragAmount ->
                                // Clamp to the allowed direction(s) so a disabled side can't be dragged.
                                val next = (dismissOffsetX.value + dragAmount).let {
                                    when {
                                        !allowLeft -> it.coerceAtLeast(0f)
                                        !allowRight -> it.coerceAtMost(0f)
                                        else -> it
                                    }
                                }
                                scope.launch { dismissOffsetX.snapTo(next) }
                                change.consume()
                            }
                        },
                    shape = cornerShape(revealTopLeft, revealTopRight, revealBottomLeft, revealBottomRight),
                    appearance = appearance,
                    progress = expandProgress,
                ) {
                    Crossfade(targetState = isExpanded, animationSpec = tween(scaled(150)), label = "islandContent") { showExpanded ->
                        if (emptyPill) {
                            if (showExpanded) {
                                CenterContent(
                                    shortcuts = centerShortcuts,
                                    showLabels = centerShowLabels,
                                    fillContainers = centerFillContainers,
                                    themedIcons = centerThemedIcons,
                                    onContentHeight = { centerContentHeightDp = it },
                                    onShortcut = { shortcut ->
                                        // Any press counts as activity, restarting the auto-collapse
                                        // timer so the center stays up while it's being used.
                                        centerInteraction++
                                        // In-place toggles (torch) keep the center open; everything
                                        // else closes it as we act, so it isn't left over the screen
                                        // (and out of a screenshot the shortcut may trigger).
                                        if (!shortcut.keepsCenterOpen) tapExpanded = false
                                        onCenterShortcut(shortcut)
                                    },
                                )
                            } else if (emptyIcon != null) {
                                EmptyPillContent(
                                    icon = emptyIcon,
                                    containerColor = emptyIconColor,
                                    heightDp = collapsed.heightDp,
                                    isStickToCamera = isStickToCamera,
                                )
                            }
                        } else {
                            shownEvent?.let { e ->
                                if (e.call != null) {
                                    CallNormalContent(event = e, onAction = onAction)
                                } else if (showExpanded) {
                                    ExpandedContent(
                                        event = e,
                                        showActions = showActions,
                                        appearance = appearance,
                                        topMarginDp = expanded.topMarginDp,
                                        replyingTo = replyingTo,
                                        replySent = confirmingSent,
                                        progressData = e.progressData,
                                        onAction = onAction,
                                        onStartReply = { replyingTo = it },
                                        onCancelReply = { replyingTo = null },
                                        onSendReply = { text ->
                                            replyingTo?.let { action ->
                                                sentReply = action to text
                                                scope.launch {
                                                    delay(REPLY_SENT_FEEDBACK_MS)
                                                    onReply(action, text)
                                                }
                                            }
                                            replyingTo = null
                                        },
                                        onDismiss = onDismiss,
                                        onHeightMeasured = { assistantContentHeightDp = it },
                                    )
                                } else {
                                    CollapsedContent(e, collapsed.heightDp, isStickToCamera)
                                }
                            }
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
    topMarginDp: Int = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
    expanded: Boolean,
    appearance: AppearanceSettings = AppearanceSettings(),
    showActions: Boolean = true,
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
        // A static preview shows one state outright, so snap the fill to it.
        progress = if (expanded) 1f else 0f,
    ) {
        if (expanded) {
            ExpandedContent(
                event = event,
                showActions = showActions,
                appearance = appearance,
                topMarginDp = topMarginDp,
                replyingTo = null,
                replySent = false,
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

/**
 * The island's surface: shadow, optional stroke and the background fill. [progress] (0 = collapsed,
 * 1 = expanded) cross-fades the normal fill into the expanded fill, so if the two states use
 * different colours (or gradients) the background morphs in lockstep with the size animation.
 */
@Composable
private fun IslandSurface(
    modifier: Modifier,
    shape: Shape,
    appearance: AppearanceSettings,
    progress: Float,
    content: @Composable () -> Unit,
) {
    val normalBrush = appearance.backgroundNormal.resolveBrush()
    val expandedBrush = appearance.backgroundExpanded.resolveBrush()
    val repColor = lerp(
        appearance.backgroundNormal.representativeColor(),
        appearance.backgroundExpanded.representativeColor(),
        progress,
    )

    val contentColor = if (repColor.luminance() > 0.5f) PillTextColorDark else PillTextColor
    val border = if (appearance.strokeEnabled) {
        BorderStroke(appearance.strokeWidthDp.dp, appearance.strokeColor.resolve())
    } else {
        null
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        shadowElevation = if (appearance.shadowEnabled) 6.dp else 0.dp,
        tonalElevation = 0.dp,
        border = border,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(normalBrush))
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = progress }
                        .background(expandedBrush),
                )
            }
            content()
        }
    }
}

/**
 * Builds a rounded shape with each corner independently sized (LTR-mapped). Corners are clamped to
 * be non-negative: the spring animation driving the radii overshoots below its target, so easing a
 * corner down to 0 dp momentarily produces a negative value, which Compose refuses to render
 * ("RoundRect with negative corners could not be rendered"). Clamping yields a plain rectangle at 0.
 */
private fun cornerShape(topLeft: Dp, topRight: Dp, bottomLeft: Dp, bottomRight: Dp) =
    RoundedCornerShape(
        topStart = topLeft.coerceAtLeast(0.dp),
        topEnd = topRight.coerceAtLeast(0.dp),
        bottomStart = bottomLeft.coerceAtLeast(0.dp),
        bottomEnd = bottomRight.coerceAtLeast(0.dp),
    )

/**
 * The cover to draw for the music tile, or null to fall back to the note glyph. The media session's
 * own art wins; a player that publishes its cover as a remote URI (Spotify) leaves the session
 * without one, so the tile uses the cover lifted off that same player's media notification. Matched
 * on package so a stale cover is never drawn over a different player's track.
 */
@Composable
private fun albumArtFor(event: IslandEvent, nowPlaying: NowPlaying?): ImageBitmap? {
    val notificationArt by MediaArtBus.state.collectAsStateWithLifecycle()
    if (event.media?.showAlbumArt != true) return null
    return nowPlaying?.albumArt
        ?: notificationArt?.takeIf { it.packageName == nowPlaying?.packageName }?.art
}

/**
 * The colour of the ring around the album cover, or null when the user hasn't asked for one. An
 * enabled ring with no colour picked falls back to the tile's own accent, matching what the
 * settings screen offers as its default swatch.
 */
@Composable
private fun albumArtStrokeFor(event: IslandEvent): Color? =
    event.media?.takeIf { it.albumArtStroke }
        ?.let { it.albumArtStrokeColor?.resolve() ?: event.accent }

@Composable
private fun CollapsedContent(event: IslandEvent, heightDp: Int, isStickToCamera: Boolean = false) {
    // The music tile shows album art, the phone tile the caller's photo, on the normal cutout.
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)
    val callPhoto = event.call?.takeIf { it.showPhoto }?.let { onCall?.photo }
    val badgeSize = (heightDp * 0.72f).dp

    Box(modifier = Modifier.fillMaxSize()) {
        val placement = Modifier
            .align(if (isStickToCamera) Alignment.BottomCenter else Alignment.CenterStart)
            .padding(
                start = if (isStickToCamera) 0.dp else (heightDp * 0.16f).dp,
                bottom = if (isStickToCamera) (heightDp * 0.14f).dp else 0.dp,
            )
        when {
            albumArt != null -> AlbumArt(
                bitmap = albumArt,
                size = badgeSize,
                modifier = placement,
                rotate = event.media?.rotateAlbumArt == true,
                playing = nowPlaying?.isPlaying == true,
                strokeColor = albumArtStrokeFor(event),
            )

            callPhoto != null -> ContactPhoto(bitmap = callPhoto, size = badgeSize, modifier = placement)

            else -> IconBadge(
                event = event,
                badgeSize = badgeSize,
                iconSize = (heightDp * 0.46f).dp,
                modifier = placement,
            )
        }
        // The timer tile shows the remaining time on the trailing edge, opposite its icon.
        if (event.timer != null && !isStickToCamera) {
            timerRemainingText()?.let { remaining ->
                Text(
                    text = remaining,
                    color = LocalContentColor.current,
                    fontSize = (heightDp * 0.34f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = (heightDp * 0.24f).dp),
                )
            }
        }
        event.progressData?.takeIf { !isStickToCamera }?.let { progress ->
            val indicatorModifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = (heightDp * 0.24f).dp)
                .size((heightDp * 0.5f).dp)
            val strokeWidth = (heightDp * 0.06f).dp
            if (progress.isIndeterminate) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = strokeWidth,
                )
            } else {
                val fraction = if (progress.max <= 0) 0f
                    else (progress.current.toFloat() / progress.max).coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    label = "collapsedProgress",
                )
                CircularProgressIndicator(
                    progress = { animatedFraction },
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

/**
 * The resting (event-less) pill's optional glyph, centred on the collapsed cutout. A user-chosen
 * [containerColor] draws a filled disc with contrasting ink behind the glyph; without one, the glyph
 * sits directly on the pill in its content colour. The glyph is a picked image or a Material icon.
 */
@Composable
private fun EmptyPillContent(
    icon: IconSource,
    containerColor: CutoutColor?,
    heightDp: Int,
    isStickToCamera: Boolean = false,
) {
    val context = LocalContext.current
    val badgeSize = (heightDp * 0.72f).dp
    val iconSize = (heightDp * 0.46f).dp

    val disc = containerColor?.resolve()
    val glyphColor = when {
        disc != null -> if (disc.luminance() > 0.5f) PillTextColorDark else PillTextColor
        else -> LocalContentColor.current
    }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = icon) {
        value = when (icon) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                Uri.parse(icon.uri).loadImageBitmapOrNull(context)
            }
            is IconSource.Material -> null
        }
    }
    val materialIcon = (icon as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Sit on the leading edge like the normal cutout's icon (clear of the camera), or centred at
        // the bottom when the pill is stuck beside the camera — mirroring CollapsedContent.
        val placement = Modifier
            .align(if (isStickToCamera) Alignment.BottomCenter else Alignment.CenterStart)
            .padding(
                start = if (isStickToCamera) 0.dp else (heightDp * 0.16f).dp,
                bottom = if (isStickToCamera) (heightDp * 0.14f).dp else 0.dp,
            )
        Box(
            modifier = placement
                .size(badgeSize)
                .clip(CircleShape)
                .then(if (disc != null) Modifier.background(disc) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val loaded = bitmap
            when {
                loaded != null -> androidx.compose.foundation.Image(
                    bitmap = loaded,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(badgeSize * 0.78f).clip(CircleShape),
                )

                materialIcon != null -> Icon(
                    imageVector = materialIcon,
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

// The height of each shortcut button in the expanded center (its diameter too, in disc mode).
private val CenterDiscDp = 64.dp

/**
 * The expanded "center" the resting pill opens with [com.ekoehler.expressivecutout.data.EmptyClickAction.OPEN_CENTER]:
 * a titled row of round shortcut buttons, scrolling horizontally when they overflow. Sits in the
 * lower part of the cutout (clear of the camera), mirroring [ExpandedContent]'s placement.
 */
@Composable
private fun CenterContent(
    shortcuts: List<CenterShortcut>,
    showLabels: Boolean,
    fillContainers: Boolean,
    themedIcons: Boolean,
    onContentHeight: (Int) -> Unit,
    onShortcut: (CenterShortcut) -> Unit,
) {
    val density = LocalDensity.current.density
    val torchOn by TorchStateBus.on.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                // Report the content's natural height so the cutout can fit itself to it.
                .onGloballyPositioned { onContentHeight((it.size.height / density).toInt()) },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.center_shortcuts_title),
                color = LocalContentColor.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (shortcuts.isEmpty()) {
                Text(
                    text = stringResource(R.string.center_shortcuts_empty),
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            } else {
                // Every shortcut shares the width equally (flex: 1), so the row always fills the
                // cutout with evenly-spread buttons. Under the EXPAND press animation a pressed button
                // borrows width from its siblings (they spring thinner) instead of overflowing in
                // place — the same give-and-take as the action chips (see [ActionChipRow]).
                val redistribute = LocalActionButtonAnimation.current == ActionButtonAnimation.EXPAND &&
                    shortcuts.size > 1
                val interactions = remember(shortcuts.size) {
                    List(shortcuts.size) { MutableInteractionSource() }
                }
                val pressedFlags = interactions.map { it.collectIsPressedAsState().value }
                val pressedIndex = if (redistribute) pressedFlags.indexOfFirst { it } else -1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shortcuts.forEachIndexed { i, shortcut ->
                        val target = when {
                            !redistribute || pressedIndex < 0 -> 1f
                            i == pressedIndex -> 1f + FULL_EXPAND_DELTA
                            else -> 1f - FULL_EXPAND_DELTA / (shortcuts.size - 1)
                        }
                        val weight by animateFloatAsState(
                            targetValue = target,
                            animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
                            label = "centerWeight",
                        )
                        CenterShortcutButton(
                            shortcut = shortcut,
                            showLabel = showLabels,
                            fillContainer = fillContainers,
                            themedIcon = themedIcons,
                            active = shortcut is CenterShortcut.Torch && torchOn,
                            onClick = { onShortcut(shortcut) },
                            interaction = interactions[i],
                            // When redistributing, the width give-and-take IS the press animation, so
                            // the button must not also widen itself in place.
                            animatePress = !redistribute,
                            modifier = Modifier.weight(weight),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single center shortcut: its container (a fixed disc, or a slot-filling pill when [fillContainer])
 * holding the glyph or the real launcher icon, with a small label beneath. A togglable shortcut that
 * is [active] lights up in the theme's primary / on-primary. Shares the island's [pressScale].
 */
@Composable
private fun CenterShortcutButton(
    shortcut: CenterShortcut,
    showLabel: Boolean,
    fillContainer: Boolean,
    themedIcon: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
    animatePress: Boolean = true,
) {
    val containerColor = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.14f)
    val glyphColor = if (active) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
    val shapeModifier = if (fillContainer) {
        Modifier.fillMaxWidth().height(CenterDiscDp)
    } else {
        Modifier.size(CenterDiscDp)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = CircleShape,
            color = containerColor,
            contentColor = glyphColor,
            modifier = shapeModifier.then(if (animatePress) Modifier.pressScale(interaction) else Modifier),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val appIcon = (shortcut as? CenterShortcut.LaunchApp)?.let { rememberAppIcon(it.packageName, themedIcon) }
                when {
                    // A themed (monochrome) app icon: tint its glyph to match the built-in shortcuts.
                    // Its safe-zone padding means it reads well filling the whole button.
                    appIcon?.themed == true -> Icon(
                        bitmap = appIcon.bitmap,
                        contentDescription = null,
                        tint = glyphColor,
                        modifier = Modifier.size(CenterDiscDp),
                    )

                    appIcon != null -> androidx.compose.foundation.Image(
                        bitmap = appIcon.bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(CenterDiscDp * 0.6f).clip(CircleShape),
                    )

                    else -> Icon(
                        imageVector = CenterShortcutCatalog.iconFor(shortcut),
                        contentDescription = null,
                        tint = glyphColor,
                        modifier = Modifier.size(CenterDiscDp * 0.46f),
                    )
                }
            }
        }
        if (showLabel) {
            Text(
                text = centerShortcutLabel(shortcut),
                color = LocalContentColor.current.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The label for a shortcut: its fixed string resource, or the app's display name for a launcher. */
@Composable
private fun centerShortcutLabel(shortcut: CenterShortcut): String {
    CenterShortcutCatalog.labelResFor(shortcut)?.let { return stringResource(it) }
    val pkg = (shortcut as? CenterShortcut.LaunchApp)?.packageName ?: return ""
    val context = LocalContext.current
    val label by produceState(initialValue = pkg, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
        }
    }
    return label
}

/** An app's loaded icon, and whether it's the themed (monochrome, tint-me) glyph vs the full-colour icon. */
private class LoadedAppIcon(val bitmap: ImageBitmap, val themed: Boolean)

/**
 * Loads an app's launcher icon off the main thread, or null if the package is gone. When [themed] is
 * on and the app ships an adaptive icon with a monochrome layer (API 33+), that layer is returned to
 * be tinted like the built-in shortcut glyphs; otherwise the full-colour icon is used.
 */
@Composable
private fun rememberAppIcon(packageName: String, themed: Boolean): LoadedAppIcon? {
    val context = LocalContext.current
    val icon by produceState<LoadedAppIcon?>(initialValue = null, packageName, themed) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val monochrome = if (themed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    (drawable as? AdaptiveIconDrawable)?.monochrome
                } else {
                    null
                }
                if (monochrome != null) {
                    LoadedAppIcon(monochrome.toBitmap().asImageBitmap(), themed = true)
                } else {
                    LoadedAppIcon(drawable.toBitmap().asImageBitmap(), themed = false)
                }
            }.getOrNull()
        }
    }
    return icon
}

/**
 * The remaining time on the timer tile, formatted m:ss (or h:mm:ss past an hour), or null when no
 * timer is present. Reads [RunningTimerBus]: a running timer ticks down against
 * [SystemClock.elapsedRealtime] (re-derived a few times a second so the collapsed pill and expanded
 * card stay in sync), while a paused timer shows its frozen remainder without ticking. Seconds are
 * rounded up so a fresh 5:00 timer reads "5:00", and it lands on "0:00" exactly at zero.
 */
@Composable
private fun timerRemainingText(): String? {
    val timer by RunningTimerBus.state.collectAsStateWithLifecycle()
    val t = timer ?: return null
    val end = t.endElapsedRealtimeMs
    val remainingMs = if (end != null) {
        var nowElapsed by remember(end) { mutableStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(end) {
            while (true) {
                nowElapsed = SystemClock.elapsedRealtime()
                delay(250L)
            }
        }
        (end - nowElapsed).coerceAtLeast(0L)
    } else {
        (t.pausedRemainingMs ?: return null).coerceAtLeast(0L)
    }
    return formatCallDuration((remainingMs + 999L) / 1_000L)
}

/** Formats an elapsed timestamp into a relative time string (e.g. "Now", "5s ago", "2m ago", "2h ago", "1d ago"). */
fun formatRelativeTime(postTimeMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val elapsedSeconds = ((nowMs - postTimeMs) / 1000L).coerceAtLeast(0L)
    return when {
        elapsedSeconds < 5L -> "Now"
        elapsedSeconds < 60L -> "${elapsedSeconds}s ago"
        elapsedSeconds < 3600L -> "${elapsedSeconds / 60L}m ago"
        elapsedSeconds < 86400L -> "${elapsedSeconds / 3600L}h ago"
        else -> "${elapsedSeconds / 86400L}d ago"
    }
}

/** Combines source app name and relative timestamp according to visibility settings. */
fun formatNotificationHeader(
    appName: String?,
    relativeTime: String?,
    showAppName: Boolean,
    showTimestamp: Boolean,
): String? {
    val showApp = showAppName && !appName.isNullOrBlank()
    val showTime = showTimestamp && !relativeTime.isNullOrBlank()
    return when {
        showApp && showTime -> "$appName • $relativeTime"
        showApp -> appName
        showTime -> relativeTime
        else -> null
    }
}

@Composable
fun rememberRelativeTime(postTimeMs: Long?): String? {
    if (postTimeMs == null) return null
    val relativeTime by produceState(initialValue = formatRelativeTime(postTimeMs), key1 = postTimeMs) {
        while (true) {
            delay(1_000L)
            value = formatRelativeTime(postTimeMs)
        }
    }
    return relativeTime
}

@Composable
private fun ExpandedContent(
    event: IslandEvent,
    showActions: Boolean,
    appearance: AppearanceSettings,
    topMarginDp: Int = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
    replyingTo: IslandAction?,
    replySent: Boolean,
    progressData: ProgressData? = null,
    onAction: (IslandAction) -> Unit,
    onStartReply: (IslandAction) -> Unit,
    onCancelReply: () -> Unit,
    onSendReply: (String) -> Unit,
    onDismiss: () -> Unit = {},
    onHeightMeasured: ((Int) -> Unit)? = null,
) {
    // The music tile has its own expanded layout (album art + playback controls).
    if (event.media != null) {
        MediaExpandedContent(
            event = event,
            buttonHeightDp = appearance.actionButtonHeightDp,
            topMarginDp = topMarginDp,
        )
        return
    }
    // The timer tile: icon + ticking remaining time, and its Reset / Add 1 min chips.
    if (event.timer != null) {
        TimerExpandedContent(
            event = event,
            appearance = appearance,
            topMarginDp = topMarginDp,
            onAction = onAction,
        )
        return
    }
    // The assistant tile: icon + text response with vertical scrolling.
    if (event.assistant != null) {
        AssistantExpandedContent(
            event = event,
            showActions = showActions,
            appearance = appearance,
            onDismiss = onDismiss,
            onHeightMeasured = onHeightMeasured,
        )
        return
    }

    val relativeTime = rememberRelativeTime(event.postTimeMs)
    val headerText = formatNotificationHeader(
        appName = event.appName,
        relativeTime = relativeTime,
        showAppName = appearance.showSourceAppName,
        showTimestamp = appearance.showTimestamp,
    )

    // Content sits below the top margin, leaving the top clear of the camera hole.
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topMarginDp.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
                Column(modifier = Modifier.weight(1f)) {
                    if (headerText != null) {
                        Text(
                            text = headerText,
                            color = event.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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

                    var lastProgressData by remember { mutableStateOf(progressData) }
                    if (progressData != null) lastProgressData = progressData
                    AnimatedVisibility(visible = progressData != null) {
                        lastProgressData?.let { p ->
                            if (p.isIndeterminate) {
                                LinearProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                                )
                            } else {
                                val fraction = if (p.max <= 0) 0f
                                    else (p.current.toFloat() / p.max).coerceIn(0f, 1f)
                                val animatedFraction by animateFloatAsState(
                                    targetValue = fraction,
                                    label = "notificationProgress",
                                )
                                LinearProgressIndicator(
                                    progress = { animatedFraction },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                                )
                            }
                        }
                    }
                }
            }

            // Fall back to the notification's own accent / a neutral tint when unset.
            val sendColor = appearance.sendButtonColor?.resolve() ?: event.accent
            when {
                replySent -> ReplySentRow(
                    tint = sendColor,
                    heightDp = appearance.actionButtonHeightDp,
                    alignment = appearance.sentAlignment,
                )

                replyingTo != null -> ReplyRow(
                    hint = replyingTo.reply?.hint,
                    accent = event.accent,
                    sendColor = sendColor,
                    cancelColor = appearance.cancelButtonColor?.resolve(),
                    inputStyle = appearance.replyInputStyle,
                    cancelOnLeft = appearance.cancelButtonOnLeft,
                    heightDp = appearance.actionButtonHeightDp,
                    onSend = onSendReply,
                    onCancel = onCancelReply,
                )

                showActions && event.actions.isNotEmpty() -> {
                    val chipFill = appearance.actionButtonColor?.resolve() ?: event.accent
                    ActionChipRow(
                        actions = event.actions.take(3),
                        style = appearance.actionButtonStyle,
                        fill = chipFill,
                        heightDp = appearance.actionButtonHeightDp,
                        alignment = appearance.actionButtonAlignment,
                        onChip = { action ->
                            if (action.reply != null) onStartReply(action) else onAction(action)
                        },
                    )
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
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
    animatePress: Boolean = true,
) {
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
        modifier = modifier
            .height(heightDp.dp)
            .then(if (animatePress) Modifier.pressScale(interaction) else Modifier),
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
 * The button press reaction, chosen in settings and provided by [DynamicIsland] so every
 * [pressScale] call site (action chips, reply buttons, call buttons) picks it up without threading
 * the setting through each one. Defaults to [ActionButtonAnimation.SCALE].
 */
private val LocalActionButtonAnimation = staticCompositionLocalOf { ActionButtonAnimation.SCALE }

// How far the EXPAND press animation widens a button, on each side.
private val PressExpandDp = 7.dp

// In a full-width (flex) row, how much extra weight a pressed chip borrows from its siblings under
// the EXPAND animation: it grows by this share while the others give up the same total between them,
// so the row always fills exactly its own width.
private const val FULL_EXPAND_DELTA = 0.15f

/**
 * The expanded action chips row. In [ActionButtonAlignment.FULL] the chips share the width equally;
 * every other alignment sizes them to content and positions the row as a group. When the button
 * animation is [ActionButtonAnimation.EXPAND] *and* the row is full with more than one chip, a
 * pressed chip borrows width from its siblings ([FULL_EXPAND_DELTA]) — its weight springs up while
 * theirs spring down by the same total — an expressive give-and-take that keeps the row at 100%.
 * In every other case each chip animates itself in place via [ActionChip]'s own [pressScale].
 */
@Composable
private fun ActionChipRow(
    actions: List<IslandAction>,
    style: ActionButtonStyle,
    fill: Color,
    heightDp: Int,
    alignment: ActionButtonAlignment,
    onChip: (IslandAction) -> Unit,
) {
    val full = alignment == ActionButtonAlignment.FULL
    val redistribute = full && actions.size > 1 &&
        LocalActionButtonAnimation.current == ActionButtonAnimation.EXPAND
    val interactions = remember(actions.size) { List(actions.size) { MutableInteractionSource() } }
    // Which chip is currently held (first press wins) — drives the width give-and-take. Collected for
    // every chip on each composition so the number of composable calls stays constant.
    val pressedFlags = interactions.map { it.collectIsPressedAsState().value }
    val pressedIndex = if (redistribute) pressedFlags.indexOfFirst { it } else -1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment.toHorizontal()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { i, action ->
            val chipModifier = when {
                redistribute -> {
                    val target = when {
                        pressedIndex < 0 -> 1f
                        i == pressedIndex -> 1f + FULL_EXPAND_DELTA
                        else -> 1f - FULL_EXPAND_DELTA / (actions.size - 1)
                    }
                    val weight by animateFloatAsState(
                        targetValue = target,
                        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
                        label = "chipWeight",
                    )
                    Modifier.weight(weight)
                }
                full -> Modifier.weight(1f)
                else -> Modifier
            }
            ActionChip(
                action = action,
                style = style,
                fill = fill,
                heightDp = heightDp,
                onClick = { onChip(action) },
                modifier = chipModifier,
                interaction = interactions[i],
                // When redistributing, the give-and-take of widths IS the press animation, so the
                // chip must not also expand itself in place.
                animatePress = !redistribute,
            )
        }
    }
}

/**
 * The press reaction shared by the action chips and the reply buttons, so every tap on the island
 * feels the same. Two flavours, selected via [LocalActionButtonAnimation]:
 * [ActionButtonAnimation.SCALE] is the expressive "squish" — a springy scale-down that settles back
 * with a little bounce on release; [ActionButtonAnimation.EXPAND] instead briefly widens the button
 * by [PressExpandDp] on each side. Both animate on the same spring and via [graphicsLayer], so the
 * surrounding layout never reflows.
 */
@Composable
private fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    pressedScale: Float = 0.88f,
): Modifier {
    val animation = LocalActionButtonAnimation.current
    val pressed by interaction.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return this.graphicsLayer {
        when (animation) {
            ActionButtonAnimation.SCALE -> {
                val scale = 1f + (pressedScale - 1f) * progress
                scaleX = scale
                scaleY = scale
            }
            ActionButtonAnimation.EXPAND -> {
                // Grow the width by PressExpandDp on each side, expressed as a scale relative to the
                // button's own measured width so layout stays put.
                val extraPx = PressExpandDp.toPx() * 2f * progress
                if (size.width > 0f) scaleX = (size.width + extraPx) / size.width
            }
        }
    }
}

/**
 * The post-send confirmation shown briefly in place of the reply field: a circular [tint] badge
 * whose check mark springs in with a little overshoot, and a "Sent" label that fades up beside it,
 * so the user gets clear feedback that the message went out before the island dismisses.
 */
@Composable
private fun ReplySentRow(tint: Color, heightDp: Int, alignment: SentAlignment) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, alignment.toHorizontal()),
    ) {
        Box(
            modifier = Modifier
                .size(heightDp.dp)
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                }
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = if (tint.luminance() > 0.5f) PillTextColorDark else PillTextColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(R.string.reply_sent),
            color = LocalContentColor.current.copy(alpha = appear.value),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
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

    // The segmented style joins cancel, field and send into one bar (see SegmentedReplyRow); the
    // others are separate controls with only the field's corner rounding differing.
    if (inputStyle == ReplyInputStyle.SEGMENTED) {
        SegmentedReplyRow(
            text = text,
            onValueChange = { text = it },
            hint = hint,
            accent = accent,
            sendColor = sendColor,
            cancelColor = cancelColor,
            cancelOnLeft = cancelOnLeft,
            heightDp = heightDp,
            focusRequester = focusRequester,
            onSend = send,
            onCancel = onCancel,
        )
        return
    }

    val fieldShape = when (inputStyle) {
        ReplyInputStyle.EXPRESSIVE -> CircleShape
        ReplyInputStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        ReplyInputStyle.MATERIAL_2 -> RoundedCornerShape(4.dp)
        ReplyInputStyle.SEGMENTED -> CircleShape // handled above; unreachable.
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

/**
 * The "segmented" reply style: cancel, field and send sit flush in one connected bar, split by a
 * small gap, with the two outer segments carrying fully-rounded end-caps and the inner edges only
 * lightly rounded. [cancelOnLeft] chooses which end the cancel button caps (send always trails).
 */
@Composable
private fun SegmentedReplyRow(
    text: String,
    onValueChange: (String) -> Unit,
    hint: String?,
    accent: Color,
    sendColor: Color,
    cancelColor: Color?,
    cancelOnLeft: Boolean,
    heightDp: Int,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val cap = (heightDp / 2).dp
    val inner = 8.dp
    val startCap = RoundedCornerShape(topStart = cap, bottomStart = cap, topEnd = inner, bottomEnd = inner)
    val endCap = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = cap, bottomEnd = cap)
    val innerShape = RoundedCornerShape(inner)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val cancel = @Composable { shape: Shape ->
            ReplySegmentButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Cancel reply",
                container = LocalContentColor.current.copy(alpha = 0.12f),
                content = cancelColor ?: LocalContentColor.current.copy(alpha = 0.7f),
                shape = shape,
                heightDp = heightDp,
                onClick = onCancel,
            )
        }
        // Leading end-cap: cancel when it's on the left, otherwise the field itself.
        if (cancelOnLeft) cancel(startCap)
        ReplyField(
            modifier = Modifier.weight(1f),
            text = text,
            onValueChange = onValueChange,
            hint = hint,
            accent = accent,
            shape = if (cancelOnLeft) innerShape else startCap,
            heightDp = heightDp,
            focusRequester = focusRequester,
            onSend = onSend,
        )
        if (!cancelOnLeft) cancel(innerShape)
        val sendEnabled = text.isNotBlank()
        ReplySegmentButton(
            icon = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Send reply",
            container = if (sendEnabled) sendColor else LocalContentColor.current.copy(alpha = 0.12f),
            content = when {
                !sendEnabled -> LocalContentColor.current.copy(alpha = 0.4f)
                sendColor.luminance() > 0.5f -> PillTextColorDark
                else -> PillTextColor
            },
            shape = endCap,
            heightDp = heightDp,
            enabled = sendEnabled,
            onClick = onSend,
        )
    }
}

/** One end-cap of the segmented reply bar: a shaped, filled tap target with a centred icon. */
@Composable
private fun ReplySegmentButton(
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    content: Color,
    shape: Shape,
    heightDp: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier.size(heightDp.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
            )
        }
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

/**
 * The music tile's expanded layout: album art + track/artist, and (when enabled) a row of
 * previous / play‑pause / next controls. Live state — art, the play vs pause icon and the
 * transport handle — is read from [NowPlayingBus] so the controls stay in sync as playback changes.
 */
@Composable
private fun MediaExpandedContent(
    event: IslandEvent,
    buttonHeightDp: Int,
    topMarginDp: Int = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
) {
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = topMarginDp.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (albumArt != null) {
                    AlbumArt(
                        bitmap = albumArt,
                        size = 44.dp,
                        rotate = event.media?.rotateAlbumArt == true,
                        playing = nowPlaying?.isPlaying == true,
                        strokeColor = albumArtStrokeFor(event),
                    )
                } else {
                    IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
                }
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            event.media?.takeIf { it.showProgress }?.let {
                MediaProgressBar(progress = nowPlaying?.progress)
            }

            event.media?.takeIf { it.showControls }?.let { media ->
                MediaControls(
                    isPlaying = nowPlaying?.isPlaying == true,
                    accent = event.accent,
                    enabled = nowPlaying != null,
                    heightDp = buttonHeightDp,
                    skipStyle = media.skipStyle,
                    playPauseStyle = media.playPauseStyle,
                    onPrevious = { nowPlaying?.transport?.previous() },
                    onPlayPause = { nowPlaying?.transport?.playPause() },
                    onNext = { nowPlaying?.transport?.next() },
                )
            }
        }
    }
}

/**
 * The music tile's playback bar. [MediaProgress] is an anchor rather than a live position — the
 * media session only republishes on a real change, never on a tick — so this drives its own clock
 * while playback runs and extrapolates from that anchor. A session that publishes no track length
 * (a live stream) gets the indeterminate bar instead, matching the notification tile's.
 */
@Composable
private fun MediaProgressBar(progress: MediaProgress?) {
    val color = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primaryContainer

    if (progress?.durationMs == null) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = trackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        return
    }

    // Re-anchored on every new snapshot, so a seek or a track change lands immediately rather than
    // being animated across from the stale position.
    var fraction by remember(progress) {
        mutableFloatStateOf(progress.fractionAt(SystemClock.elapsedRealtime()) ?: 0f)
    }
    LaunchedEffect(progress) {
        while (progress.speed > 0f) {
            delay(PROGRESS_TICK_MS)
            fraction = progress.fractionAt(SystemClock.elapsedRealtime()) ?: 0f
        }
    }

    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth(),
        color = color,
        trackColor = trackColor,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    )
}

/**
 * Previous / play‑pause / next. Each button's fill colour, opacity and corner rounding come from the
 * music tile's settings: the skip buttons share [skipStyle] (plain over the pill by default), the
 * centre button uses [playPauseStyle] (the tile accent by default).
 */
@Composable
private fun MediaControls(
    isPlaying: Boolean,
    accent: Color,
    enabled: Boolean,
    heightDp: Int,
    skipStyle: MusicButtonStyle,
    playPauseStyle: MusicButtonStyle,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous track",
            enabled = enabled,
            heightDp = heightDp,
            iconSize = 26.dp,
            fill = skipStyle.resolveFill(fallback = null),
            cornerPercent = skipStyle.cornerPercent,
            onClick = onPrevious,
        )
        MediaButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            enabled = enabled,
            heightDp = heightDp,
            // The play/pause button is a 16:9 rectangle rather than a square.
            widthDp = heightDp * 16 / 9,
            iconSize = 24.dp,
            fill = playPauseStyle.resolveFill(fallback = accent),
            cornerPercent = playPauseStyle.cornerPercent,
            onClick = onPlayPause,
        )
        MediaButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next track",
            enabled = enabled,
            heightDp = heightDp,
            iconSize = 26.dp,
            fill = skipStyle.resolveFill(fallback = null),
            cornerPercent = skipStyle.cornerPercent,
            onClick = onNext,
        )
    }
}

/** The concrete fill for a transport button, or null (a plain, unfilled button) when neither the
 *  style nor the [fallback] supplies a colour and the style isn't [MusicButtonStyle.filled].
 *  A filled style with no colour falls back to [MusicButtonFilledDefault]. Opacity folds into alpha. */
@Composable
private fun MusicButtonStyle.resolveFill(fallback: Color?): Color? {
    val base = color?.resolve() ?: fallback ?: if (filled) MusicButtonFilledDefault else return null
    return base.copy(alpha = opacity)
}

/**
 * A transport button with the shared press "squish". A null [fill] renders a plain (unfilled) button
 * tinted with the content colour; a non-null [fill] renders a filled button whose corners are rounded
 * by [cornerPercent] relative to its height (50 = a pill / stadium, 0 = a square) with an
 * auto-contrasting icon. [widthDp] defaults to [heightDp] (a square); a larger value makes a
 * rectangle — e.g. the 16:9 play/pause button.
 */
@Composable
private fun MediaButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    heightDp: Int,
    iconSize: Dp,
    fill: Color?,
    cornerPercent: Int,
    onClick: () -> Unit,
    widthDp: Int = heightDp,
) {
    val interaction = remember { MutableInteractionSource() }
    val modifier = Modifier
        .size(width = widthDp.dp, height = heightDp.dp)
        .pressScale(interaction)
    if (fill == null) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = modifier,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = LocalContentColor.current,
                modifier = Modifier.size(iconSize),
            )
        }
    } else {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            // Corner radius keyed to height (not width) so a wide button still reads as a clean
            // pill at 50% rather than an ellipse: 50 → height/2 (stadium), 0 → square.
            shape = RoundedCornerShape((heightDp * cornerPercent / 100f).dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = fill,
                contentColor = if (fill.luminance() > 0.5f) PillTextColorDark else PillTextColor,
                disabledContainerColor = LocalContentColor.current.copy(alpha = 0.12f),
                disabledContentColor = LocalContentColor.current.copy(alpha = 0.4f),
            ),
            modifier = modifier,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

// Layout metrics for the call cutout, shared by CallNormalContent (which draws it) and
// callCutoutWidthPercent (which measures the name to size the pill) so the two stay in agreement.
private const val CALL_ROW_PADDING_DP = 8
private const val CALL_ROW_SPACING_DP = 12
// The photo/icon container and the hang-up button are deliberately the same size so the cutout
// reads as symmetrical, with the caller between two equal circles.
private const val CALL_HANGUP_BUTTON_DP = 44
private const val CALL_AVATAR_DP = CALL_HANGUP_BUTTON_DP
private const val CALL_NAME_SIZE_SP = 15
// A little breathing room so the name never sits flush against the button before the pill grows.
private const val CALL_NAME_SLACK_DP = 8

// Metrics for the two-row incoming-call layout (caller row over Take / Hang up buttons). The layout
// grows past the expanded cutout by [callIncomingExtraDp] so the caller row can sit below the camera
// hole with a flexible gap before the buttons pinned to the bottom edge.
private const val CALL_INCOMING_SIDE_PAD_DP = 14
private const val CALL_INCOMING_BOTTOM_PAD_DP = 14
// Top clearance for the camera hole, matching the empty top the expanded card leaves for it.
private const val CALL_INCOMING_TOP_PAD_DP = 34
private const val CALL_INCOMING_BUTTON_GAP_DP = 10
private const val CALL_INCOMING_BUTTON_DP = 44
private const val CALL_INCOMING_AVATAR_DP = 40

/**
 * The extra height (over the expanded cutout) the incoming two-row layout claims for its bottom Take /
 * Hang up row, mirroring [expandedActionsExtraDp]. Shared with the overlay controller so the window and
 * touchable region stay as tall as what [IncomingCallExpandedContent] renders.
 */
internal fun callIncomingExtraDp(): Int = CALL_INCOMING_BUTTON_DP + CALL_INCOMING_BUTTON_GAP_DP

/**
 * The width (as a screen-width percentage) the call cutout should span for [callerName]:
 * [CALL_MIN_WIDTH_PERCENT] by default, widening to fit a long name up to [CALL_MAX_WIDTH_PERCENT].
 * [trailingButtons] reserves room for that many trailing call buttons (one for a connected call's
 * hang-up, two for an incoming call's decline + answer, zero when actions are hidden). An [incoming]
 * call always spans the full [CALL_MAX_WIDTH_PERCENT] (never narrower) so its two buttons and the
 * number/name always have room. The pill is sized to this width and its content laid out within it —
 * a name too long for even the max width ellipsizes — so measuring the name here (rather than letting
 * content drive the size) lets the overlay's rendering and its touchable region agree exactly on the
 * pill's width. [density] converts the measured text to dp.
 */
internal fun callCutoutWidthPercent(
    callerName: String,
    trailingButtons: Int,
    incoming: Boolean,
    displayWidthDp: Int,
    density: Float,
): Int {
    // Everything on the row that isn't the name: leading avatar + its spacing, the trailing button(s) +
    // their spacing (when shown), and the row's horizontal padding on both edges. Mirrors CallNormalContent.
    val trailingDp = trailingButtons * (CALL_HANGUP_BUTTON_DP + CALL_ROW_SPACING_DP)
    val fixedDp = CALL_ROW_PADDING_DP * 2 + CALL_AVATAR_DP + CALL_ROW_SPACING_DP + trailingDp
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = CALL_NAME_SIZE_SP * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val nameWidthDp = paint.measureText(callerName) / density
    val neededDp = fixedDp + nameWidthDp + CALL_NAME_SLACK_DP
    val percent = (neededDp / displayWidthDp.coerceAtLeast(1) * 100f).roundToInt()
    // An incoming call is pinned to the full width; a connected one adapts from the minimum up.
    val floor = if (incoming) CALL_MAX_WIDTH_PERCENT else CALL_MIN_WIDTH_PERCENT
    return percent.coerceIn(floor, CALL_MAX_WIDTH_PERCENT)
}

/**
 * The phone tile's normal-only layout (it has no expanded state), dispatched by call state. A
 * connected call, and an incoming call when the two-row layout is off, use one compact row
 * ([CallSingleRowContent]). An incoming (still ringing) call with the "Expanded layout for incoming
 * calls" setting on uses the taller two-row [IncomingCallExpandedContent] — the caller (below the
 * camera) over full-width Take / Hang up buttons, matching the fuller shape from [asCallCutout]. Live
 * state (photo, caller number, connected-or-ringing, duration start) is read from [OnCallBus].
 */
@Composable
private fun CallNormalContent(
    event: IslandEvent,
    onAction: (IslandAction) -> Unit,
) {
    val call = event.call ?: return
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val incoming = onCall?.ongoing == false
    // The two-row layout only earns its extra height when there are buttons to fill the second row.
    val hasActions = call.showActions && event.actions.isNotEmpty()
    if (incoming && call.incomingExpandedLayout && hasActions) {
        IncomingCallExpandedContent(event = event, call = call, onCall = onCall, onAction = onAction)
    } else {
        CallSingleRowContent(event = event, call = call, onCall = onCall, incoming = incoming, onAction = onAction)
    }
}

/**
 * The compact single-row call layout. Left to right: photo, the caller text, and the call button(s).
 * A connected call shows the duration over the caller name and one hang-up button; an [incoming] call
 * shows just the caller label (the contact name if known, otherwise the number — never both) and two
 * buttons, decline then answer.
 */
@Composable
private fun CallSingleRowContent(
    event: IslandEvent,
    call: CallTileOptions,
    onCall: OnCall?,
    incoming: Boolean,
    onAction: (IslandAction) -> Unit,
) {
    val photo = onCall?.photo?.takeIf { call.showPhoto }
    // The decline / hang-up (destructive) action; fall back to the first action if the dialer flags none.
    val hangUp = event.actions.firstOrNull { it.destructive } ?: event.actions.firstOrNull()
    val answer = event.actions.firstOrNull { it.answer }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = CALL_ROW_PADDING_DP.dp,
                end = CALL_ROW_PADDING_DP.dp,
                // An incoming call sits its content at the bottom so the single caller label clears
                // the camera hole at the pill's top edge; a connected call stays vertically centred.
                bottom = if (incoming) CALL_ROW_PADDING_DP.dp else 0.dp,
            ),
        verticalAlignment = if (incoming) Alignment.Bottom else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CALL_ROW_SPACING_DP.dp),
    ) {
        if (photo != null) {
            ContactPhoto(bitmap = photo, size = CALL_AVATAR_DP.dp)
        } else {
            IconBadge(event = event, badgeSize = CALL_AVATAR_DP.dp, iconSize = 24.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            // Connected calls put the ticking duration above the name; an incoming call shows only
            // the caller label (name or number), so there is nothing to stack above it.
            if (!incoming) {
                AnimatedVisibility(visible = call.showDuration) {
                    CallStatus(onCall = onCall)
                }
            }
            Text(
                text = event.label,
                color = LocalContentColor.current,
                fontSize = CALL_NAME_SIZE_SP.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (call.showActions) {
            if (incoming) {
                if (answer != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.Call,
                        description = "Answer",
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onAction(answer) },
                    )
                }
                if (hangUp != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.CallEnd,
                        description = "Decline",
                        container = MaterialTheme.colorScheme.error,
                        content = MaterialTheme.colorScheme.onError,
                        onClick = { onAction(hangUp) },
                    )
                }
            } else if (hangUp != null) {
                val fill = call.hangUpColor.resolve()
                CallCircleButton(
                    icon = Icons.Rounded.CallEnd,
                    description = "Hang up",
                    container = fill,
                    content = if (fill.luminance() > 0.5f) PillTextColorDark else PillTextColor,
                    onClick = { onAction(hangUp) },
                )
            }
        }
    }
}

/**
 * The incoming-call two-row layout, sized to the expanded cutout plus [callIncomingExtraDp]. Top (below
 * a camera-clearing pad): the caller's photo and a single label — their contact name if they have one,
 * otherwise their number. Bottom (pinned to the edge, a flexible gap between): full-width Take (answer,
 * primary) and Hang up (decline, red) buttons, degrading to a single full-width button if the dialer
 * exposes only one of the two actions.
 */
@Composable
private fun IncomingCallExpandedContent(
    event: IslandEvent,
    call: CallTileOptions,
    onCall: OnCall?,
    onAction: (IslandAction) -> Unit,
) {
    val photo = onCall?.photo?.takeIf { call.showPhoto }
    val hangUp = event.actions.firstOrNull { it.destructive } ?: event.actions.firstOrNull()
    val answer = event.actions.firstOrNull { it.answer }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = CALL_INCOMING_SIDE_PAD_DP.dp,
                end = CALL_INCOMING_SIDE_PAD_DP.dp,
                top = CALL_INCOMING_TOP_PAD_DP.dp,
                bottom = CALL_INCOMING_BOTTOM_PAD_DP.dp,
            ),
    ) {
        // Caller row — pinned just below the top camera clearance, so the single label (already the
        // name when known, else the number) sits clear of the camera hole.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CALL_ROW_SPACING_DP.dp),
        ) {
            if (photo != null) {
                ContactPhoto(bitmap = photo, size = CALL_INCOMING_AVATAR_DP.dp)
            } else {
                IconBadge(event = event, badgeSize = CALL_INCOMING_AVATAR_DP.dp, iconSize = 24.dp)
            }
            Text(
                text = event.label,
                modifier = Modifier.weight(1f),
                color = LocalContentColor.current,
                fontSize = CALL_NAME_SIZE_SP.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // A flexible gap pushes the button row down to the bottom edge.
        Spacer(modifier = Modifier.weight(1f))
        // Button row — Take (answer) then Hang up (decline), each filling half the width.
        if (call.showActions && (answer != null || hangUp != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CALL_INCOMING_BUTTON_GAP_DP.dp),
            ) {
                if (answer != null) {
                    CallWideButton(
                        icon = Icons.Rounded.Call,
                        label = stringResource(R.string.phone_answer),
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(answer) },
                    )
                }
                if (hangUp != null) {
                    CallWideButton(
                        icon = Icons.Rounded.CallEnd,
                        label = stringResource(R.string.phone_hang_up),
                        container = MaterialTheme.colorScheme.error,
                        content = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(hangUp) },
                    )
                }
            }
        }
    }
}

/** A full-width, filled call button (Take / Hang up) with a leading icon and label, used by the
 *  incoming-call layout's bottom row. */
@Composable
private fun CallWideButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = modifier
            .height(CALL_INCOMING_BUTTON_DP.dp)
            .pressScale(interaction),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A round, filled call button (hang up / decline / answer) on the trailing edge of the call cutout. */
@Composable
private fun CallCircleButton(
    icon: ImageVector,
    description: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content,
        ),
        modifier = Modifier
            .size(CALL_HANGUP_BUTTON_DP.dp)
            .pressScale(interaction),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The phone tile's secondary line: a duration that ticks up once connected, else "incoming call". */
@Composable
private fun CallStatus(onCall: OnCall?) {
    val start = onCall?.startTimeMs
    val text = if (start != null) {
        var now by remember(start) { mutableStateOf(System.currentTimeMillis()) }
        LaunchedEffect(start) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1_000L)
            }
        }
        formatCallDuration(((now - start) / 1_000L).coerceAtLeast(0L))
    } else if (onCall != null) {
        stringResource(R.string.phone_ringing)
    } else {
        return
    }
    Text(
        text = text,
        color = LocalContentColor.current.copy(alpha = 0.70f),
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The timer tile's expanded layout: a timer icon + the ticking remaining time, and (when enabled)
 * the Reset / Add 1 min chips. The countdown is read live from [RunningTimerBus]; the chips fire the
 * clock app's own notification actions, coloured by [TimerTileOptions] (Reset apart from Add 1 min).
 */
@Composable
private fun TimerExpandedContent(
    event: IslandEvent,
    appearance: AppearanceSettings,
    topMarginDp: Int = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
    onAction: (IslandAction) -> Unit,
) {
    val timer = event.timer ?: return

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = topMarginDp.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
                Column(modifier = Modifier.weight(1f)) {
                    // The remaining time is the headline; the timer's name (or "Timer") sits beneath.
                    Text(
                        text = timerRemainingText() ?: event.label,
                        color = LocalContentColor.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = event.label,
                        color = LocalContentColor.current.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (timer.showActions && event.actions.isNotEmpty()) {
                // A reset / stop button gets its own colour; every other button shares the second.
                val resetFill = timer.resetColor.resolve()
                val addFill = timer.addButtonColor.resolve()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    event.actions.take(3).forEach { action ->
                        ActionChip(
                            action = action,
                            style = appearance.actionButtonStyle,
                            fill = if (action.destructive) resetFill else addFill,
                            heightDp = appearance.actionButtonHeightDp,
                            onClick = { onAction(action) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The assistant tile's expanded layout: an assistant icon + the voice response text, wrapped in a
 * scrollable column constrained by max cutout height percentage.
 */
@Composable
private fun AssistantExpandedContent(
    event: IslandEvent,
    showActions: Boolean,
    appearance: AppearanceSettings,
    onDismiss: () -> Unit,
    onHeightMeasured: ((Int) -> Unit)? = null,
) {
    val assistant = event.assistant ?: return
    val contentColor = LocalContentColor.current
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val maxCutoutHeightDp = (configuration.screenHeightDp * assistant.maxCutoutHeightPercent / 100).dp
    val maxHeaderWidthDp = (configuration.screenWidthDp * 0.47f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxCutoutHeightDp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Measure the scroll *content*, not the viewport. The scroll container above is clamped
                // to the island's live (animating) height, so measuring it would feed that height back
                // into its own fit-to-content target — the loop that made the cutout bob as the answer
                // streamed in. The content column is laid out with unbounded height, so its reported
                // height is the answer's true natural height, independent of the surrounding animation.
                .onGloballyPositioned { coordinates ->
                    val hDp = (coordinates.size.height / density).toInt()
                    if (hDp > 0) {
                        onHeightMeasured?.invoke(hDp + 28)
                    }
                },
        ) {
            // Title header ("Assistant") constrained to max 47% screen width so it never goes behind camera hole
            Row(
                modifier = Modifier.widthIn(max = maxHeaderWidthDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(event = event, badgeSize = 36.dp, iconSize = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = event.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Answer content text displayed below title header
            if (assistant.displayAnswerInCutout) {
                Spacer(Modifier.height(8.dp))
                val textToDisplay = assistant.answerText.takeIf { !it.isNullOrBlank() } ?: "Assistant active..."
                Text(
                    text = textToDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.88f),
                )
            }

            // Close action button at the end, obeying action button settings
            if (showActions) {
                Spacer(Modifier.height(14.dp))
                val chipFill = appearance.actionButtonColor?.resolve() ?: event.accent
                val full = appearance.actionButtonAlignment == ActionButtonAlignment.FULL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, appearance.actionButtonAlignment.toHorizontal()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionChip(
                        action = IslandAction(label = "Close"),
                        style = appearance.actionButtonStyle,
                        fill = chipFill,
                        heightDp = appearance.actionButtonHeightDp,
                        onClick = onDismiss,
                        modifier = if (full) Modifier.weight(1f) else Modifier,
                    )
                }
            }
        }
    }
}

/** Formats elapsed call seconds as m:ss, or h:mm:ss once the call passes an hour. */
private fun formatCallDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** The caller's contact photo, cropped to a circle. Mirrors [AlbumArt] without the spin. */
@Composable
private fun ContactPhoto(bitmap: ImageBitmap, size: Dp, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

/**
 * Album art, cropped to fill. Normally a rounded square; when [rotate] is on it becomes a disc that
 * spins ([ALBUM_SPIN_MS] per turn) while [playing], freezing at its current angle when paused. A
 * non-null [strokeColor] rings the cover, set apart from it by a small gap.
 */
@Composable
private fun AlbumArt(
    bitmap: ImageBitmap,
    size: Dp,
    modifier: Modifier = Modifier,
    rotate: Boolean = false,
    playing: Boolean = false,
    /** Colour of the ring drawn around the cover, or null to leave it bare. */
    strokeColor: Color? = null,
) {
    val angle = remember { Animatable(0f) }
    // Spin only while enabled and playing; on pause the effect cancels and the angle holds. Restart
    // repeats identical 0→360 turns from the held value, so a pause/resume is seamless.
    LaunchedEffect(rotate, playing) {
        if (rotate && playing) {
            angle.animateTo(
                targetValue = angle.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = ALBUM_SPIN_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
        }
    }
    // The ring sits inside the cover's existing footprint rather than around it, so switching it on
    // never grows the badge or shoves the pill's text along. Without a ring the artwork fills the
    // footprint exactly as before.
    val strokeWidth = if (strokeColor != null) size * ALBUM_STROKE_FRACTION else 0.dp
    val gap = if (strokeColor != null) size * ALBUM_STROKE_GAP_FRACTION else 0.dp
    val coverSize = size - (strokeWidth + gap) * 2

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (strokeColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // A spinning square would visibly swing its corners, so a rotatable cover — and
                    // the ring tracking it — is drawn as a circle.
                    .border(strokeWidth, strokeColor, albumArtShape(rotate, size)),
            )
        }
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(coverSize)
                .rotate(if (rotate) angle.value else 0f)
                .clip(albumArtShape(rotate, coverSize)),
        )
    }
}

/** Circle for a spinning cover, else a rounded square whose radius scales with [size]. */
private fun albumArtShape(rotate: Boolean, size: Dp) =
    if (rotate) CircleShape else RoundedCornerShape(size * 0.24f)

@Composable
private fun IconBadge(
    event: IslandEvent,
    badgeSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    // A tile's chosen container colour wins: a filled disc with contrasting ink. A per-event colour
    // override then recolours the default look (a faint tinted disc + full-colour glyph). Otherwise
    // "Dynamic color for all events" gives a role-coloured badge with its matching "on" ink, and the
    // plain default is a faint accent-tinted disc behind a full-accent glyph.
    val container = event.iconContainerColor
    val override = event.colorOverride
    val badgeColor: Color
    val glyphColor: Color
    when {
        container != null -> {
            badgeColor = container.resolve()
            glyphColor = when (container) {
                is CutoutColor.Dynamic -> onDynamicRole(container.role)
                is CutoutColor.Solid ->
                    if (badgeColor.luminance() > 0.5f) PillTextColorDark else PillTextColor
            }
        }

        override != null -> {
            val tint = override.resolve()
            badgeColor = tint.copy(alpha = 0.20f)
            glyphColor = tint
        }

        event.useThemeColor -> {
            badgeColor = MaterialTheme.colorScheme.forRole(event.themeColorRole)
                .copy(alpha = event.themeColorOpacity)
            glyphColor = MaterialTheme.colorScheme.onForRole(event.themeColorRole)
        }

        else -> {
            badgeColor = event.accent.copy(alpha = 0.20f)
            glyphColor = event.accent
        }
    }
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        when (val icon = event.icon) {
            is IslandIcon.Vector -> Icon(
                imageVector = icon.image,
                contentDescription = null,
                tint = glyphColor,
                modifier = Modifier.size(iconSize),
            )

            // Full-colour art fills the badge disc; a monochrome glyph (a notification's small
            // icon) is drawn at glyph size in the badge's ink instead, like a vector icon.
            is IslandIcon.Raster -> androidx.compose.foundation.Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                contentScale = if (icon.tint) ContentScale.Fit else ContentScale.Crop,
                colorFilter = if (icon.tint) ColorFilter.tint(glyphColor) else null,
                modifier = if (icon.tint) {
                    Modifier.size(iconSize)
                } else {
                    Modifier.size(badgeSize * 0.78f).clip(CircleShape)
                },
            )

            is IslandIcon.Lottie -> {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(icon.resId),
                )
                // Each animation carries its own playback: the unlock padlock clips to frames 0..45
                // and holds open, while a looping icon (e.g. charging) runs its full range forever.
                val clip = if (icon.clipStartFrame != null && icon.clipEndFrame != null) {
                    LottieClipSpec.Frame(icon.clipStartFrame, icon.clipEndFrame)
                } else {
                    null
                }
                // Recolour every layer to the badge glyph colour (the settings' role/accent) when asked.
                val dynamicProperties = if (icon.tint) {
                    rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.COLOR_FILTER,
                            value = SimpleColorFilter(glyphColor.toArgb()),
                            keyPath = arrayOf("**"),
                        ),
                    )
                } else {
                    null
                }
                LottieAnimation(
                    composition = composition,
                    iterations = icon.iterations,
                    clipSpec = clip,
                    dynamicProperties = dynamicProperties,
                    // requiredSize (not size) so a scale > 1 can render past the badge bounds instead of
                    // being clamped to them; the overflow is clipped to the badge circle by the parent.
                    modifier = Modifier.requiredSize(iconSize * icon.scale),
                )
            }
        }
    }
}
