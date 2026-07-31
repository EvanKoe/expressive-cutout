package com.ekoehler.expressivecutout.overlay

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
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
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.MediaArtBus
import com.ekoehler.expressivecutout.core.NowPlaying
import com.ekoehler.expressivecutout.core.NowPlayingBus
import com.ekoehler.expressivecutout.core.OnCall
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimerBus
import com.ekoehler.expressivecutout.data.ActionButtonAlignment
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.SentAlignment
import com.ekoehler.expressivecutout.data.AnimationBounce
import com.ekoehler.expressivecutout.data.AnimationSpeed
import com.ekoehler.expressivecutout.data.AnimationStyle
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.CALL_MAX_WIDTH_PERCENT
import com.ekoehler.expressivecutout.data.CALL_MIN_WIDTH_PERCENT
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.asCallCutout
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.data.SwipeDismissDirection
import com.ekoehler.expressivecutout.data.SwipeDismissTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Text colours for a dark fill; on a light fill we swap in a dark text colour (see contentColorFor).
private val PillTextColor = Color(0xFFF5F5F5)
private val PillTextColorDark = Color(0xFF0A0A0A)

/** Fallback fill for a button asked to be [MusicButtonStyle.filled] before the user picks a colour. */
private val MusicButtonFilledDefault = Color(0xFFE0E0E0)

