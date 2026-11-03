package com.ekoehler.expressivecutout.overlay

import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import com.ekoehler.expressivecutout.data.BatteryPercentage
import com.ekoehler.expressivecutout.data.EmptyPillInfoSettings
import com.ekoehler.expressivecutout.data.SweWidget
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.data.UnreadShape
import com.ekoehler.expressivecutout.events.BatteryStatus
import com.ekoehler.expressivecutout.service.UnreadCounts
import com.ekoehler.expressivecutout.system.PermissionUsage
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import com.ekoehler.expressivecutout.ui.components.rememberRobotoFlexFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Ink on a light fill. Same value the rest of the overlay uses; the pill is colour-exempt. */
private val INK_DARK = Color(0xFF0A0A0A)

/** Ink on a dark fill. */
private val INK_LIGHT = Color(0xFFF5F5F5)

/** The unread badge's shape colour when the user hasn't picked one. */
private val UNREAD_DEFAULT_FILL = Color(0xFFE53935)

/** The battery fill while charging, when the user hasn't picked a colour. */
private val BATTERY_CHARGING_FILL = Color(0xFF43A047)

/** The widest clock the pill will ever show; the side is sized for it so it never twitches. */
private const val CLOCK_WORST_CASE = "88:88"

/** The widest unread count shown; anything above is capped to this. */
private const val UNREAD_CAP_TEXT = "99+"

/** The widest battery percentage. */
private const val BATTERY_WORST_CASE = "100"

/** The percent sign optionally drawn after the level. */
private const val PERCENT_SIGN = "%"

/** How much the inside percentage shrinks when it carries a "%", so it still fits the body. */
private const val BATTERY_INSIDE_SIGN_SCALE = 0.78f

private const val UNREAD_CAP = 99
private const val PERCENT = 100f
private const val CLOCK_TEXT_FRACTION = 0.5f
private const val UNREAD_TEXT_FRACTION = 0.4f
private const val BATTERY_TEXT_FRACTION = 0.4f
/** The percentage drawn inside the body, as a share of the body's own height. */
private const val BATTERY_INSIDE_TEXT_FRACTION = 0.68f
private const val UNREAD_PILL_TEXT_PAD_FRACTION = 0.16f
private const val BATTERY_BODY_WIDTH_FRACTION = 0.64f
private const val BATTERY_BODY_HEIGHT_FRACTION = 0.34f
private const val BATTERY_INSIDE_BODY_WIDTH_FRACTION = 1.0f
private const val BATTERY_INSIDE_BODY_HEIGHT_FRACTION = 0.58f

/** The unread pill's height: the same as the battery body with its percentage inside, so the two line up. */
private const val UNREAD_PILL_HEIGHT_FRACTION = BATTERY_INSIDE_BODY_HEIGHT_FRACTION
private const val BATTERY_TIP_WIDTH_FRACTION = 0.05f
private const val BATTERY_TIP_HEIGHT_RATIO = 0.4f
private const val BATTERY_CORNER_RATIO = 0.3f
private const val BATTERY_TRACK_ALPHA = 0.3f
private const val BATTERY_TEXT_GAP_FRACTION = 0.1f
private const val ITEM_GAP_FRACTION = 0.2f
private const val EDGE_INSET_FRACTION = 0.22f

/** Extra room on a measured string so layout rounding and the real font never clip it. */
private const val MEASURE_SAFETY = 1.06f

/** How far the clock's rendered width swings from normal at the narrowest / widest `wdth` setting. */
private const val CLOCK_WIDTH_SWING = 0.3f

private const val STAR_LOBES = 10
private const val STAR_DEPTH = 0.07f
private const val STAR_STEPS = 120
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
 */
