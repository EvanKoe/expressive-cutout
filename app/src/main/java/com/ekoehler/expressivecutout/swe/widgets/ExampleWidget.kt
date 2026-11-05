package com.ekoehler.expressivecutout.swe.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import org.json.JSONObject

/**
 * Template for a new resting-pill widget: copy this file, rename `ExampleWidget` and the file,
 * replace [TAG] and the title string, fill in each member, then add the companion to
 * [SweWidgets.all]. It is deliberately not registered, so it never appears in the app.
 *
 * Settings go in the constructor as `val`s with defaults; edits arrive as `onChange(copy(...))`
 * from [Controls]. The step-by-step guide is `docs/ADD_SWE_WIDGET.md`.
 */
data class ExampleWidget(
    override val id: String = SweWidgets.newId(),
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    /** This widget's settings as JSON; `"type"` and `"id"` are required by [SweWidgets.fromJson]. */
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
    }

    /** Whether there is something to draw; a hidden widget takes no width on the pill. */
    override fun isVisible(data: SweData): Boolean = true

    /** The widest this widget can be, in dp; Paint-based (`measureDp`) since it runs outside Compose. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float = 0f

    /** The widget on the pill. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
    }

    /** The settings sheet body, below the side picker the editor already shows. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
    }

    /** One short line under the widget's name in the editor list. */
    @Composable
    override fun summary(): String = ""

    /** The example kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        /** Stored in user settings and backups; never change it once shipped. */
        private const val TAG = "example"

        override val tag: String = TAG
        override val icon: ImageVector = Icons.Rounded.Widgets

        // A real widget adds its own title string in values/, values-fr-rFR/ and values-tr/
        override val titleRes: Int = R.string.label_default

        override fun create(): SweWidget = ExampleWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = ExampleWidget(id = id)
    }
}
