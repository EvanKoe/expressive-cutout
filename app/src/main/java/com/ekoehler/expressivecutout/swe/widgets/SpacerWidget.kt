package com.ekoehler.expressivecutout.swe.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import com.ekoehler.expressivecutout.ui.screen.SettingsToggleCard
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * An empty gap between widgets: either a fixed width ([spaceDp]) or, when [flex] is on, all the
 * room left on its side. It draws nothing.
 */
data class SpacerWidget(
    override val id: String = SweWidgets.newId(),
    val spaceDp: Int = DEFAULT_WIDTH_DP,
    val flex: Boolean = false,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override val isFlexible: Boolean get() = flex

    /** This widget's settings as JSON; `"type"` and `"id"` are required by [SweWidgets.fromJson]. */
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
        put("spaceDp", spaceDp)
        put("flex", flex)
    }

    /** A spacer is always there; it has nothing to wait for. */
    override fun isVisible(data: SweData): Boolean = true

    /** The fixed width, or 0 when flexible: its width then comes from the room on its side. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float =
        if (flex) 0f else spaceDp.toFloat()

    /** The empty gap on the pill. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        Spacer(if (flex) Modifier.fillMaxWidth() else Modifier.width(spaceDp.dp))
    }

    /** A flex switch; the width slider shows under it only while the spacer is fixed. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        var widthValue by remember(spaceDp) { mutableFloatStateOf(spaceDp.toFloat()) }

        SettingsToggleCard(
            shape = groupedShape(isFirst = true, isLast = true),
            title = stringResource(R.string.widgets_spacer_flex_title),
            description = stringResource(R.string.widgets_spacer_flex_desc),
            checked = flex,
            onCheckedChange = { onChange(copy(flex = it)) },
            content = if (flex) null else {
                {
                    // Width
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        AdjustableSlider(
                            label = stringResource(R.string.width),
                            valueText = "${widthValue.roundToInt()} dp",
                            value = widthValue,
                            valueRange = MIN_WIDTH_DP.toFloat()..MAX_WIDTH_DP.toFloat(),
                            step = WIDTH_STEP_DP,
                            onValueChange = { widthValue = it },
                            onCommit = { onChange(copy(spaceDp = widthValue.roundToInt())) },
                        )
                    }
                }
            },
        )
    }

    /** "Flexible", or the fixed width in dp. */
    @Composable
    override fun summary(): String =
        if (flex) stringResource(R.string.widgets_spacer_flex_title) else "$spaceDp dp"

    /** The spacer kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        /** Stored in user settings and backups; never change it once shipped. */
        private const val TAG = "spacer"

        /** The fixed width range in dp, its slider step, and the width a new spacer starts with. */
        private const val MIN_WIDTH_DP = 2
        private const val MAX_WIDTH_DP = 100
        private const val DEFAULT_WIDTH_DP = 16
        private const val WIDTH_STEP_DP = 2f

        override val tag: String = TAG
        override val icon: ImageVector = Icons.Rounded.SpaceBar
        override val titleRes: Int = R.string.widgets_spacer_title

        override fun create(): SweWidget = SpacerWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = SpacerWidget(
            id = id,
            spaceDp = obj.optInt("spaceDp", DEFAULT_WIDTH_DP).coerceIn(MIN_WIDTH_DP, MAX_WIDTH_DP),
            flex = obj.optBoolean("flex", false),
        )
    }
}
