package com.ekoehler.expressivecutout.swe.widgets

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.BatteryPercentage
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.InfoSegmentCard
import com.ekoehler.expressivecutout.ui.components.groupedShape
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
import org.json.JSONObject

/** A battery glyph with its percentage inside, beside it (on [percentageSide]) or hidden. */
data class BatteryWidget(
    override val id: String = SweWidgets.newId(),
    val color: CutoutColor? = null,
    val percentage: BatteryPercentage = BatteryPercentage.INSIDE,
    val percentageSide: InfoSide = InfoSide.RIGHT,
    val percentSign: Boolean = false,
    val percentageColor: CutoutColor? = null,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override fun toJson(): JSONObject = JSONObject().apply {
        put(TYPE, TAG)
        put(ID, id)
        putColor("color", color)
        put("percentage", percentage.name)
        put("percentageSide", percentageSide.name)
        put("percentSign", percentSign)
        putColor("percentageColor", percentageColor)
    }

    override fun isVisible(data: SweData): Boolean = data.battery != null

    /** The width the battery takes, including its percentage text when it sits beside the body. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float {
        val inside = percentage == BatteryPercentage.INSIDE
        val body = heightDp * if (inside) INSIDE_BODY_WIDTH_FRACTION else BODY_WIDTH_FRACTION
        val base = body + heightDp * TIP_WIDTH_FRACTION
        if (percentage != BatteryPercentage.NEXT_TO) return base
        val worst = if (percentSign) WORST_CASE + PERCENT_SIGN else WORST_CASE
        val text = measureDp(worst, heightDp * TEXT_FRACTION, density, Typeface.DEFAULT_BOLD)
        return base + heightDp * TEXT_GAP_FRACTION + text
    }

    /** A battery glyph filled to the reading's level; the last reading is kept so a fade-out keeps it. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        var lastBattery by remember { mutableStateOf(data.battery) }
        if (data.battery != null) lastBattery = data.battery
        val battery = lastBattery ?: return

        val inside = percentage == BatteryPercentage.INSIDE
        val fillColor = color?.resolve()
            ?: if (battery.charging) CHARGING_FILL else LocalContentColor.current
        val autoPercent = if (inside) inkOn(fillColor) else LocalContentColor.current
        val percentColor by animateColorAsState(percentageColor?.resolve() ?: autoPercent, label = "batteryPercent")
        val density = LocalDensity.current

        val bodyWidth = (heightDp * if (inside) INSIDE_BODY_WIDTH_FRACTION else BODY_WIDTH_FRACTION).dp
        val bodyHeight = (heightDp * if (inside) INSIDE_BODY_HEIGHT_FRACTION else BODY_HEIGHT_FRACTION).dp
        val tipWidth = (heightDp * TIP_WIDTH_FRACTION).dp
        val percentText = battery.level.coerceIn(0, 100).toString() + if (percentSign) PERCENT_SIGN else ""
        val beside = percentage == BatteryPercentage.NEXT_TO
        val besideLeft = beside && percentageSide == InfoSide.LEFT
        val gap = (heightDp * TEXT_GAP_FRACTION).dp

        @Composable
        fun PercentBeside(modifier: Modifier) {
            Text(
                text = percentText,
                modifier = modifier,
                color = percentColor,
                fontSize = with(density) { (heightDp * TEXT_FRACTION).dp.toSp() },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Percentage before the body
            if (besideLeft) {
                PercentBeside(Modifier.padding(end = gap))
            }

            // Body and tip
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(bodyWidth + tipWidth, bodyHeight)) {
                    val bodyW = bodyWidth.toPx()
                    val radius = CornerRadius(size.height * CORNER_RATIO)

                    drawRoundRect(
                        fillColor.copy(alpha = TRACK_ALPHA),
                        size = Size(bodyW, size.height),
                        cornerRadius = radius
                    )

                    clipRect(right = bodyW * battery.level.coerceIn(0, 100) / 100f) {
                        drawRoundRect(
                            fillColor,
                            size = Size(bodyW, size.height),
                            cornerRadius = radius
                        )
                    }

                    val tipHeight = size.height * TIP_HEIGHT_RATIO

                    drawRoundRect(
                        fillColor.copy(alpha = TRACK_ALPHA),
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
                            val scale = if (percentSign) INSIDE_SIGN_SCALE else 1f
                            (bodyHeight * INSIDE_TEXT_FRACTION * scale).toSp()
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            // Percentage after the body
            if (beside && !besideLeft) {
                PercentBeside(Modifier.padding(start = gap))
            }
        }
    }

    /** A battery colour, where its percentage goes, and the percentage side, sign and colour when shown. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        val battery = this
        val showsPercentage = battery.percentage != BatteryPercentage.NONE

        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            // Battery colour
            ColorPickerCard(
                label = stringResource(R.string.empty_info_battery_color),
                selected = battery.color,
                onSelect = { onChange(battery.copy(color = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface,
                shape = groupedShape(isFirst = true),
            )

            // Percentage placement
            InfoSegmentCard(
                title = stringResource(R.string.empty_info_battery_pct),
                options = listOf(
                    stringResource(R.string.empty_info_battery_pct_next_to),
                    stringResource(R.string.empty_info_battery_pct_inside),
                    stringResource(R.string.empty_info_battery_pct_none),
                ),
                selectedIndex = battery.percentage.ordinal,
                onSelect = { onChange(battery.copy(percentage = BatteryPercentage.entries[it])) },
                shape = groupedShape(isLast = !showsPercentage),
            )

            // Percentage side, only when it sits beside the icon
            AnimatedVisibility(visible = battery.percentage == BatteryPercentage.NEXT_TO) {
                InfoSegmentCard(
                    title = stringResource(R.string.empty_info_battery_pct_side),
                    options = listOf(
                        stringResource(R.string.empty_info_side_left),
                        stringResource(R.string.empty_info_side_right),
                    ),
                    selectedIndex = battery.percentageSide.ordinal,
                    onSelect = { onChange(battery.copy(percentageSide = InfoSide.entries[it])) },
                    shape = groupedShape(),
                )
            }

            // Percent sign and colour, only when a percentage is drawn
            AnimatedVisibility(visible = showsPercentage) {
                Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    SettingsToggleCard(
                        shape = groupedShape(),
                        title = stringResource(R.string.empty_info_battery_pct_sign),
                        description = stringResource(R.string.empty_info_battery_pct_sign_desc),
                        checked = battery.percentSign,
                        onCheckedChange = { onChange(battery.copy(percentSign = it)) },
                    )
                    ColorPickerCard(
                        label = stringResource(R.string.empty_info_battery_pct_color),
                        selected = battery.percentageColor,
                        onSelect = { onChange(battery.copy(percentageColor = it)) },
                        defaultLabel = stringResource(R.string.label_default),
                        defaultColor = MaterialTheme.colorScheme.onSurface,
                        shape = groupedShape(isLast = true),
                    )
                }
            }
        }
    }

    /** Names where the percentage is drawn: beside the glyph, inside it or not at all. */
    @Composable
    override fun summary(): String = stringResource(
        when (percentage) {
            BatteryPercentage.NEXT_TO -> R.string.empty_info_battery_pct_next_to
            BatteryPercentage.INSIDE -> R.string.empty_info_battery_pct_inside
            BatteryPercentage.NONE -> R.string.empty_info_battery_pct_none
        },
    )

    /** The battery kind: its tag, icon, title and JSON reader, plus the glyph's layout constants. */
    companion object : SweWidgetType {
        override val tag: String = TAG
        override val icon: ImageVector = Icons.Rounded.BatteryFull
        override val titleRes: Int = R.string.empty_info_battery_title

        override fun create(): SweWidget = BatteryWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = BatteryWidget(
            id = id,
            color = obj.color("color"),
            percentage = obj.enum<BatteryPercentage>("percentage") ?: BatteryPercentage.INSIDE,
            percentageSide = obj.enum<InfoSide>("percentageSide") ?: InfoSide.RIGHT,
            percentSign = obj.optBoolean("percentSign", false),
            percentageColor = obj.color("percentageColor"),
        )

        private const val TAG = "battery"
        private const val TYPE = "type"
        private const val ID = "id"
        private const val PERCENT_SIGN = "%"
        private const val WORST_CASE = "100"
        private const val INSIDE_SIGN_SCALE = 0.78f
        private const val TEXT_FRACTION = 0.4f
        private const val INSIDE_TEXT_FRACTION = 0.68f
        private const val BODY_WIDTH_FRACTION = 0.64f
        private const val BODY_HEIGHT_FRACTION = 0.34f
        private const val INSIDE_BODY_WIDTH_FRACTION = 1.0f
        private const val INSIDE_BODY_HEIGHT_FRACTION = 0.58f
        private const val TIP_WIDTH_FRACTION = 0.05f
        private const val TIP_HEIGHT_RATIO = 0.4f
        private const val CORNER_RATIO = 0.3f
        private const val TRACK_ALPHA = 0.3f
        private const val TEXT_GAP_FRACTION = 0.1f

        /** The battery fill while charging, when the user has not picked a colour. */
        private val CHARGING_FILL = Color(0xFF43A047)
    }
}
