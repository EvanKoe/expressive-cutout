package com.ekoehler.expressivecutout.swe.widgets

import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.rememberMinuteClock
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.components.rememberRobotoFlexFamily
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.swe.putColor
import com.ekoehler.expressivecutout.swe.color
import com.ekoehler.expressivecutout.swe.measureDp
import com.ekoehler.expressivecutout.swe.PERCENT
import org.json.JSONObject
import kotlin.math.roundToInt

/** The time, in Roboto Flex at the chosen width, weight, size and colour. Null colour = default ink. */
data class ClockWidget(
    override val id: String = SweWidgets.newId(),
    val color: CutoutColor? = null,
    val width: Float = ROBOTO_FLEX_DEFAULT_WIDTH,
    val weight: Int = DEFAULT_WEIGHT,
    val sizePercent: Int = DEFAULT_SIZE_PERCENT,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
        putColor("color", color)
        put("width", width.toDouble())
        put("weight", weight)
        put("sizePercent", sizePercent)
    }

    override fun isVisible(data: SweData): Boolean = true

    /** The width the clock's worst-case string takes at the chosen weight and width. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float {
        val clampedWidth = width.coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH)
        val span = if (clampedWidth <= ROBOTO_FLEX_DEFAULT_WIDTH) ROBOTO_FLEX_DEFAULT_WIDTH - ROBOTO_FLEX_MIN_WIDTH
        else ROBOTO_FLEX_MAX_WIDTH - ROBOTO_FLEX_DEFAULT_WIDTH
        val scaleX = 1f + (clampedWidth - ROBOTO_FLEX_DEFAULT_WIDTH) / span * WIDTH_SWING
        val typeface = Typeface.create(Typeface.DEFAULT, weight, false)
        return measureDp(WORST_CASE, fontDp(heightDp), density, typeface, scaleX)
    }

    /** The current time in the user's chosen font width and weight, with tabular figures. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        val family = rememberRobotoFlexFamily(width, weight)
        val density = LocalDensity.current
        val widthDp = remember(this, heightDp, density.density) {
            widthDp(heightDp, density.density, data)
        }
        Text(
            text = rememberMinuteClock(),
            modifier = Modifier.width(widthDp.dp),
            color = color?.resolve() ?: LocalContentColor.current,
            fontSize = with(density) { fontDp(heightDp).dp.toSp() },
            fontFamily = family,
            fontWeight = FontWeight(weight),
            style = TextStyle(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }

    /** The colour, then width, weight and size sliders under a centred live preview. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        var widthValue by remember(width) { mutableFloatStateOf(width) }
        var weightValue by remember(weight) { mutableFloatStateOf(weight.toFloat()) }
        var sizeValue by remember(sizePercent) { mutableFloatStateOf(sizePercent.toFloat()) }

        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            // Text colour
            ColorPickerCard(
                label = stringResource(R.string.empty_info_clock_color),
                selected = color,
                onSelect = { onChange(copy(color = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface,
                shape = groupedShape(isFirst = true),
            )

            // Width, weight, size and live preview
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
                        text = "12:34",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * (sizeValue / PERCENT),
                        fontFamily = rememberRobotoFlexFamily(widthValue, weightValue.roundToInt()),
                        fontWeight = FontWeight(weightValue.roundToInt()),
                        color = color?.resolve() ?: MaterialTheme.colorScheme.onSurface,
                    )

                    // Font width
                    AdjustableSlider(
                        label = stringResource(R.string.empty_info_clock_width),
                        valueText = "${widthValue.roundToInt()}",
                        value = widthValue,
                        valueRange = ROBOTO_FLEX_MIN_WIDTH..ROBOTO_FLEX_MAX_WIDTH,
                        step = 1f,
                        onValueChange = { widthValue = it },
                        onCommit = { onChange(copy(width = widthValue)) },
                    )

                    // Font weight
                    AdjustableSlider(
                        label = stringResource(R.string.empty_info_clock_weight),
                        valueText = "${weightValue.roundToInt()}",
                        value = weightValue,
                        valueRange = MIN_WEIGHT.toFloat()..MAX_WEIGHT.toFloat(),
                        step = 100f,
                        onValueChange = { weightValue = it },
                        onCommit = { onChange(copy(weight = weightValue.roundToInt())) },
                    )

                    // Font size
                    AdjustableSlider(
                        label = stringResource(R.string.empty_info_clock_size),
                        valueText = "${sizeValue.roundToInt()}%",
                        value = sizeValue,
                        valueRange = MIN_SIZE_PERCENT.toFloat()..MAX_SIZE_PERCENT.toFloat(),
                        step = 5f,
                        onValueChange = { sizeValue = it },
                        onCommit = { onChange(copy(sizePercent = sizeValue.roundToInt())) },
                    )
                }
            }
        }
    }

    @Composable
    override fun summary(): String = "$weight · $sizePercent%"

    /** The clock's font size in dp: its share of the pill height, scaled by the size setting. */
    private fun fontDp(heightDp: Int): Float = heightDp * TEXT_FRACTION * sizePercent / PERCENT

    /** The clock kind: its registry entry, defaults, clamping ranges and JSON reader. */
    companion object : SweWidgetType {
        const val DEFAULT_WEIGHT = 700
        const val DEFAULT_SIZE_PERCENT = 100
        const val MIN_WEIGHT = 100
        const val MAX_WEIGHT = 1000
        const val MIN_SIZE_PERCENT = 50
        const val MAX_SIZE_PERCENT = 150

        /** The widest clock the pill will ever show; the side is sized for it so it never twitches. */
        private const val WORST_CASE = "88:88"
        private const val TEXT_FRACTION = 0.5f

        /** How far the clock's rendered width swings from normal at the narrowest / widest `wdth` setting. */
        private const val WIDTH_SWING = 0.3f

        private const val TAG = "clock"

        override val tag: String = TAG
        override val icon: ImageVector = Icons.Rounded.Schedule
        override val titleRes: Int = R.string.empty_info_clock_title

        override fun create(): SweWidget = ClockWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = clamped(
            id = id,
            color = obj.color("color"),
            width = obj.optDouble("width", ROBOTO_FLEX_DEFAULT_WIDTH.toDouble()).toFloat(),
            weight = obj.optInt("weight", DEFAULT_WEIGHT),
            sizePercent = obj.optInt("sizePercent", DEFAULT_SIZE_PERCENT),
        )

        /** A clock with width, weight and size forced into their valid ranges. */
        fun clamped(id: String, color: CutoutColor?, width: Float, weight: Int, sizePercent: Int): ClockWidget =
            ClockWidget(
                id = id,
                color = color,
                width = width.coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH),
                weight = weight.coerceIn(MIN_WEIGHT, MAX_WEIGHT),
                sizePercent = sizePercent.coerceIn(MIN_SIZE_PERCENT, MAX_SIZE_PERCENT),
            )
    }
}
