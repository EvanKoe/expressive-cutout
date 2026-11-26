package com.ekoehler.expressivecutout.overlay

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.data.EmptyPillInfoSettings
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.swe.LocalSweGroupPosition
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweGroupPosition
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.system.PermissionUsage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * The gap between neighbouring widgets as a fraction of the pill height: [ITEM_GAP_FRACTION] in
 * general, the narrower [GROUP_GAP_FRACTION] between two widgets that both draw a background, so they
 * read as one segmented pill.
 */
private const val ITEM_GAP_FRACTION = 0.2f
private const val GROUP_GAP_FRACTION = 0.08f

/** The outer-edge inset as a fraction of the pill height; equals the gap above and below a badge (1 - 0.72) / 2, so round widgets sit evenly. */
private const val EDGE_INSET_FRACTION = 0.14f
private const val MINUTE_MS = 60_000L

/** How long the items wait after the resting pill appears, so it can settle before they fade in. */
private const val ENTER_DELAY_MS = 250L

/** How long one item takes to fade and scale in or out. */
private const val ITEM_FADE_MS = 220

/** The scale an item grows from as it appears, and shrinks to as it leaves. */
private const val ITEM_INITIAL_SCALE = 0.8f

/** How an info item appears: a fade with a slight grow, opening its slot in the row. */
private val ItemEnter = fadeIn(tween(ITEM_FADE_MS)) + scaleIn(tween(ITEM_FADE_MS), ITEM_INITIAL_SCALE) +
    expandHorizontally(tween(ITEM_FADE_MS))

/** How an info item leaves: the reverse of [ItemEnter]. */
private val ItemExit = fadeOut(tween(ITEM_FADE_MS)) + scaleOut(tween(ITEM_FADE_MS), ITEM_INITIAL_SCALE) +
    shrinkHorizontally(tween(ITEM_FADE_MS))

/**
 * The room each side of the camera hole needs for the resting pill's widgets, in dp, as (left,
 * right). Pure and Paint-based; a clock is measured on its widest possible string so the pill never
 * changes width as the minutes tick. [dotsLeftDp] / [dotsRightDp] is the room the permission dots take
 * on a side (see [emptyPillDotRoomDp]), on top of its widgets. A side with nothing visible is 0, and
 * when both are 0 the pill is left as the user sized it.
 *
 * The Roboto Flex resource can't be loaded without a Context, so a clock is measured on the system
 * font at the same weight, scaled by the width axis's approximate effect, plus a safety margin.
 *
 * A side holding a visible flexible widget ([SweWidget.isFlexible]) is at least [sideRoomDp] wide (see
 * `emptyInfoSideRoomDp`), so the pill reaches the screen edge; other sides are unaffected.
 */
internal fun emptyPillSideWidthsDp(
    settings: EmptyPillInfoSettings,
    heightDp: Int,
    density: Float,
    data: SweData,
    dotsLeftDp: Float = 0f,
    dotsRightDp: Float = 0f,
    sideRoomDp: Float = 0f,
): Pair<Float, Float> {
    fun sideWidth(side: InfoSide): Float {
        val visible = settings.on(side).filter { it.isVisible(data) }
        val widths = visible.map { it.widthDp(heightDp, density, data) }
        val dots = if (side == InfoSide.LEFT) dotsLeftDp else dotsRightDp
        if (widths.isEmpty() && dots <= 0f) return 0f
        val gaps = visible.indices.sumOf { gapBeforeDp(visible.getOrNull(it - 1), visible[it], heightDp).toDouble() }.toFloat()
        val natural = widths.sum() + gaps + heightDp * EDGE_INSET_FRACTION + dots
        return if (visible.any { it.isFlexible }) max(natural, sideRoomDp) else natural
    }
    return sideWidth(InfoSide.LEFT) to sideWidth(InfoSide.RIGHT)
}

