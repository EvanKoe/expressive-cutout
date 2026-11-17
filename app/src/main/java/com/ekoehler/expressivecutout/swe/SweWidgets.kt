package com.ekoehler.expressivecutout.swe

import com.ekoehler.expressivecutout.data.BatteryPercentage
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.data.UnreadShape
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.swe.widgets.BatteryWidget
import com.ekoehler.expressivecutout.swe.widgets.ClockWidget
import com.ekoehler.expressivecutout.swe.widgets.CustomTextWidget
import com.ekoehler.expressivecutout.swe.widgets.UnreadWidget
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * The registry of resting-pill widget kinds and the JSON codec for their lists.
 * A contributor adds a widget by appending its [SweWidgetType] to [all].
 */
object SweWidgets {
    /** Every widget kind, in add-sheet order. Tags must be unique. */
    val all: List<SweWidgetType> = listOf(ClockWidget, UnreadWidget, BatteryWidget,
        CustomTextWidget)

    private const val TYPE = "type"
    private const val ID = "id"

    /**
     * Fixed ids for the widgets converted from the pre-widget settings, so they stay stable
     * across reads until the converted lists are first saved.
     */
    private const val LEGACY_CLOCK_ID = "legacy-clock"
    private const val LEGACY_UNREAD_ID = "legacy-unread"
    private const val LEGACY_BATTERY_ID = "legacy-battery"

    init {
        check(all.map { it.tag }.toSet().size == all.size) { "Two SweWidgetTypes share a tag" }
    }

    /** A fresh unique id for a newly added widget. */
    fun newId(): String = UUID.randomUUID().toString()

    /** Reads one [SweWidget.toJson] object; null for an unknown type or a missing id, so one bad entry is dropped alone. */
    fun fromJson(obj: JSONObject): SweWidget? {
        val id = obj.optString(ID).takeIf { it.isNotEmpty() } ?: return null
        val tag = obj.optString(TYPE)
        return all.firstOrNull { it.tag == tag }?.fromJson(id, obj)
    }

    /** Packs an ordered list into one JSON array string. */
    fun encodeList(widgets: List<SweWidget>): String =
        JSONArray().apply { widgets.forEach { put(it.toJson()) } }.toString()

    /** Unpacks [encodeList]; malformed input yields an empty list rather than an error. */
    fun decodeList(raw: String?): List<SweWidget> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let(::fromJson)
        }
    }

    /**
     * Rebuilds the (left, right) lists from the pre-widget flat settings, one widget per enabled
     * item in the old clock, unread, battery order. [get] looks a value up by its old camelCase
     * export name, so the same conversion serves both the stored keys and an old backup.
     * One-off conversion: delete it once pre-widget settings no longer matter.
     */
    fun fromLegacy(get: (String) -> Any?): Pair<List<SweWidget>, List<SweWidget>> {
        fun bool(name: String) = get(name) as? Boolean ?: false
        fun int(name: String, default: Int) = (get(name) as? Number)?.toInt() ?: default
        fun color(name: String) = CutoutColor.deserialize(get(name) as? String)
        fun side(name: String, default: InfoSide) =
            (get(name) as? String)?.let { raw -> InfoSide.entries.firstOrNull { it.name == raw } } ?: default

        val placed = buildList {
            if (bool("clockEnabled")) {
                add(
                    side("clockSide", InfoSide.LEFT) to ClockWidget.clamped(
                        id = LEGACY_CLOCK_ID,
                        color = color("clockColor"),
                        width = (get("clockWidth") as? Number)?.toFloat() ?: ROBOTO_FLEX_DEFAULT_WIDTH,
                        weight = int("clockWeight", ClockWidget.DEFAULT_WEIGHT),
                        sizePercent = int("clockSizePercent", ClockWidget.DEFAULT_SIZE_PERCENT),
                    )
                )
            }

            if (bool("unreadEnabled")) {
                add(
                    side("unreadSide", InfoSide.RIGHT) to UnreadWidget.clamped(
                        id = LEGACY_UNREAD_ID,
                        shape = (get("unreadShape") as? String)
                            ?.let { raw -> UnreadShape.entries.firstOrNull { it.name == raw } } ?: UnreadShape.STAR,
                        shapeColor = color("unreadShapeColor"),
                        pillWidthPercent = int("unreadPillWidthPercent", UnreadWidget.MIN_PILL_WIDTH_PERCENT),
                        textColor = color("unreadTextColor"),
                        countSilent = bool("unreadCountSilent"),
                    )
                )
            }

            if (bool("batteryEnabled")) {
                add(
                    side("batterySide", InfoSide.RIGHT) to BatteryWidget(
                        id = LEGACY_BATTERY_ID,
                        color = color("batteryColor"),
                        percentage = (get("batteryPercentage") as? String)
                            ?.let { raw -> BatteryPercentage.entries.firstOrNull { it.name == raw } }
                            ?: BatteryPercentage.INSIDE,
                        percentageSide = side("batteryPercentageSide", InfoSide.RIGHT),
                        percentSign = bool("batteryPercentSign"),
                        percentageColor = color("batteryPercentageColor"),
                    )
                )
            }
        }

        return placed.filter { it.first == InfoSide.LEFT }.map { it.second } to
            placed.filter { it.first == InfoSide.RIGHT }.map { it.second }
    }
}
