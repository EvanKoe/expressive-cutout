package com.ekoehler.expressivecutout.swe.widgets

import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.UnreadShape
import com.ekoehler.expressivecutout.overlay.badgeSizeFor
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.service.UnreadCounts
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.InfoSegmentCard
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import com.ekoehler.expressivecutout.ui.screen.SettingsToggleCard
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.swe.putColor
import com.ekoehler.expressivecutout.swe.color
import com.ekoehler.expressivecutout.swe.enum
import com.ekoehler.expressivecutout.swe.measureDp
import com.ekoehler.expressivecutout.swe.inkOn
import com.ekoehler.expressivecutout.swe.PERCENT
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** The unread notification count on a star, a pill or bare. Hidden while the count is 0. */
data class UnreadWidget(
    override val id: String = SweWidgets.newId(),
    val shape: UnreadShape = UnreadShape.STAR,
    val shapeColor: CutoutColor? = null,
    val pillWidthPercent: Int = MIN_PILL_WIDTH_PERCENT,
    val textColor: CutoutColor? = null,
    val countSilent: Boolean = false,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
        put("shape", shape.name)
        putColor("shapeColor", shapeColor)
        put("pillWidthPercent", pillWidthPercent)
        putColor("textColor", textColor)
        put("countSilent", countSilent)
    }

    /** The count this widget shows: every unread notification when [countSilent], else only the audible ones. */
    private fun count(counts: UnreadCounts): Int = if (countSilent) counts.all else counts.nonSilent

    override fun isVisible(data: SweData): Boolean = count(data.counts) > 0

    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float =
        widthDp(count(data.counts), heightDp, density)

    /** The width the badge takes for [count] drawn as [shape]. */
    private fun widthDp(count: Int, heightDp: Int, density: Float): Float {
        val badge = badgeSizeFor(heightDp)
        val text = measureDp(unreadText(count), heightDp * UNREAD_TEXT_FRACTION, density, Typeface.DEFAULT_BOLD)
        return when (shape) {
            UnreadShape.NONE -> text
            UnreadShape.STAR -> max(badge.value, text)
            UnreadShape.PILL -> max(
                badge.value * pillWidthPercent / PERCENT,
                text + heightDp * UNREAD_PILL_TEXT_PAD_FRACTION * 2f,
            )
        }
    }

    /** The count on a star, a pill or bare, crossfading when the shape setting changes. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) = DrawCount(count(data.counts), heightDp)

    /**
     * Draws [count] on the widget's shape, keeping the last non-zero value while it fades out
     * unless [showZero], in which case 0 is drawn as is. The overlay's hotspot badge calls it
     * directly with [showZero] set.
     */
    @Composable
    fun DrawCount(count: Int, heightDp: Int, showZero: Boolean = false) {
        var lastCount by remember { mutableIntStateOf(count) }

        if (count > 0 || showZero) {
            lastCount = count
        }

        val fill by animateColorAsState(shapeColor?.resolve() ?: UNREAD_DEFAULT_FILL, label = "unreadFill")
        val autoText = if (shape == UnreadShape.NONE) LocalContentColor.current else inkOn(fill)
        val numberColor by animateColorAsState(textColor?.resolve() ?: autoText, label = "unreadText")
        val badge = badgeSizeFor(heightDp)
        val density = LocalDensity.current

        AnimatedContent(
            targetState = shape,
            transitionSpec = { (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) },
            label = "unreadShape",
        ) { targetShape ->
            val boxShape: Shape = when (targetShape) {
                UnreadShape.STAR -> StarBadgeShape
                UnreadShape.PILL -> RoundedCornerShape(50)
                UnreadShape.NONE -> RectangleShape
            }

            val boxModifier = when (targetShape) {
                UnreadShape.NONE -> Modifier
                UnreadShape.STAR -> Modifier.size(badge).background(fill, boxShape)
                UnreadShape.PILL -> Modifier
                    .size(
                        width = copy(shape = targetShape).widthDp(lastCount, heightDp, density.density).dp,
                        height = (heightDp * UNREAD_PILL_HEIGHT_FRACTION).dp,
                    )
                    .background(fill, boxShape)
            }

            Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = unreadText(lastCount),
                    color = numberColor,
                    fontSize = with(density) { (heightDp * UNREAD_TEXT_FRACTION).dp.toSp() },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }

    /** The badge style controls followed by the silent-counting toggle. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            BadgeStyleControls(widget = this@UnreadWidget, onChange = onChange)

            // Silent notifications
            SettingsToggleCard(
                shape = groupedShape(isLast = true),
                title = stringResource(R.string.empty_info_unread_silent),
                description = stringResource(R.string.empty_info_unread_silent_desc),
                checked = countSilent,
                onCheckedChange = { onChange(copy(countSilent = it)) },
            )
        }
    }

    @Composable
    override fun summary(): String = listOfNotNull(
        stringResource(
            when (shape) {
                UnreadShape.STAR -> R.string.empty_info_unread_shape_star
                UnreadShape.PILL -> R.string.empty_info_unread_shape_pill
                UnreadShape.NONE -> R.string.empty_info_unread_shape_none
            },
        ),
        stringResource(R.string.empty_info_unread_silent).takeIf { countSilent },
    ).joinToString(" · ")

    /** The registry entry for unread widgets, plus their setting ranges. */
    companion object : SweWidgetType {
        /** Smallest pill width, as a percentage of the badge size. */
        const val MIN_PILL_WIDTH_PERCENT = 100

        /** Widest pill width, as a percentage of the badge size. */
        const val MAX_PILL_WIDTH_PERCENT = 300

        private const val TAG = "unread"

        /** The badge's shape colour when the user hasn't picked one. */
        private val UNREAD_DEFAULT_FILL = Color(0xFFE53935)

        /** The widest unread count shown; anything above is capped to this. */
        private const val UNREAD_CAP_TEXT = "99+"
        private const val UNREAD_CAP = 99
        private const val UNREAD_TEXT_FRACTION = 0.4f
        private const val UNREAD_PILL_TEXT_PAD_FRACTION = 0.16f

        /** The pill height share; must match the battery's inside-body height so the two line up. */
        private const val UNREAD_PILL_HEIGHT_FRACTION = 0.58f

        override val tag: String = TAG
        override val icon: ImageVector get() = Icons.Rounded.Notifications
        override val titleRes: Int = R.string.empty_info_unread_title

        override fun create(): SweWidget = UnreadWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = clamped(
            id = id,
            shape = obj.enum<UnreadShape>("shape") ?: UnreadShape.STAR,
            shapeColor = obj.color("shapeColor"),
            pillWidthPercent = obj.optInt("pillWidthPercent", MIN_PILL_WIDTH_PERCENT),
            textColor = obj.color("textColor"),
            countSilent = obj.optBoolean("countSilent", false),
        )

        /** An [UnreadWidget] with [pillWidthPercent] clamped to its valid range. */
        fun clamped(
            id: String,
            shape: UnreadShape,
            shapeColor: CutoutColor?,
            pillWidthPercent: Int,
            textColor: CutoutColor?,
            countSilent: Boolean,
        ) = UnreadWidget(
            id = id,
            shape = shape,
            shapeColor = shapeColor,
            pillWidthPercent = pillWidthPercent.coerceIn(MIN_PILL_WIDTH_PERCENT, MAX_PILL_WIDTH_PERCENT),
            textColor = textColor,
            countSilent = countSilent,
        )

        /** The count as shown on the badge, capped at [UNREAD_CAP_TEXT]. */
        private fun unreadText(count: Int): String = if (count > UNREAD_CAP) UNREAD_CAP_TEXT else count.toString()
    }
}

