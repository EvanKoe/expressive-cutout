package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Backing store for the resting pill's widgets. */
private val Context.emptyPillInfoDataStore: DataStore<Preferences> by preferencesDataStore(name = "empty_pill_info_prefs")

/** Which half of the empty pill a widget sits on. */
enum class InfoSide { LEFT, RIGHT }

/** The badge drawn behind the unread count. */
enum class UnreadShape { STAR, PILL, NONE }

/** Where the battery percentage is drawn relative to the battery glyph. */
enum class BatteryPercentage { NEXT_TO, INSIDE, NONE }

/**
 * The widgets on the resting pill, [left] and [right] of the camera hole, each listed in the order
 * they appear on screen from left to right.
 */
data class EmptyPillInfoSettings(
    val left: List<SweWidget> = emptyList(),
    val right: List<SweWidget> = emptyList(),
) {
    /** The list for [side]. */
    fun on(side: InfoSide): List<SweWidget> = if (side == InfoSide.LEFT) left else right

    /** This settings object with [side]'s list replaced by [widgets]. */
    fun withSide(side: InfoSide, widgets: List<SweWidget>): EmptyPillInfoSettings =
        if (side == InfoSide.LEFT) copy(left = widgets) else copy(right = widgets)

    /** Which side holds the widget with [id], or null when it isn't placed. */
    fun sideOf(id: String): InfoSide? = when {
        left.any { it.id == id } -> InfoSide.LEFT
        right.any { it.id == id } -> InfoSide.RIGHT
        else -> null
    }
}

/**
 * Persists the resting pill's widget lists. Settings saved before widgets existed (one flat clock /
 * unread / battery each) are read through [SweWidget.fromLegacy] until the lists are first saved,
 * which also drops the old keys — so nothing is rewritten until the user edits.
 */
class EmptyPillInfoPreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<EmptyPillInfoSettings> = context.emptyPillInfoDataStore.data.map { read(it) }

    /**
     * Applies [transform] to the stored lists and saves both in one write, reading and writing inside
     * the same transaction so quick successive edits never overwrite each other and a widget moving
     * sides is never in both lists or neither.
     */
    suspend fun update(transform: (EmptyPillInfoSettings) -> EmptyPillInfoSettings) =
        context.emptyPillInfoDataStore.edit {
            val next = transform(read(it))
            it.writeLists(next.left, next.right)
        }

    /** The stored lists, or the pre-widget settings converted when no list has been saved yet. */
    private fun read(prefs: Preferences): EmptyPillInfoSettings {
        if (prefs.contains(LEFT_WIDGETS) || prefs.contains(RIGHT_WIDGETS)) {
            return EmptyPillInfoSettings(
                left = SweWidget.decodeList(prefs[LEFT_WIDGETS]),
                right = SweWidget.decodeList(prefs[RIGHT_WIDGETS]),
            )
        }
        val byName = prefs.asMap().mapKeys { (key, _) -> key.name }
        val (left, right) = SweWidget.fromLegacy { name -> byName[name.toSnakeCase()] }
        return EmptyPillInfoSettings(left, right)
    }

    /** Exports both lists as JSON arrays. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put(LEFT_JSON, JSONArray(SweWidget.encodeList(s.left)))
            put(RIGHT_JSON, JSONArray(SweWidget.encodeList(s.right)))
        }.toString()
    }

    /**
     * Applies an export. A backup made before widgets existed carries the old flat fields instead of
     * lists, and is converted the same way the stored settings are.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        val (left, right) = if (obj.has(LEFT_JSON) || obj.has(RIGHT_JSON)) {
            SweWidget.decodeList(obj.optJSONArray(LEFT_JSON)?.toString()) to
                SweWidget.decodeList(obj.optJSONArray(RIGHT_JSON)?.toString())
        } else {
            SweWidget.fromLegacy { name -> obj.opt(name).takeUnless { it == JSONObject.NULL } }
        }
        context.emptyPillInfoDataStore.edit { it.writeLists(left, right) }
    }

    /** Writes both lists and clears every pre-widget key, which are no longer read once lists exist. */
    private fun MutablePreferences.writeLists(left: List<SweWidget>, right: List<SweWidget>) {
        asMap().keys.filter { it != LEFT_WIDGETS && it != RIGHT_WIDGETS }.forEach { remove(it) }
        this[LEFT_WIDGETS] = SweWidget.encodeList(left)
        this[RIGHT_WIDGETS] = SweWidget.encodeList(right)
    }

    /** Maps an old export name (`clockSizePercent`) to its stored key name (`clock_size_percent`). */
    private fun String.toSnakeCase(): String = replace(Regex("([A-Z])")) { "_" + it.value.lowercase() }

    private companion object {
        val LEFT_WIDGETS = stringPreferencesKey("left_widgets")
        val RIGHT_WIDGETS = stringPreferencesKey("right_widgets")
        const val LEFT_JSON = "leftWidgets"
        const val RIGHT_JSON = "rightWidgets"
    }
}
