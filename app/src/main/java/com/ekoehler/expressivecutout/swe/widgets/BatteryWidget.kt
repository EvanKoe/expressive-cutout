package com.ekoehler.expressivecutout.swe.widgets

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.BatteryLook
import com.ekoehler.expressivecutout.data.BatteryPercentage
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.InfoSegmentCard
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.components.rememberRobotoFlexFamily
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
import com.ekoehler.expressivecutout.swe.robotoFlexScaleX
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * The battery level, drawn per [look] as a battery glyph (percentage inside, beside it on
 * [percentageSide] or hidden) or as a progress ring with the percentage centred or hidden.
 */
data class BatteryWidget(
    override val id: String = SweWidgets.newId(),
    val look: BatteryLook = BatteryLook.PILL,
    val color: CutoutColor? = null,
    val percentage: BatteryPercentage = BatteryPercentage.INSIDE,
    val percentageSide: InfoSide = InfoSide.RIGHT,
    val percentSign: Boolean = false,
    val percentageColor: CutoutColor? = null,
    val percentWidth: Float = ROBOTO_FLEX_DEFAULT_WIDTH,
    val percentWeight: Int = DEFAULT_PERCENT_WEIGHT,
    val percentSizePercent: Int = ClockWidget.DEFAULT_SIZE_PERCENT,
    val heightPercent: Int = ClockWidget.DEFAULT_SIZE_PERCENT,
    val trackColor: CutoutColor? = null,
    val circleSizePercent: Int = ClockWidget.DEFAULT_SIZE_PERCENT,
    val circleStrokePercent: Int = DEFAULT_CIRCLE_STROKE_PERCENT,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override fun toJson(): JSONObject = JSONObject().apply {
        put(TYPE, TAG)
        put(ID, id)
        put("look", look.name)
        putColor("color", color)
        put("percentage", percentage.name)
        put("percentageSide", percentageSide.name)
        put("percentSign", percentSign)
        putColor("percentageColor", percentageColor)
        put("percentWidth", percentWidth.toDouble())
        put("percentWeight", percentWeight)
        put("percentSizePercent", percentSizePercent)
        put("heightPercent", heightPercent)
        putColor("trackColor", trackColor)
        put("circleSizePercent", circleSizePercent)
        put("circleStrokePercent", circleStrokePercent)
    }

    override fun isVisible(data: SweData): Boolean = data.battery != null

    /** The width the battery takes, including its percentage text when it sits beside the body. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float {
        if (look == BatteryLook.CIRCLE) return circleDiameterDp(heightDp)
        val inside = percentage == BatteryPercentage.INSIDE
        val body = heightDp * if (inside) INSIDE_BODY_WIDTH_FRACTION else BODY_WIDTH_FRACTION
        val base = body + heightDp * TIP_WIDTH_FRACTION
        if (percentage != BatteryPercentage.NEXT_TO) return base
        val worst = if (percentSign) WORST_CASE + PERCENT_SIGN else WORST_CASE
        val typeface = Typeface.create(Typeface.DEFAULT, percentWeight, false)
        val text = measureDp(worst, heightDp * TEXT_FRACTION * percentScale, density, typeface, robotoFlexScaleX(percentWidth))
        return base + heightDp * TEXT_GAP_FRACTION + text
    }

    /** The ring's diameter in dp: its default share of the pill height, scaled by the size setting. */
    private fun circleDiameterDp(heightDp: Int): Float =
        heightDp * CIRCLE_DIAMETER_FRACTION * circleSizePercent / PERCENT

    /** A battery glyph filled to the reading's level; the last reading is kept so a fade-out keeps it. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        var lastBattery by remember { mutableStateOf(data.battery) }
        if (data.battery != null) lastBattery = data.battery
        val battery = lastBattery ?: return

        val inside = percentage == BatteryPercentage.INSIDE
        val fillColor = color?.resolve()
            ?: if (battery.charging) CHARGING_FILL else LocalContentColor.current
        val trackFill = trackColor?.resolve() ?: fillColor.copy(alpha = TRACK_ALPHA)
        val autoPercent = if (look == BatteryLook.PILL && inside) inkOn(fillColor) else LocalContentColor.current
        val percentColor by animateColorAsState(percentageColor?.resolve() ?: autoPercent, label = "batteryPercent")
        val percentFamily = rememberRobotoFlexFamily(percentWidth, percentWeight)
        val percentText = battery.level.coerceIn(0, 100).toString() + if (percentSign) PERCENT_SIGN else ""

        when (look) {
            BatteryLook.CIRCLE -> CircleContent(
                heightDp = heightDp,
                level = battery.level,
                fillColor = fillColor,
                trackFill = trackFill,
                percentColor = percentColor,
                percentText = percentText,
                percentFamily = percentFamily,
            )
            BatteryLook.PILL -> PillContent(
                heightDp = heightDp,
                level = battery.level,
                fillColor = fillColor,
                trackFill = trackFill,
                percentColor = percentColor,
                percentText = percentText,
                percentFamily = percentFamily,
            )
        }
    }

    /** The classic battery glyph: a rounded body filled to [level] with a tip, and the percentage inside or beside it. */
    @Composable
    private fun PillContent(
        heightDp: Int,
        level: Int,
        fillColor: Color,
        trackFill: Color,
        percentColor: Color,
        percentText: String,
        percentFamily: FontFamily,
    ) {
        val inside = percentage == BatteryPercentage.INSIDE
        val density = LocalDensity.current
        val bodyWidth = (heightDp * if (inside) INSIDE_BODY_WIDTH_FRACTION else BODY_WIDTH_FRACTION).dp
        val bodyHeight = (heightDp * (if (inside) INSIDE_BODY_HEIGHT_FRACTION else BODY_HEIGHT_FRACTION) * heightPercent / PERCENT).dp
        val tipWidth = (heightDp * TIP_WIDTH_FRACTION).dp
        val beside = percentage == BatteryPercentage.NEXT_TO
        val besideLeft = beside && percentageSide == InfoSide.LEFT
        val gap = (heightDp * TEXT_GAP_FRACTION).dp

        @Composable
        fun PercentBeside(modifier: Modifier) {
            Text(
                text = percentText,
                modifier = modifier,
                color = percentColor,
                fontSize = with(density) { (heightDp * TEXT_FRACTION * percentScale).dp.toSp() },
                fontFamily = percentFamily,
                fontWeight = FontWeight(percentWeight),
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
                        trackFill,
                        size = Size(bodyW, size.height),
                        cornerRadius = radius
                    )

                    clipRect(right = bodyW * level.coerceIn(0, 100) / 100f) {
                        drawRoundRect(
                            fillColor,
                            size = Size(bodyW, size.height),
                            cornerRadius = radius
                        )
                    }

                    val tipHeight = size.height * TIP_HEIGHT_RATIO

                    drawRoundRect(
                        trackFill,
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
                            (bodyHeight * INSIDE_TEXT_FRACTION * scale * percentScale).toSp()
                        },
                        fontFamily = percentFamily,
                        fontWeight = FontWeight(percentWeight),
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

    /** A progress ring filled clockwise from the top to [level], with the percentage centred when shown. */
    @Composable
    private fun CircleContent(
        heightDp: Int,
        level: Int,
        fillColor: Color,
        trackFill: Color,
        percentColor: Color,
        percentText: String,
        percentFamily: FontFamily,
    ) {
        val density = LocalDensity.current
        val diameter = circleDiameterDp(heightDp)

        Box(modifier = Modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
            // Track and progress arcs
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * circleStrokePercent / PERCENT
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = trackFill,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )

                // A round cap would still draw a dot at level 0
                if (level > 0) {
                    drawArc(
                        color = fillColor,
                        startAngle = -90f,
                        sweepAngle = 360f * level.coerceIn(0, 100) / 100f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            // Centred percentage
            if (percentage != BatteryPercentage.NONE) {
                Text(
                    text = percentText,
                    color = percentColor,
                    fontSize = with(density) {
                        val scale = if (percentSign) INSIDE_SIGN_SCALE else 1f
                        (diameter * CIRCLE_TEXT_FRACTION * scale * percentScale).dp.toSp()
                    },
                    fontFamily = percentFamily,
                    fontWeight = FontWeight(percentWeight),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }

    /** A look chooser, then the controls of the chosen look. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            // Look chooser
            InfoSegmentCard(
                title = stringResource(R.string.empty_info_battery_look),
                options = listOf(
                    stringResource(R.string.empty_info_battery_look_pill),
                    stringResource(R.string.empty_info_battery_look_circle),
                ),
                selectedIndex = look.ordinal,
                onSelect = { onChange(copy(look = BatteryLook.entries[it])) },
                shape = groupedShape(isFirst = true),
            )

            when (look) {
                BatteryLook.PILL -> PillControls(onChange)
                BatteryLook.CIRCLE -> CircleControls(onChange)
            }
        }
    }

    /** The ring's progress and empty colours, its size and stroke width, a percentage toggle, and the percent sign, text colour and text size when shown. */
    @Composable
    private fun CircleControls(onChange: (SweWidget) -> Unit) {
        val battery = this
        val showsPercentage = battery.percentage != BatteryPercentage.NONE
        var sizeValue by remember(circleSizePercent) { mutableFloatStateOf(circleSizePercent.toFloat()) }
        var strokeValue by remember(circleStrokePercent) { mutableFloatStateOf(circleStrokePercent.toFloat()) }
        var textSizeValue by remember(percentSizePercent) { mutableFloatStateOf(percentSizePercent.toFloat()) }

        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            // Progress colour
            ColorPickerCard(
                label = stringResource(R.string.empty_info_battery_color),
                selected = battery.color,
                onSelect = { onChange(battery.copy(color = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface,
                shape = groupedShape(),
            )

            // Empty progress colour
            ColorPickerCard(
                label = stringResource(R.string.empty_info_battery_track_color),
                selected = battery.trackColor,
                onSelect = { onChange(battery.copy(trackColor = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
                dynamicRoles = TRACK_ROLES,
                shape = groupedShape(),
            )

            // Ring size and stroke width
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdjustableSlider(
                        label = stringResource(R.string.size),
                        valueText = "${sizeValue.roundToInt()}%",
                        value = sizeValue,
                        valueRange = MIN_CIRCLE_SIZE_PERCENT.toFloat()..MAX_CIRCLE_SIZE_PERCENT.toFloat(),
                        step = 5f,
                        onValueChange = { sizeValue = it },
                        onCommit = { onChange(battery.copy(circleSizePercent = sizeValue.roundToInt())) },
                    )
                    AdjustableSlider(
                        label = stringResource(R.string.width),
                        valueText = "${strokeValue.roundToInt()}%",
                        value = strokeValue,
                        valueRange = MIN_CIRCLE_STROKE_PERCENT.toFloat()..MAX_CIRCLE_STROKE_PERCENT.toFloat(),
                        step = 1f,
                        onValueChange = { strokeValue = it },
                        onCommit = { onChange(battery.copy(circleStrokePercent = strokeValue.roundToInt())) },
                    )
                }
            }

            // Percentage toggle
            SettingsToggleCard(
                shape = groupedShape(isLast = !showsPercentage),
                title = stringResource(R.string.empty_info_battery_pct_show),
                description = stringResource(R.string.empty_info_battery_pct_show_desc),
                checked = showsPercentage,
                onCheckedChange = {
                    onChange(battery.copy(percentage = if (it) BatteryPercentage.INSIDE else BatteryPercentage.NONE))
                },
            )

            // Percent sign and text colour, only when a percentage is drawn
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
                        shape = groupedShape(),
                    )

                    // Percentage text size
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = groupedShape(isLast = true),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            AdjustableSlider(
                                label = stringResource(R.string.size),
                                valueText = "${textSizeValue.roundToInt()}%",
                                value = textSizeValue,
                                valueRange = ClockWidget.MIN_SIZE_PERCENT.toFloat()..ClockWidget.MAX_SIZE_PERCENT.toFloat(),
                                step = 5f,
                                onValueChange = { textSizeValue = it },
                                onCommit = { onChange(battery.copy(percentSizePercent = textSizeValue.roundToInt())) },
                            )
                        }
                    }
                }
            }
        }
    }

    /** A battery and empty-container colour, a height, where its percentage goes, and the percentage side, sign, colour and font when shown. */
    @Composable
    private fun PillControls(onChange: (SweWidget) -> Unit) {
        val battery = this
        val showsPercentage = battery.percentage != BatteryPercentage.NONE
        var widthValue by remember(percentWidth) { mutableFloatStateOf(percentWidth) }
        var weightValue by remember(percentWeight) { mutableFloatStateOf(percentWeight.toFloat()) }
        var sizeValue by remember(percentSizePercent) { mutableFloatStateOf(percentSizePercent.toFloat()) }
        var heightValue by remember(heightPercent) { mutableFloatStateOf(heightPercent.toFloat()) }

        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            // Battery colour
            ColorPickerCard(
                label = stringResource(R.string.empty_info_battery_color),
                selected = battery.color,
                onSelect = { onChange(battery.copy(color = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface,
                shape = groupedShape(),
            )

            // Empty container colour
            ColorPickerCard(
                label = stringResource(R.string.empty_info_battery_track_color),
                selected = battery.trackColor,
                onSelect = { onChange(battery.copy(trackColor = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
                dynamicRoles = TRACK_ROLES,
                shape = groupedShape(),
            )

            // Battery height
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    AdjustableSlider(
                        label = stringResource(R.string.height),
                        valueText = "${heightValue.roundToInt()}%",
                        value = heightValue,
                        valueRange = ClockWidget.MIN_SIZE_PERCENT.toFloat()..ClockWidget.MAX_SIZE_PERCENT.toFloat(),
                        step = 5f,
                        onValueChange = { heightValue = it },
                        onCommit = { onChange(battery.copy(heightPercent = heightValue.roundToInt())) },
                    )
                }
            }

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

            // Percent sign, colour and font, only when a percentage is drawn
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
                        shape = groupedShape(),
                    )

                    // Percentage width, weight, size and live preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = groupedShape(isLast = true),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Preview
                            Text(
                                text = PREVIEW_LEVEL + if (percentSign) PERCENT_SIGN else "",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize * (sizeValue / PERCENT),
                                fontFamily = rememberRobotoFlexFamily(widthValue, weightValue.roundToInt()),
                                fontWeight = FontWeight(weightValue.roundToInt()),
                                color = percentageColor?.resolve() ?: MaterialTheme.colorScheme.onSurface,
                            )

                            // Font width
                            AdjustableSlider(
                                label = stringResource(R.string.width),
                                valueText = "${widthValue.roundToInt()}",
                                value = widthValue,
                                valueRange = ROBOTO_FLEX_MIN_WIDTH..ROBOTO_FLEX_MAX_WIDTH,
                                step = 1f,
                                onValueChange = { widthValue = it },
                                onCommit = { onChange(battery.copy(percentWidth = widthValue)) },
                            )

                            // Font weight
                            AdjustableSlider(
                                label = stringResource(R.string.weight),
                                valueText = "${weightValue.roundToInt()}",
                                value = weightValue,
                                valueRange = ClockWidget.MIN_WEIGHT.toFloat()..ClockWidget.MAX_WEIGHT.toFloat(),
                                step = 100f,
                                onValueChange = { weightValue = it },
                                onCommit = { onChange(battery.copy(percentWeight = weightValue.roundToInt())) },
                            )

                            // Font size
                            AdjustableSlider(
                                label = stringResource(R.string.size),
                                valueText = "${sizeValue.roundToInt()}%",
                                value = sizeValue,
                                valueRange = ClockWidget.MIN_SIZE_PERCENT.toFloat()..ClockWidget.MAX_SIZE_PERCENT.toFloat(),
                                step = 5f,
                                onValueChange = { sizeValue = it },
                                onCommit = { onChange(battery.copy(percentSizePercent = sizeValue.roundToInt())) },
                            )
                        }
                    }
                }
            }
        }
    }

    /** The percentage's size setting as a fraction of its default size. */
    private val percentScale: Float get() = percentSizePercent / PERCENT

    /** Names the ring look, or where the glyph's percentage is drawn: beside it, inside it or not at all. */
    @Composable
    override fun summary(): String = stringResource(
        if (look == BatteryLook.CIRCLE) {
            R.string.empty_info_battery_look_circle
        } else {
            when (percentage) {
                BatteryPercentage.NEXT_TO -> R.string.empty_info_battery_pct_next_to
                BatteryPercentage.INSIDE -> R.string.empty_info_battery_pct_inside
                BatteryPercentage.NONE -> R.string.empty_info_battery_pct_none
            }
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
            look = obj.enum<BatteryLook>("look") ?: BatteryLook.PILL,
            color = obj.color("color"),
            percentage = obj.enum<BatteryPercentage>("percentage") ?: BatteryPercentage.INSIDE,
            percentageSide = obj.enum<InfoSide>("percentageSide") ?: InfoSide.RIGHT,
            percentSign = obj.optBoolean("percentSign", false),
            percentageColor = obj.color("percentageColor"),
            percentWidth = obj.optDouble("percentWidth", ROBOTO_FLEX_DEFAULT_WIDTH.toDouble()).toFloat()
                .coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH),
            percentWeight = obj.optInt("percentWeight", DEFAULT_PERCENT_WEIGHT)
                .coerceIn(ClockWidget.MIN_WEIGHT, ClockWidget.MAX_WEIGHT),
            percentSizePercent = obj.optInt("percentSizePercent", ClockWidget.DEFAULT_SIZE_PERCENT)
                .coerceIn(ClockWidget.MIN_SIZE_PERCENT, ClockWidget.MAX_SIZE_PERCENT),
            heightPercent = obj.optInt("heightPercent", ClockWidget.DEFAULT_SIZE_PERCENT)
                .coerceIn(ClockWidget.MIN_SIZE_PERCENT, ClockWidget.MAX_SIZE_PERCENT),
            trackColor = obj.color("trackColor"),
            circleSizePercent = obj.optInt("circleSizePercent", ClockWidget.DEFAULT_SIZE_PERCENT)
                .coerceIn(MIN_CIRCLE_SIZE_PERCENT, MAX_CIRCLE_SIZE_PERCENT),
            circleStrokePercent = obj.optInt("circleStrokePercent", DEFAULT_CIRCLE_STROKE_PERCENT)
                .coerceIn(MIN_CIRCLE_STROKE_PERCENT, MAX_CIRCLE_STROKE_PERCENT),
        )

        /** The accents, the ink colours and the dark-scheme surface tiers, offered for the empty container. */
        private val TRACK_ROLES =
            listOf(DynamicRole.PRIMARY, DynamicRole.SECONDARY, DynamicRole.TERTIARY) +
                DynamicRole.entries.filter { it.isOnColor || it.forcesDarkScheme }

        private const val TAG = "battery"
        private const val TYPE = "type"
        private const val ID = "id"
        private const val PERCENT_SIGN = "%"
        private const val WORST_CASE = "100"
        private const val PREVIEW_LEVEL = "85"
        private const val DEFAULT_PERCENT_WEIGHT = 700
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

        /** The ring look: its default diameter as a fraction of the pill height, and its text size as a fraction of the diameter. */
        private const val CIRCLE_DIAMETER_FRACTION = 0.78f
        private const val CIRCLE_TEXT_FRACTION = 0.44f

        /** The ring size setting, in percent of [CIRCLE_DIAMETER_FRACTION]; 125 % keeps the ring inside the pill height. */
        private const val MIN_CIRCLE_SIZE_PERCENT = 50
        private const val MAX_CIRCLE_SIZE_PERCENT = 125

        /** The ring stroke width setting, in percent of the diameter. */
        private const val MIN_CIRCLE_STROKE_PERCENT = 4
        private const val MAX_CIRCLE_STROKE_PERCENT = 30
        private const val DEFAULT_CIRCLE_STROKE_PERCENT = 10

        /** The battery fill while charging, when the user has not picked a colour. */
        private val CHARGING_FILL = Color(0xFF43A047)
    }
}