internal fun emptyPillSideWidthsDp(
    settings: EmptyPillInfoSettings,
    heightDp: Int,
    density: Float,
    counts: UnreadCounts,
    hasBattery: Boolean,
    dotsLeftDp: Float = 0f,
    dotsRightDp: Float = 0f,
): Pair<Float, Float> {
    fun sideWidth(side: InfoSide): Float {
        val widths = settings.on(side)
            .filter { it.isVisible(counts, hasBattery) }
            .map { it.widthDp(heightDp, density, counts) }
        val dots = if (side == InfoSide.LEFT) dotsLeftDp else dotsRightDp
        if (widths.isEmpty() && dots <= 0f) return 0f
        val gaps = (widths.size - 1).coerceAtLeast(0) * heightDp * ITEM_GAP_FRACTION
        return widths.sum() + gaps + heightDp * EDGE_INSET_FRACTION + dots
    }
    return sideWidth(InfoSide.LEFT) to sideWidth(InfoSide.RIGHT)
}

/**
 * Whether the resting pill shows any widget right now. When it shows none, the pill stays centred on
 * the camera and only grows if the permission dots don't fit as it is.
 */
internal fun emptyPillHasItems(settings: EmptyPillInfoSettings, counts: UnreadCounts, hasBattery: Boolean): Boolean =
    (settings.left + settings.right).any { it.isVisible(counts, hasBattery) }

/** Whether this widget has something to draw: an unread count above 0, a known battery level. */
internal fun SweWidget.isVisible(counts: UnreadCounts, hasBattery: Boolean): Boolean = when (this) {
    is SweWidget.Clock -> true
    is SweWidget.Unread -> count(counts) > 0
    is SweWidget.Battery -> hasBattery
}

/** The count this widget shows, with or without silent notifications as it is set to. */
internal fun SweWidget.Unread.count(counts: UnreadCounts): Int = if (countSilent) counts.all else counts.nonSilent

/** The width this widget takes on the pill, in dp, assuming it is visible. */
private fun SweWidget.widthDp(heightDp: Int, density: Float, counts: UnreadCounts): Float = when (this) {
    is SweWidget.Clock -> clockWidthDp(this, heightDp, density)
    is SweWidget.Unread -> unreadWidthDp(shape, pillWidthPercent, count(counts), heightDp, density)
    is SweWidget.Battery -> batteryWidthDp(percentage, percentSign, heightDp, density)
}

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

/** Measures [text] at [sizeDp] in [typeface], in dp, with [MEASURE_SAFETY] on top. */
private fun measureDp(text: String, sizeDp: Float, density: Float, typeface: Typeface, scaleX: Float = 1f): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizeDp * density
        this.typeface = typeface
        textScaleX = scaleX
    }
    return paint.measureText(text) / density * MEASURE_SAFETY
}

/** The clock's font size in dp: its share of the pill height, scaled by the widget's size setting. */
private fun clockFontDp(clock: SweWidget.Clock, heightDp: Int): Float =
    heightDp * CLOCK_TEXT_FRACTION * clock.sizePercent / PERCENT

/** The width the clock's worst-case string takes at the chosen weight and width. */
private fun clockWidthDp(clock: SweWidget.Clock, heightDp: Int, density: Float): Float {
    val width = clock.width.coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH)
    val span = if (width <= ROBOTO_FLEX_DEFAULT_WIDTH) ROBOTO_FLEX_DEFAULT_WIDTH - ROBOTO_FLEX_MIN_WIDTH
    else ROBOTO_FLEX_MAX_WIDTH - ROBOTO_FLEX_DEFAULT_WIDTH
    val scaleX = 1f + (width - ROBOTO_FLEX_DEFAULT_WIDTH) / span * CLOCK_WIDTH_SWING
    val typeface = Typeface.create(Typeface.DEFAULT, clock.weight, false)
    return measureDp(CLOCK_WORST_CASE, clockFontDp(clock, heightDp), density, typeface, scaleX)
}