// Material 3 expressive "emphasized" easing — cubic-bezier(0.2, 0.0, 0.0, 1.0).
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

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
    animationDurationMs: Int,
    autoCollapse: Boolean,
    autoCollapseMs: Long,
    appearance: AppearanceSettings,
    showActions: Boolean,
    shrinkOnSwipeUp: Boolean,
    swipeToDismiss: Boolean,
    swipeDismissDirection: SwipeDismissDirection,
    swipeDismissTarget: SwipeDismissTarget,
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

    val initialExpandedState = if (forcedExpanded == false) false else (shownEvent?.initiallyExpanded ?: false)
    var tapExpanded by remember(shownEvent?.id, forcedExpanded) { mutableStateOf(initialExpandedState) }
    // The reply action currently being typed for, if any. Reset when the event changes.
    var replyingTo by remember(shownEvent?.id) { mutableStateOf<IslandAction?>(null) }
    val replying = replyingTo != null
    // Non-null immediately after the user hits send: the island shows a brief "Sent" confirmation,
    // then this action/text pair is dispatched (which dismisses the island).
    var sentReply by remember(shownEvent?.id) { mutableStateOf<Pair<IslandAction, String>?>(null) }
    val confirmingSent = sentReply != null
    // The phone tile has no expanded state: it is shown as one bigger "normal" cutout, so tapping
    // never expands it and its size never switches to the expanded dimensions.
    val isCall = shownEvent?.call != null
    val isExpanded = if (isCall || forcedExpanded == false) false else (forcedExpanded ?: tapExpanded)
    val boopScale = remember { Animatable(1f) }
    // Horizontal drag offset for swipe-to-dismiss; reset for each new event so a fresh pill starts centred.
    val dismissOffsetX = remember(shownEvent?.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // One user-tunable knob scales every tween-based island animation — the expand/collapse, the
    // pop-in/out reveal, the background fade, the content crossfade and the tap "boop" — in
    // proportion to its tuned baseline, so the whole motion speeds up or slows down together.
    // At 0ms everything snaps instantly; at the default (BASE_TRANSITION_MS) the feel is unchanged.
    val animScale = animationDurationMs / BASE_TRANSITION_MS.toFloat()
    fun scaled(baseMs: Int) = (baseMs * animScale).roundToInt()

    // The island's primary motion (reveal, size / position / corners, background fade) follows the
    // configured style: expressive spatial springs (speed picked by the user, durations ignored) or
    // a standard ease-in-out tween scaled by the animation-duration slider. IslandMotion is shared
    // with the settings example pills, so the preview there shows exactly this motion.
    val motion = remember(animationStyle, animationSpeed, animationBounce, animationDurationMs) {
        IslandMotion(animationStyle, animationSpeed, animationBounce, animationDurationMs)
    }

    // Tell the controller to make the window focusable (for the keyboard) and pause dismissal.
    LaunchedEffect(replying) { onReplyActiveChange(replying) }
    LaunchedEffect(isExpanded, event != null) {
        if (event != null) onExpandedChange(isExpanded)
    }
    // User-expanded (not the pinned preview) optionally collapses after the delay — never while
    // a reply is being typed or its "sent" confirmation is still showing.
    LaunchedEffect(tapExpanded, forcedExpanded, autoCollapse, autoCollapseMs, replying, confirmingSent) {
        if (forcedExpanded == null && tapExpanded && autoCollapse && !replying && !confirmingSent) {
            delay(autoCollapseMs)
            tapExpanded = false
        }
    }

    // Only pad the height when the expanded island will actually render a bottom row of controls —
    // notification action chips, the music tile's playback buttons, or the phone tile's call actions.
    val hasActions = showActions && (shownEvent?.actions?.isNotEmpty() == true)
    val hasMediaControls = shownEvent?.media?.showControls == true
    val hasCallActions = shownEvent?.call?.showActions == true && (shownEvent?.actions?.isNotEmpty() == true)
    val hasTimerActions = shownEvent?.timer?.showActions == true && (shownEvent?.actions?.isNotEmpty() == true)
    // An incoming call in its two-row layout puts the buttons on their own row (none trailing); a
    // one-line incoming shows two trailing buttons (decline + answer); a connected call shows one
    // (hang up). Read live from the call bus so the pill re-sizes when the call is answered.
    val liveCall by OnCallBus.state.collectAsStateWithLifecycle()
    val callIncoming = isCall && liveCall?.ongoing == false
    val callTwoRow = callIncoming && shownEvent?.call?.incomingExpandedLayout == true && hasCallActions
    val callTrailingButtons = when {
        !isCall || !hasCallActions -> 0
        callTwoRow -> 0
        callIncoming -> 2
        else -> 1
    }
    // The call cutout widens to fit a long caller name (up to its max); measured once per name/state.
    // Any incoming call is pinned to the full width; the two-row layout also uses the taller shape.
    val density = LocalDensity.current.density
    val callWidthPercent = remember(isCall, shownEvent?.label, callTrailingButtons, callIncoming, displayWidthDp, density) {
        if (isCall && shownEvent != null) {
            callCutoutWidthPercent(shownEvent.label, callTrailingButtons, callIncoming, displayWidthDp, density)
        } else {
            CALL_MIN_WIDTH_PERCENT
        }
    }
    val dims = when {
        // The two-row incoming layout starts from the expanded cutout and grows by its button row.
        callTwoRow -> expanded
        isCall -> collapsed.asCallCutout(callWidthPercent)
        isExpanded -> expanded
        else -> collapsed
    }
    val heightBonus = when {
        isExpanded && (hasActions || hasMediaControls || hasCallActions || hasTimerActions) ->
            expandedActionsExtraDp(appearance.actionButtonHeightDp)
        // The incoming two-row layout grows past the expanded height so its Take / Hang up row has its
        // own space below a caller row that clears the camera hole (calls never enter the expanded state).
        callTwoRow -> callIncomingExtraDp()
        else -> 0
    }
    // Appear / disappear reveal: the cutout emerges as a small, camera-sized dot and stretches out
    // horizontally to its full width, then shrinks back into the dot when it's dismissed. `reveal`
    // runs 0 (dot) → 1 (full pill); it eases in on show and back out on hide.
    val present = event != null
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(present) {
        reveal.animateTo(
            targetValue = if (present) 1f else 0f,
            animationSpec = motion.float(baseMs = if (present) 320 else 200),
        )
    }
    // While the pill is fully hidden (reveal at 0) the size / position / corners snap straight to the
    // next state instead of animating: a cutout dismissed while expanded resets to its normal height
    // off-screen, so the next appearance grows from the dot at the right height with no catch-up lag.
    val spec: AnimationSpec<Dp> = if (reveal.value == 0f) snap() else motion.dp()
    val width by animateDpAsState(
        if (isStickToCamera) collapsed.heightDp.dp else (displayWidthDp * dims.widthPercent / 100f).dp,
        spec, label = "islandWidth"
    )
    val height by animateDpAsState(
        if (isStickToCamera) (collapsed.heightDp * 2.2f).dp else (dims.heightDp + heightBonus).dp,
        spec, label = "islandHeight"
    )
    val cornerRadius = (collapsed.heightDp / 2f).dp
    val offsetX by animateDpAsState(if (isStickToCamera) 0.dp else dims.offsetXDp.dp, spec, label = "islandOffsetX")
    val offsetY by animateDpAsState(if (isStickToCamera) 0.dp else dims.offsetYDp.dp, spec, label = "islandOffsetY")
    val topLeft by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerTopLeftDp.dp, spec, label = "cornerTL")
    val topRight by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerTopRightDp.dp, spec, label = "cornerTR")
    val bottomLeft by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerBottomLeftDp.dp, spec, label = "cornerBL")
    val bottomRight by animateDpAsState(if (isStickToCamera) cornerRadius else dims.cornerBottomRightDp.dp, spec, label = "cornerBR")
    // Drives the background cross-fade between the normal and expanded fills, in step with the size.
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = motion.fade(),
        label = "islandBackgroundFade",
    )

    // The dot's diameter is the cutout's own (collapsed) height — the camera-sized nub it grows from —
    // so the normal cutout keeps that height throughout and only its width expands. Corners stay fully
    // round while it's a dot and ease to the configured shape as it opens out.
    val dotDp = collapsed.heightDp.dp
    val revealWidth = lerpDp(dotDp, width, reveal.value)
    val revealHeight = lerpDp(dotDp, height, reveal.value)
    val dotCorner = dotDp / 2
    val revealTopLeft = lerpDp(dotCorner, topLeft, reveal.value)
    val revealTopRight = lerpDp(dotCorner, topRight, reveal.value)
    val revealBottomLeft = lerpDp(dotCorner, bottomLeft, reveal.value)
    val revealBottomRight = lerpDp(dotCorner, bottomRight, reveal.value)

    Box(modifier = Modifier.fillMaxSize()) {
        val stickAlignment = if (isRotation270) Alignment.CenterEnd else Alignment.CenterStart
        val stickPaddingStart = if (isStickToCamera && !isRotation270) offsetYDp.dp else 0.dp
        val stickPaddingEnd = if (isStickToCamera && isRotation270) offsetYDp.dp else 0.dp

        // Position the island in the full-size (non-clipping) window; then animate visibility.
        Box(
            modifier = Modifier
                .align(if (isStickToCamera) stickAlignment else Alignment.TopCenter)
                .padding(start = stickPaddingStart, end = stickPaddingEnd)
                .offset(x = if (isStickToCamera) 0.dp else offsetX, y = if (isStickToCamera) 0.dp else offsetY),
        ) {
            // Keep rendering through the exit (until `reveal` reaches 0) so the shrink-back animates.
            if (present || reveal.value > 0f) {
                IslandSurface(
                    modifier = Modifier
                        .width(revealWidth)
                        .height(revealHeight)
                        .graphicsLayer {
                            scaleX = boopScale.value
                            // Follow the finger during a dismiss swipe, fading as it slides away.
                            translationX = dismissOffsetX.value
                            val travel = abs(dismissOffsetX.value) / size.width.coerceAtLeast(1f)
                            // Fade the dot in/out quickly over the first/last fifth of the reveal so
                            // it never hard-pops on or off screen; combine with the swipe fade.
                            val revealAlpha = (reveal.value / 0.2f).coerceIn(0f, 1f)
                            alpha = (1f - travel).coerceIn(0.25f, 1f) * revealAlpha
                        }
                        .pointerInput(forcedExpanded, isExpanded, replying, shownEvent?.id) {
                            if (forcedExpanded == true) return@pointerInput
                            detectTapGestures {
                                // While typing a reply, ignore taps on the surface itself.
                                if (replying) return@detectTapGestures
                                // The phone tile is normal-only, so a tap never toggles it open;
                                // instead it opens the dialer's in-call screen (its content intent).
                                if (isCall) {
                                    if (shownEvent?.contentIntent != null) onActivate()
                                    return@detectTapGestures
                                }
                                // Once expanded, or in normal-only mode (forcedExpanded == false), tapping a notification opens its app.
                                if ((isExpanded || forcedExpanded == false) && shownEvent?.contentIntent != null) {
                                    onActivate()
                                } else if (forcedExpanded == null) {
                                    tapExpanded = !tapExpanded
                                    scope.launch {
                                        boopScale.animateTo(1.02f, tween(durationMillis = scaled(120), easing = EmphasizedEasing))
                                        boopScale.animateTo(1f, tween(durationMillis = scaled(BASE_TRANSITION_MS), easing = EmphasizedEasing))
                                    }
                                }
                            }
                        }
                        // Swipe up on the expanded island to shrink it back to the normal cutout.
                        .pointerInput(forcedExpanded, isExpanded, replying, shrinkOnSwipeUp, shownEvent?.id) {
                            if (forcedExpanded != null || !shrinkOnSwipeUp) return@pointerInput
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
                        .pointerInput(forcedExpanded, swipeToDismiss, swipeDismissDirection, swipeDismissTarget, isExpanded, replying, shownEvent?.id) {
                            val targetAllows = when (swipeDismissTarget) {
                                SwipeDismissTarget.BOTH -> true
                                SwipeDismissTarget.EXPANDED -> isExpanded
                                SwipeDismissTarget.NORMAL -> !isExpanded
                            }
                            if (forcedExpanded != null || !swipeToDismiss || replying || !targetAllows) return@pointerInput
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
                        shownEvent?.let { e ->
                            if (e.call != null) {
                                // The phone tile: one bigger normal cutout — caller on the left,
                                // hang-up on the right — with no separate expanded layout.
                                CallNormalContent(event = e, onAction = onAction)
                            } else if (showExpanded) {
                                ExpandedContent(
                                    event = e,
                                    showActions = showActions,
                                    appearance = appearance,
                                    replyingTo = replyingTo,
                                    replySent = confirmingSent,
                                    onAction = onAction,
                                    onStartReply = { replyingTo = it },
                                    onCancelReply = { replyingTo = null },
                                    onSendReply = { text ->
                                        // Swap the field for the "Sent" confirmation, then dispatch
                                        // the reply once it has been seen. Launched from the (un-keyed)
                                        // composition scope so a notification arriving mid-hold can't
                                        // cancel the send.
                                        replyingTo?.let { action ->
                                            sentReply = action to text
                                            scope.launch {
                                                delay(REPLY_SENT_FEEDBACK_MS)
                                                onReply(action, text)
                                            }
                                        }
                                        replyingTo = null
                                    },
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
    // Keep text/icons legible: dark ink on a light fill, otherwise the near-white default. The
    // reference colour tracks the fade so ink flips at the right moment when the states differ.
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
    // The Surface itself is transparent (so a gradient fill is possible); the fill is drawn by the
    // opaque child below, which also keeps the layer opaque so the elevation shadow still renders.
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
    }
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

@Composable
private fun ExpandedContent(
    event: IslandEvent,
    showActions: Boolean,
    appearance: AppearanceSettings,
    replyingTo: IslandAction?,
    replySent: Boolean,
    onAction: (IslandAction) -> Unit,
    onStartReply: (IslandAction) -> Unit,
    onCancelReply: () -> Unit,
    onSendReply: (String) -> Unit,
) {
    // The music tile has its own expanded layout (album art + playback controls).
    if (event.media != null) {
        MediaExpandedContent(event = event, buttonHeightDp = appearance.actionButtonHeightDp)
        return
    }
    // The timer tile: icon + ticking remaining time, and its Reset / Add 1 min chips.
    if (event.timer != null) {
        TimerExpandedContent(event = event, appearance = appearance, onAction = onAction)
        return
    }
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
            // Fall back to the notification's own accent / a neutral tint when unset.
            val sendColor = appearance.sendButtonColor?.resolve() ?: event.accent
            when {
                // Just sent: a brief confirmation replaces the field before the island dismisses.
                replySent -> ReplySentRow(
                    tint = sendColor,
                    heightDp = appearance.actionButtonHeightDp,
                    alignment = appearance.sentAlignment,
                )
                // Typing a reply: the input field replaces the chips until sent or cancelled.
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
                // Action chips (at most three fit comfortably); a reply chip opens the input,
                // any other chip fires its action.
                showActions && event.actions.isNotEmpty() -> {
                    // Chip fill follows the configured colour, or the notification's accent when unset.
                    val chipFill = appearance.actionButtonColor?.resolve() ?: event.accent
                    // Full-length: each chip takes an equal weighted share of the whole width; the
                    // other alignments size chips to content and position the row as a group.
                    val full = appearance.actionButtonAlignment == ActionButtonAlignment.FULL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, appearance.actionButtonAlignment.toHorizontal()),
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
                                modifier = if (full) Modifier.weight(1f) else Modifier,
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
    modifier: Modifier = Modifier,
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
        modifier = modifier
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
private fun MediaExpandedContent(event: IslandEvent, buttonHeightDp: Int) {
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
                if (hangUp != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.CallEnd,
                        description = "Decline",
                        container = MaterialTheme.colorScheme.error,
                        content = MaterialTheme.colorScheme.onError,
                        onClick = { onAction(hangUp) },
                    )
                }
                if (answer != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.Call,
                        description = "Answer",
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onAction(answer) },
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
    onAction: (IslandAction) -> Unit,
) {
    val timer = event.timer ?: return

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