/**
 * The gap in dp before [widget] when [previous] is the visible widget to its left (null for the first
 * one): none for the first, [GROUP_GAP_FRACTION] between two background widgets, else
 * [ITEM_GAP_FRACTION]. The single source of truth for gaps, shared by the row and its measurement.
 */
private fun gapBeforeDp(previous: SweWidget?, widget: SweWidget, heightDp: Int): Float = when {
    previous == null -> 0f
    previous.hasBackground && widget.hasBackground -> heightDp * GROUP_GAP_FRACTION
    else -> heightDp * ITEM_GAP_FRACTION
}

/**
 * Where [widget] sits in a run of adjacent background widgets among the [visible] ones: [SINGLE][SweGroupPosition.SINGLE]
 * when it, or both its visible neighbours, have no background.
 */
internal fun groupPosition(visible: List<SweWidget>, widget: SweWidget): SweGroupPosition {
    val index = visible.indexOf(widget)
    if (index < 0 || !widget.hasBackground) return SweGroupPosition.SINGLE
    val joinsPrevious = visible.getOrNull(index - 1)?.hasBackground == true
    val joinsNext = visible.getOrNull(index + 1)?.hasBackground == true
    return when {
        joinsPrevious && joinsNext -> SweGroupPosition.MIDDLE
        joinsPrevious -> SweGroupPosition.END
        joinsNext -> SweGroupPosition.START
        else -> SweGroupPosition.SINGLE
    }
}

/**
 * Whether the resting pill shows any widget right now. When it shows none, the pill stays centred on
 * the camera and only grows if the permission dots don't fit as it is.
 */
internal fun emptyPillHasItems(settings: EmptyPillInfoSettings, data: SweData): Boolean =
    (settings.left + settings.right).any { it.isVisible(data) }

/** The pill's inset from its rounded outer edge on a side that shows something, in dp. */
internal fun emptyPillEdgeInsetDp(heightDp: Int): Float = heightDp * EDGE_INSET_FRACTION

/**
 * The room the lit permission dots need on the resting pill's side: their row plus one gap, so the
 * side's widgets sit clear of them. Zero when no dot is lit.
 */
internal fun emptyPillDotRoomDp(usage: PermissionUsage, heightDp: Int, vertical: Boolean): Float {
    if (usage.count == 0) return 0f
    return permissionDotRowWidthDp(usage, heightDp, vertical) + heightDp * DOT_GAP_FRACTION
}

/**
 * The resting (event-less) pill's widgets, split between the left and right of the camera hole in
 * the order [settings] lists them. Sized with [emptyPillSideWidthsDp], which must be given the same
 * inputs. This overlay pill is the documented exception to the no-hardcoded-colours rule: its
 * defaults are fixed light/dark inks picked by luminance, since the pill sits on the user's own
 * backdrop rather than the app theme.
 *
 * @param enterDelayMs how long the widgets wait before fading in; 0 shows them at once (previews).
 * @param sideRoomDp the room a flexible widget's side fills, as given to [emptyPillSideWidthsDp].
 */
