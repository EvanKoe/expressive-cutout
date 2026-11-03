package com.ekoehler.expressivecutout.data

import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * One widget on the resting (SWE) pill. Each instance carries its own [id], so the same kind can be
 * added several times with different styling, and its own settings. Stored as JSON by [toJson] /
 * [fromJson]; the side and order it sits in come from the list holding it.
 */
sealed interface SweWidget {
    val id: String

    /** This widget as a tagged JSON object, read back by [fromJson]. */
    fun toJson(): JSONObject

    /** The time, in Roboto Flex at the chosen width, weight, size and colour. Null colour = default ink. */
    data class Clock(
        override val id: String = newId(),
        val color: CutoutColor? = null,
        val width: Float = ROBOTO_FLEX_DEFAULT_WIDTH,
        val weight: Int = DEFAULT_CLOCK_WEIGHT,
        val sizePercent: Int = DEFAULT_PERCENT,
    ) : SweWidget {
        override fun toJson(): JSONObject = JSONObject().apply {
            put(TYPE, CLOCK_TYPE)
            put(ID, id)
            putColor("color", color)
            put("width", width.toDouble())
            put("weight", weight)
            put("sizePercent", sizePercent)
        }
    }

    /** The unread notification count on a star, a pill or bare. Hidden while the count is 0. */
    data class Unread(
        override val id: String = newId(),
        val shape: UnreadShape = UnreadShape.STAR,
        val shapeColor: CutoutColor? = null,
        val pillWidthPercent: Int = MIN_PILL_WIDTH_PERCENT,
        val textColor: CutoutColor? = null,
        val countSilent: Boolean = false,
    ) : SweWidget {
        override fun toJson(): JSONObject = JSONObject().apply {
            put(TYPE, UNREAD_TYPE)
            put(ID, id)
            put("shape", shape.name)
            putColor("shapeColor", shapeColor)
            put("pillWidthPercent", pillWidthPercent)
            putColor("textColor", textColor)
            put("countSilent", countSilent)
        }
    }

    /** A battery glyph with its percentage inside, beside it (on [percentageSide]) or hidden. */
    data class Battery(
        override val id: String = newId(),
        val color: CutoutColor? = null,
        val percentage: BatteryPercentage = BatteryPercentage.INSIDE,
        val percentageSide: InfoSide = InfoSide.RIGHT,
        val percentSign: Boolean = false,
        val percentageColor: CutoutColor? = null,
    ) : SweWidget {
        override fun toJson(): JSONObject = JSONObject().apply {
            put(TYPE, BATTERY_TYPE)
            put(ID, id)
            putColor("color", color)
            put("percentage", percentage.name)
            put("percentageSide", percentageSide.name)
            put("percentSign", percentSign)
            putColor("percentageColor", percentageColor)
        }
    }

