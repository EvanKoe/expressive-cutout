package com.ekoehler.expressivecutout.swe

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONObject

/**
 * What the registry and the add sheet need to know about a widget kind before an instance exists.
 * Implemented by the companion object of each [SweWidget] and listed once in [SweWidgets.all].
 */
interface SweWidgetType {
    /** The JSON type tag; never change it once shipped, stored settings and backups carry it. */
    val tag: String

    /** The icon shown in the add sheet and the list. */
    val icon: ImageVector

    /** The display name of this kind. */
    @get:StringRes
    val titleRes: Int

    /** A new widget with default settings and a fresh id. */
    fun create(): SweWidget

    /** Rebuilds a widget from its [SweWidget.toJson] object, using defaults and clamping ranges. */
    fun fromJson(id: String, obj: JSONObject): SweWidget
}
