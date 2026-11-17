package com.ekoehler.expressivecutout.swe

import androidx.compose.runtime.Composable
import org.json.JSONObject

/**
 * One widget on the resting (SWE) pill. Implementations are immutable data classes; each instance
 * carries its own [id] so the same [type] can be added several times with different settings.
 * The side and order a widget sits in come from the list holding it.
 *
 * Contract for implementers:
 * - [toJson] writes `type.tag` under `"type"` and [id] under `"id"`, plus its own settings;
 *   [SweWidgetType.fromJson] must read it back and clamp every value to its valid range.
 * - [widthDp] runs outside Compose, before composition, to size the accessibility overlay window.
 *   It must therefore be a worst-case estimate measured with a `Paint` (see `measureDp`), never
 *   with Compose APIs. Too small a value clips the widget; it must not depend on values that
 *   change often (such as the minute or the battery level) or the pill width would twitch.
 * - [isVisible] is also called outside Compose; a widget that returns false takes no width.
 * - [Content] draws the widget and keeps its own "last value" so a fade-out shows the old reading.
 */
interface SweWidget {
    /** Unique among all widgets; stable across edits. */
    val id: String

    /** The kind of this widget, used for its tag, icon and title. */
    val type: SweWidgetType

    /** This widget as a tagged JSON object, read back by [SweWidgetType.fromJson]. */
    fun toJson(): JSONObject

    /** Whether the widget has something to show for [data]; hidden widgets take no width. */
    fun isVisible(data: SweData): Boolean

    /** Worst-case width in dp for a pill [heightDp] tall at [density]; Paint-based, no Compose. */
    fun widthDp(heightDp: Int, density: Float, data: SweData): Float

    /** Draws the widget inside a pill [heightDp] tall. */
    @Composable
    fun Content(data: SweData, heightDp: Int)

    /** The settings sheet body; [onChange] receives the edited copy of this widget. */
    @Composable
    fun Controls(onChange: (SweWidget) -> Unit)

    /** A short one-line reminder of the current settings, shown in the editor list. */
    @Composable
    fun summary(): String
}