/**
 * The shape, its colour and pill width when they apply, and the number colour of [widget]. Shared
 * by the Unread widget editor and the hotspot device-count settings; the last card is left open so
 * the caller can append its own.
 */
@Composable
internal fun BadgeStyleControls(widget: UnreadWidget, onChange: (UnreadWidget) -> Unit) {
    var pillWidth by remember(widget.pillWidthPercent) { mutableFloatStateOf(widget.pillWidthPercent.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Badge shape
        InfoSegmentCard(
            title = stringResource(R.string.empty_info_unread_shape),
            options = listOf(
                stringResource(R.string.empty_info_unread_shape_star),
                stringResource(R.string.empty_info_unread_shape_pill),
                stringResource(R.string.empty_info_unread_shape_none),
            ),
            selectedIndex = widget.shape.ordinal,
            onSelect = { onChange(widget.copy(shape = UnreadShape.entries[it])) },
            shape = groupedShape(isFirst = true),
        )

        // Badge colour, only when there is a badge
        AnimatedVisibility(visible = widget.shape != UnreadShape.NONE) {
            ColorPickerCard(
                label = stringResource(R.string.empty_info_unread_shape_color),
                selected = widget.shapeColor,
                onSelect = { onChange(widget.copy(shapeColor = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.primary,
                shape = groupedShape(),
            )
        }

        // Pill width, only for the pill shape
        AnimatedVisibility(visible = widget.shape == UnreadShape.PILL) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    AdjustableSlider(
                        label = stringResource(R.string.empty_info_unread_pill_width),
                        valueText = "${pillWidth.roundToInt()}%",
                        value = pillWidth,
                        valueRange = UnreadWidget.MIN_PILL_WIDTH_PERCENT.toFloat()..UnreadWidget.MAX_PILL_WIDTH_PERCENT.toFloat(),
                        step = 10f,
                        onValueChange = { pillWidth = it },
                        onCommit = { onChange(widget.copy(pillWidthPercent = pillWidth.roundToInt())) },
                    )
                }
            }
        }

        // Number colour
        ColorPickerCard(
            label = stringResource(R.string.empty_info_unread_text_color),
            selected = widget.textColor,
            onSelect = { onChange(widget.copy(textColor = it)) },
            defaultLabel = stringResource(R.string.label_default),
            defaultColor = MaterialTheme.colorScheme.onSurface,
            shape = groupedShape(),
        )
    }
}

/**
 * A scalloped circle — a radius that swells and dips [STAR_LOBES] times around — for the unread
 * badge. Built from a plain polygon so it needs no shapes library.
 */
private object StarBadgeShape : Shape {
    private const val STAR_LOBES = 10
    private const val STAR_DEPTH = 0.07f
    private const val STAR_STEPS = 120

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