/** The width the unread badge takes for [count] drawn as [shape]. */
private fun unreadWidthDp(shape: UnreadShape, pillWidthPercent: Int, count: Int, heightDp: Int, density: Float): Float {
    val badge = badgeSizeFor(heightDp)
    val text = measureDp(unreadText(count), heightDp * UNREAD_TEXT_FRACTION, density, Typeface.DEFAULT_BOLD)
    return when (shape) {
        UnreadShape.NONE -> text
        UnreadShape.STAR -> max(badge.value, text)
        UnreadShape.PILL -> max(badge.value * pillWidthPercent / PERCENT, text + heightDp * UNREAD_PILL_TEXT_PAD_FRACTION * 2f)
    }
}

/** The width the battery takes, including its percentage text when it sits beside the body. */
private fun batteryWidthDp(percentage: BatteryPercentage, percentSign: Boolean, heightDp: Int, density: Float): Float {
    val inside = percentage == BatteryPercentage.INSIDE
    val body = heightDp * if (inside) BATTERY_INSIDE_BODY_WIDTH_FRACTION else BATTERY_BODY_WIDTH_FRACTION
    val base = body + heightDp * BATTERY_TIP_WIDTH_FRACTION
    if (percentage != BatteryPercentage.NEXT_TO) return base
    val worst = if (percentSign) BATTERY_WORST_CASE + PERCENT_SIGN else BATTERY_WORST_CASE
    val text = measureDp(worst, heightDp * BATTERY_TEXT_FRACTION, density, Typeface.DEFAULT_BOLD)
    return base + heightDp * BATTERY_TEXT_GAP_FRACTION + text
}

/** The count as shown on the badge, capped at [UNREAD_CAP_TEXT]. */
private fun unreadText(count: Int): String = if (count > UNREAD_CAP) UNREAD_CAP_TEXT else count.toString()

/**
 * The resting (event-less) pill's widgets, split between the left and right of the camera hole in
 * the order [settings] lists them. Sized with [emptyPillSideWidthsDp], which must be given the same
 * inputs. This overlay pill is the documented exception to the no-hardcoded-colours rule: its
 * defaults are fixed light/dark inks picked by luminance, since the pill sits on the user's own
 * backdrop rather than the app theme.
 *
 * @param enterDelayMs how long the widgets wait before fading in; 0 shows them at once (previews).
 */