@Composable
internal fun EmptyPillInfoContent(
    settings: EmptyPillInfoSettings,
    heightDp: Int,
    data: SweData,
    dotsLeftDp: Float = 0f,
    dotsRightDp: Float = 0f,
    enterDelayMs: Long = ENTER_DELAY_MS,
    sideRoomDp: Float = 0f,
) {
    // Held back until the pill has mostly shrunk from whatever it was showing, so the widgets fade in
    // on the settled pill instead of riding its edges as it collapses
    var settled by remember { mutableStateOf(enterDelayMs <= 0L) }
    LaunchedEffect(Unit) {
        delay(enterDelayMs)
        settled = true
    }

    val density = LocalDensity.current.density
    val (leftWidthDp, rightWidthDp) = remember(settings, heightDp, data, dotsLeftDp, dotsRightDp, sideRoomDp, density) {
        emptyPillSideWidthsDp(settings, heightDp, density, data, dotsLeftDp, dotsRightDp, sideRoomDp)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Left group
        InfoSideRow(
            side = InfoSide.LEFT,
            widgets = settings.left,
            settled = settled,
            heightDp = heightDp,
            data = data,
            dotsDp = dotsLeftDp,
            flexWidthDp = leftWidthDp.takeIf { settings.left.hasVisibleFlexible(data) },
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // Right group
        InfoSideRow(
            side = InfoSide.RIGHT,
            widgets = settings.right,
            settled = settled,
            heightDp = heightDp,
            data = data,
            dotsDp = dotsRightDp,
            flexWidthDp = rightWidthDp.takeIf { settings.right.hasVisibleFlexible(data) },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** Whether any widget in this list is flexible and visible right now. */
private fun List<SweWidget>.hasVisibleFlexible(data: SweData): Boolean = any { it.isFlexible && it.isVisible(data) }

/**
 * One side's widgets, in list order, with the edge inset on the pill's outer side. [flexWidthDp] is
 * the side's total width, inset and dots included; non-null only when it holds a flexible widget, so
 * that widget's weight has room to fill.
 */
@Composable
private fun InfoSideRow(
    side: InfoSide,
    widgets: List<SweWidget>,
    settled: Boolean,
    heightDp: Int,
    data: SweData,
    dotsDp: Float,
    flexWidthDp: Float?,
    modifier: Modifier = Modifier,
) {
    // The dots sit on the outer edge, so the widgets step inward past them
    val inset = (heightDp * EDGE_INSET_FRACTION + dotsDp).dp
    val visible = widgets.filter { it.isVisible(data) }
    Row(
        modifier = modifier.then(if (flexWidthDp != null) Modifier.width(flexWidthDp.dp) else Modifier).padding(
            start = if (side == InfoSide.LEFT) inset else 0.dp,
            end = if (side == InfoSide.RIGHT) inset else 0.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        widgets.forEach { widget ->
            val index = visible.indexOf(widget)
            val gapDp = if (index < 0) 0f else gapBeforeDp(visible.getOrNull(index - 1), widget, heightDp)
            // Keyed by id so reordering moves a widget instead of restarting its animation
            key(widget.id) {
                WidgetSlot(widget, settled, heightDp, data, gapDp, groupPosition(visible, widget))
            }
        }
    }
}

/**
 * One widget that fades and scales in and out on its own, and only once [settled]. [gapDp] is the gap
 * before it, kept inside the animated slot so it opens and closes with the widget; [position] is
 * handed to the widget through [LocalSweGroupPosition].
 */
@Composable
private fun RowScope.WidgetSlot(
    widget: SweWidget,
    settled: Boolean,
    heightDp: Int,
    data: SweData,
    gapDp: Float,
    position: SweGroupPosition,
) {
    AnimatedVisibility(
        visible = settled && widget.isVisible(data),
        modifier = if (widget.isFlexible) Modifier.weight(1f) else Modifier,
        enter = ItemEnter,
        exit = ItemExit,
    ) {
        // Widget with its leading gap
        Box(modifier = Modifier.padding(start = gapDp.dp)) {
            CompositionLocalProvider(LocalSweGroupPosition provides position) {
                widget.Content(data, heightDp)
            }
        }
    }
}

/**
 * The current time as "HH:mm" (24-hour setting) or "h:mm", refreshed on each minute boundary. Polls
 * by delaying to the next minute rather than registering a receiver, so there is nothing to undo.
 */
@Composable
internal fun rememberMinuteClock(): String {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val format = remember(is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()) }
    val time by produceState(initialValue = format.format(Date()), format) {
        while (true) {
            val now = System.currentTimeMillis()
            value = format.format(Date(now))
            delay(MINUTE_MS - now % MINUTE_MS)
        }
    }
    return time
}