    companion object {
        const val MIN_CLOCK_WEIGHT = 100
        const val MAX_CLOCK_WEIGHT = 1000
        const val MIN_CLOCK_SIZE_PERCENT = 50
        const val MAX_CLOCK_SIZE_PERCENT = 150
        const val MIN_PILL_WIDTH_PERCENT = 100
        const val MAX_PILL_WIDTH_PERCENT = 300
        private const val DEFAULT_CLOCK_WEIGHT = 700
        private const val DEFAULT_PERCENT = 100

        private const val TYPE = "type"
        private const val ID = "id"
        private const val CLOCK_TYPE = "clock"
        private const val UNREAD_TYPE = "unread"
        private const val BATTERY_TYPE = "battery"

        /**
         * Fixed ids for the widgets converted from the pre-widget settings, so they stay stable
         * across reads until the converted lists are first saved.
         */
        private const val LEGACY_CLOCK_ID = "legacy-clock"
        private const val LEGACY_UNREAD_ID = "legacy-unread"
        private const val LEGACY_BATTERY_ID = "legacy-battery"

        /** A fresh unique id for a newly added widget. */
        fun newId(): String = UUID.randomUUID().toString()

        /** Reads one [toJson] object; null for an unknown type or a missing id, so one bad entry is dropped alone. */
        fun fromJson(obj: JSONObject): SweWidget? {
            val id = obj.optString(ID).takeIf { it.isNotEmpty() } ?: return null
            return when (obj.optString(TYPE)) {
                CLOCK_TYPE -> Clock(
                    id = id,
                    color = obj.color("color"),
                    width = obj.optDouble("width", ROBOTO_FLEX_DEFAULT_WIDTH.toDouble()).toFloat().coerceWidth(),
                    weight = obj.optInt("weight", DEFAULT_CLOCK_WEIGHT).coerceWeight(),
                    sizePercent = obj.optInt("sizePercent", DEFAULT_PERCENT).coerceClockSize(),
                )
                UNREAD_TYPE -> Unread(
                    id = id,
                    shape = obj.enum<UnreadShape>("shape") ?: UnreadShape.STAR,
                    shapeColor = obj.color("shapeColor"),
                    pillWidthPercent = obj.optInt("pillWidthPercent", MIN_PILL_WIDTH_PERCENT).coercePillWidth(),
                    textColor = obj.color("textColor"),
                    countSilent = obj.optBoolean("countSilent", false),
                )
                BATTERY_TYPE -> Battery(
                    id = id,
                    color = obj.color("color"),
                    percentage = obj.enum<BatteryPercentage>("percentage") ?: BatteryPercentage.INSIDE,
                    percentageSide = obj.enum<InfoSide>("percentageSide") ?: InfoSide.RIGHT,
                    percentSign = obj.optBoolean("percentSign", false),
                    percentageColor = obj.color("percentageColor"),
                )
                else -> null
            }
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
         */
        fun fromLegacy(get: (String) -> Any?): Pair<List<SweWidget>, List<SweWidget>> {
            fun bool(name: String) = get(name) as? Boolean ?: false
            fun int(name: String, default: Int) = (get(name) as? Number)?.toInt() ?: default
            fun color(name: String) = CutoutColor.deserialize(get(name) as? String)
            fun side(name: String, default: InfoSide) =
                (get(name) as? String)?.let { raw -> InfoSide.entries.firstOrNull { it.name == raw } } ?: default

            val placed = buildList {
                if (bool("clockEnabled")) add(
                    side("clockSide", InfoSide.LEFT) to Clock(
                        id = LEGACY_CLOCK_ID,
                        color = color("clockColor"),
                        width = ((get("clockWidth") as? Number)?.toFloat() ?: ROBOTO_FLEX_DEFAULT_WIDTH).coerceWidth(),
                        weight = int("clockWeight", DEFAULT_CLOCK_WEIGHT).coerceWeight(),
                        sizePercent = int("clockSizePercent", DEFAULT_PERCENT).coerceClockSize(),
                    )
                )
                if (bool("unreadEnabled")) add(
                    side("unreadSide", InfoSide.RIGHT) to Unread(
                        id = LEGACY_UNREAD_ID,
                        shape = (get("unreadShape") as? String)
                            ?.let { raw -> UnreadShape.entries.firstOrNull { it.name == raw } } ?: UnreadShape.STAR,
                        shapeColor = color("unreadShapeColor"),
                        pillWidthPercent = int("unreadPillWidthPercent", MIN_PILL_WIDTH_PERCENT).coercePillWidth(),
                        textColor = color("unreadTextColor"),
                        countSilent = bool("unreadCountSilent"),
                    )
                )
                if (bool("batteryEnabled")) add(
                    side("batterySide", InfoSide.RIGHT) to Battery(
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
            return placed.filter { it.first == InfoSide.LEFT }.map { it.second } to
                placed.filter { it.first == InfoSide.RIGHT }.map { it.second }
        }

        /** Writes [color] under [name], or JSON null for the default. */
        private fun JSONObject.putColor(name: String, color: CutoutColor?) {
            put(name, color?.serialize() ?: JSONObject.NULL)
        }

        /** Reads the colour under [name]; absent, null or unparseable means the default. */
        private fun JSONObject.color(name: String): CutoutColor? =
            if (isNull(name)) null else CutoutColor.deserialize(optString(name))

        /** Reads the enum under [name], or null when absent or no longer a valid name. */
        private inline fun <reified E : Enum<E>> JSONObject.enum(name: String): E? =
            optString(name).takeIf { it.isNotEmpty() }?.let { raw -> enumValues<E>().firstOrNull { it.name == raw } }

        /** Clamps the setting ranges, shared by [fromJson] and [fromLegacy]. */
        private fun Float.coerceWidth() = coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH)
        private fun Int.coerceWeight() = coerceIn(MIN_CLOCK_WEIGHT, MAX_CLOCK_WEIGHT)
        private fun Int.coerceClockSize() = coerceIn(MIN_CLOCK_SIZE_PERCENT, MAX_CLOCK_SIZE_PERCENT)
        private fun Int.coercePillWidth() = coerceIn(MIN_PILL_WIDTH_PERCENT, MAX_PILL_WIDTH_PERCENT)
    }
}