@Composable
internal fun EmptyPillInfoContent(
    settings: EmptyPillInfoSettings,
    heightDp: Int,
    counts: UnreadCounts,
    battery: BatteryStatus?,
    dotsLeftDp: Float = 0f,
    dotsRightDp: Float = 0f,
    enterDelayMs: Long = ENTER_DELAY_MS,
) {
    // Held back until the pill has mostly shrunk from whatever it was showing, so the widgets fade in
    // on the settled pill instead of riding its edges as it collapses
    var settled by remember { mutableStateOf(enterDelayMs <= 0L) }
    LaunchedEffect(Unit) {
        delay(enterDelayMs)
        settled = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Left group
        InfoSideRow(
            side = InfoSide.LEFT,
            widgets = settings.left,
            settled = settled,
            heightDp = heightDp,
            counts = counts,
            battery = battery,
            dotsDp = dotsLeftDp,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // Right group
        InfoSideRow(
            side = InfoSide.RIGHT,
            widgets = settings.right,
            settled = settled,
            heightDp = heightDp,
            counts = counts,
            battery = battery,
            dotsDp = dotsRightDp,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** One side's widgets, in list order, with the edge inset on the pill's outer side. */
@Composable
private fun InfoSideRow(
    side: InfoSide,
    widgets: List<SweWidget>,
    settled: Boolean,
    heightDp: Int,
    counts: UnreadCounts,
    battery: BatteryStatus?,
    dotsDp: Float,
    modifier: Modifier = Modifier,
) {
    // The dots sit on the outer edge, so the widgets step inward past them
    val inset = (heightDp * EDGE_INSET_FRACTION + dotsDp).dp
    Row(
        modifier = modifier.padding(
            start = if (side == InfoSide.LEFT) inset else 0.dp,
            end = if (side == InfoSide.RIGHT) inset else 0.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((heightDp * ITEM_GAP_FRACTION).dp),
    ) {
        widgets.forEach { widget ->
            // Keyed by id so reordering moves a widget instead of restarting its animation
            key(widget.id) {
                WidgetSlot(widget, settled, heightDp, counts, battery)
            }
        }
    }
}

/**
 * One widget that fades and scales in and out on its own, and only once [settled]. The last unread
 * count and battery reading are kept so a widget leaving shows its old value rather than a 0 or a
 * blank while it fades.
 */
@Composable
private fun RowScope.WidgetSlot(
    widget: SweWidget,
    settled: Boolean,
    heightDp: Int,
    counts: UnreadCounts,
    battery: BatteryStatus?,
) {
    val visible = widget.isVisible(counts, battery != null)
    val count = (widget as? SweWidget.Unread)?.count(counts) ?: 0
    var lastCount by remember { mutableIntStateOf(count) }
    if (count > 0) lastCount = count
    var lastBattery by remember { mutableStateOf(battery) }
    if (battery != null) lastBattery = battery

    AnimatedVisibility(visible = settled && visible, enter = ItemEnter, exit = ItemExit) {
        when (widget) {
            is SweWidget.Clock -> ClockText(widget, heightDp)
            is SweWidget.Unread -> UnreadBadge(widget, lastCount, heightDp)
            is SweWidget.Battery -> lastBattery?.let { BatteryIndicator(widget, it, heightDp) }
        }
    }
}

/** The current time in the user's chosen font width and weight, with tabular figures. */
@Composable
private fun ClockText(clock: SweWidget.Clock, heightDp: Int) {
    val family = rememberRobotoFlexFamily(clock.width, clock.weight)
    val density = LocalDensity.current
    val widthDp = remember(clock, heightDp, density.density) {
        clockWidthDp(clock, heightDp, density.density)
    }
    Text(
        text = rememberMinuteClock(),
        modifier = Modifier.width(widthDp.dp),
        color = clock.color?.resolve() ?: LocalContentColor.current,
        fontSize = with(density) { clockFontDp(clock, heightDp).dp.toSp() },
        fontFamily = family,
        fontWeight = FontWeight(clock.weight),
        style = TextStyle(fontFeatureSettings = "tnum"),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}

/** The unread count on a star, a pill or bare, crossfading when the shape setting changes. */
@Composable
private fun UnreadBadge(unread: SweWidget.Unread, count: Int, heightDp: Int) {
    val fill by animateColorAsState(unread.shapeColor?.resolve() ?: UNREAD_DEFAULT_FILL, label = "unreadFill")
    val autoText = if (unread.shape == UnreadShape.NONE) LocalContentColor.current else inkOn(fill)
    val textColor by animateColorAsState(unread.textColor?.resolve() ?: autoText, label = "unreadText")
    val badge = badgeSizeFor(heightDp)
    val density = LocalDensity.current

    AnimatedContent(
        targetState = unread.shape,
        transitionSpec = { (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) },
        label = "unreadShape",
    ) { shape ->
        val boxShape: Shape = when (shape) {
            UnreadShape.STAR -> StarBadgeShape
            UnreadShape.PILL -> RoundedCornerShape(50)
            UnreadShape.NONE -> RectangleShape
        }
        val boxModifier = when (shape) {
            UnreadShape.NONE -> Modifier
            UnreadShape.STAR -> Modifier.size(badge).background(fill, boxShape)
            UnreadShape.PILL -> Modifier
                .size(
                    width = unreadWidthDp(shape, unread.pillWidthPercent, count, heightDp, density.density).dp,
                    height = (heightDp * UNREAD_PILL_HEIGHT_FRACTION).dp,
                )
                .background(fill, boxShape)
        }
        Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
            Text(
                text = unreadText(count),
                color = textColor,
                fontSize = with(density) { (heightDp * UNREAD_TEXT_FRACTION).dp.toSp() },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** A battery glyph filled to [battery]'s level, with the percentage inside, beside it, or hidden. */
@Composable
private fun BatteryIndicator(widget: SweWidget.Battery, battery: BatteryStatus, heightDp: Int) {
    val inside = widget.percentage == BatteryPercentage.INSIDE
    val fillColor = widget.color?.resolve()
        ?: if (battery.charging) BATTERY_CHARGING_FILL else LocalContentColor.current
    val autoPercent = if (inside) inkOn(fillColor) else LocalContentColor.current
    val percentColor by animateColorAsState(widget.percentageColor?.resolve() ?: autoPercent, label = "batteryPercent")
    val density = LocalDensity.current

    val bodyWidth = (heightDp * if (inside) BATTERY_INSIDE_BODY_WIDTH_FRACTION else BATTERY_BODY_WIDTH_FRACTION).dp
    val bodyHeight = (heightDp * if (inside) BATTERY_INSIDE_BODY_HEIGHT_FRACTION else BATTERY_BODY_HEIGHT_FRACTION).dp
    val tipWidth = (heightDp * BATTERY_TIP_WIDTH_FRACTION).dp
    val percentText = battery.level.coerceIn(0, 100).toString() + if (widget.percentSign) PERCENT_SIGN else ""
    val beside = widget.percentage == BatteryPercentage.NEXT_TO
    val besideLeft = beside && widget.percentageSide == InfoSide.LEFT
    val gap = (heightDp * BATTERY_TEXT_GAP_FRACTION).dp

    @Composable
    fun PercentBeside(modifier: Modifier) {
        Text(
            text = percentText,
            modifier = modifier,
            color = percentColor,
            fontSize = with(density) { (heightDp * BATTERY_TEXT_FRACTION).dp.toSp() },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Percentage before the body
        if (besideLeft) PercentBeside(Modifier.padding(end = gap))

        // Body and tip
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(bodyWidth + tipWidth, bodyHeight)) {
                val bodyW = bodyWidth.toPx()
                val radius = CornerRadius(size.height * BATTERY_CORNER_RATIO)
                drawRoundRect(fillColor.copy(alpha = BATTERY_TRACK_ALPHA), size = Size(bodyW, size.height), cornerRadius = radius)
                clipRect(right = bodyW * battery.level.coerceIn(0, 100) / 100f) {
                    drawRoundRect(fillColor, size = Size(bodyW, size.height), cornerRadius = radius)
                }
                val tipHeight = size.height * BATTERY_TIP_HEIGHT_RATIO
                drawRoundRect(
                    fillColor.copy(alpha = BATTERY_TRACK_ALPHA),
                    topLeft = Offset(bodyW, (size.height - tipHeight) / 2f),
                    size = Size(tipWidth.toPx(), tipHeight),
                    cornerRadius = CornerRadius(tipWidth.toPx() / 2f),
                )
            }
            if (inside) {
                Text(
                    text = percentText,
                    modifier = Modifier.padding(end = tipWidth),
                    color = percentColor,
                    fontSize = with(density) {
                        val scale = if (widget.percentSign) BATTERY_INSIDE_SIGN_SCALE else 1f
                        (bodyHeight * BATTERY_INSIDE_TEXT_FRACTION * scale).toSp()
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        // Percentage after the body
        if (beside && !besideLeft) PercentBeside(Modifier.padding(start = gap))
    }
}

/** Dark ink on a light [fill], light ink on a dark one. */
private fun inkOn(fill: Color): Color = if (fill.luminance() > 0.5f) INK_DARK else INK_LIGHT

/**
 * A scalloped circle — a radius that swells and dips [STAR_LOBES] times around — for the unread
 * badge. Built from a plain polygon so it needs no shapes library.
 */
private object StarBadgeShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = minOf(size.width, size.height) / 2f
        val path = Path()
        for (step in 0..STAR_STEPS) {
            val angle = step * 2.0 * Math.PI / STAR_STEPS
            val r = radius * (1f - STAR_DEPTH + STAR_DEPTH * cos(STAR_LOBES * angle).toFloat())
            val x = size.width / 2f + r * cos(angle).toFloat()
            val y = size.height / 2f + r * sin(angle).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
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
