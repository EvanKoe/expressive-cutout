package com.ekoehler.expressivecutout.swe.widgets

import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontVariation.weight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.swe.PERCENT
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.swe.color
import com.ekoehler.expressivecutout.swe.measureDp
import com.ekoehler.expressivecutout.swe.putColor
import com.ekoehler.expressivecutout.swe.widgets.ClockWidget.Companion.MAX_WEIGHT
import com.ekoehler.expressivecutout.swe.widgets.ClockWidget.Companion.MIN_WEIGHT
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.components.rememberRobotoFlexFamily
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.with

/**
 * Template for a new resting-pill widget: copy this file, rename `ExampleWidget` and the file,
 * replace [TAG] and the title string, fill in each member, then add the companion to
 * [SweWidgets.all]. It is deliberately not registered, so it never appears in the app.
 *
 * Settings go in the constructor as `val`s with defaults; edits arrive as `onChange(copy(...))`
 * from [Controls]. The step-by-step guide is `docs/ADD_SWE_WIDGET.md`.
 */
data class CustomTextWidget(
    override val id: String = SweWidgets.newId(),
    val body: String = DEFAULT_BODY,
    val textColor: CutoutColor = DEFAULT_TEXT_COLOR,
    val fontWeight: Int = DEFAULT_WEIGHT,
    val fontWidth: Float = DEFAULT_WIDTH
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    /** This widget's settings as JSON; `"type"` and `"id"` are required by [SweWidgets.fromJson]. */
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
        put("body", body)
        putColor("textColor", textColor)
        put("fontWidth", fontWidth.toDouble())
        put("fontWeight", fontWeight)
    }

    /** Whether there is something to draw; a hidden widget takes no width on the pill. */
    override fun isVisible(data: SweData): Boolean = true

    /** The widest this widget can be, in dp; Paint-based (`measureDp`) since it runs outside Compose. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float {
        val clampedWidth = fontWidth.coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH)
        val span = if (clampedWidth <= ROBOTO_FLEX_DEFAULT_WIDTH) ROBOTO_FLEX_DEFAULT_WIDTH - ROBOTO_FLEX_MIN_WIDTH
        else ROBOTO_FLEX_MAX_WIDTH - ROBOTO_FLEX_DEFAULT_WIDTH
        val scaleX = 1f + (clampedWidth - ROBOTO_FLEX_DEFAULT_WIDTH) / span * 0.3f
        val typeface = Typeface.create(Typeface.DEFAULT, fontWeight, false)

        return measureDp(body, heightDp * 0.5f, density, typeface, scaleX)
    }

    /** The widget on the pill. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        val family = rememberRobotoFlexFamily(fontWidth, fontWeight)
        val color by animateColorAsState(textColor.resolve())
        val density = LocalDensity.current
        val widthDp = remember(this, heightDp, density.density) {
            widthDp(heightDp, density.density, data)
        }

        Text(
            text = body,
            modifier = Modifier.width(widthDp.dp),
            color = color,
            fontFamily = family,
            fontSize = with(density) { (heightDp * 0.5f).dp.toSp() },
            fontWeight = FontWeight(fontWeight),
            textAlign = TextAlign.Center,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            softWrap = false
        )
    }

    /** The settings sheet body, below the side picker the editor already shows. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        var mutableBody by remember(body) { mutableStateOf(body) }
        var widthValue by remember(fontWidth) { mutableFloatStateOf(fontWidth) }
        var weightValue by remember(fontWeight) { mutableFloatStateOf(fontWeight.toFloat()) }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Custom text
            Card(
                shape = groupedShape(isFirst = true),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Text input
                    BasicTextField(
                        value = mutableBody,
                        onValueChange = { mutableBody = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    )

                    // Save button
                    FilledTonalButton(
                        enabled = mutableBody != body,
                        onClick = { onChange(copy(body = mutableBody)) }
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            }

            // Font color
            ColorPickerCard(
                label = stringResource(R.string.appearance_text_color),
                allowAppIcon = false,
                selected = textColor,
                onSelect = { onChange(copy(textColor = it ?: DEFAULT_TEXT_COLOR)) },
                shape = groupedShape(),
                defaultColor = Color.White,
                defaultLabel = ""
            )

            // Width, weight and live preview
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
                        text = body,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontFamily = rememberRobotoFlexFamily(widthValue, weightValue.roundToInt()),
                        fontWeight = FontWeight(weightValue.roundToInt()),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Font width
                    AdjustableSlider(
                        label = stringResource(R.string.width),
                        valueText = "${widthValue.roundToInt()}",
                        value = widthValue,
                        valueRange = ROBOTO_FLEX_MIN_WIDTH..ROBOTO_FLEX_MAX_WIDTH,
                        step = 1f,
                        onValueChange = { widthValue = it },
                        onCommit = { onChange(copy(fontWidth = widthValue)) },
                    )

                    // Font weight
                    AdjustableSlider(
                        label = stringResource(R.string.weight),
                        valueText = "${weightValue.roundToInt()}",
                        value = weightValue,
                        valueRange = MIN_WEIGHT.toFloat()..MAX_WEIGHT.toFloat(),
                        step = 100f,
                        onValueChange = { weightValue = it },
                        onCommit = { onChange(copy(fontWeight = weightValue.roundToInt())) },
                    )
                }
            }
        }
    }

    /** One short line under the widget's name in the editor list. */
    @Composable
    override fun summary(): String = body

    /** The example kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        /** Stored in user settings and backups; never change it once shipped. */
        private const val TAG = "customText"

        /** The text a new widget shows, and the fallback when stored JSON has no `"body"`. */
        private const val DEFAULT_BODY: String = "Hello world!"
        private val DEFAULT_TEXT_COLOR: CutoutColor = CutoutColor.Solid(0xFFFFFFFF)
        private const val DEFAULT_WEIGHT: Int = 400
        private const val DEFAULT_WIDTH: Float = 100f

        override val tag: String = TAG
        override val icon: ImageVector = Icons.Rounded.Widgets

        // A real widget adds its own title string in values/, values-fr-rFR/ and values-tr/
        override val titleRes: Int = R.string.widgets_customText_title

        override fun create(): SweWidget = CustomTextWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = CustomTextWidget(
            id = id,
            body = obj.optString("body", DEFAULT_BODY),
            textColor = obj.color("textColor") ?: DEFAULT_TEXT_COLOR,
            fontWeight = obj.optInt("fontWeight", DEFAULT_WEIGHT),
            fontWidth = obj.optDouble("fontWidth", DEFAULT_WIDTH.toDouble()).toFloat()
        )
    }
}
