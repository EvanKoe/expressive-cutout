package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// Its own file: the recent picks are shared by every colour picker in the app, and a second
// delegate over an existing file throws "multiple DataStores active for the same file".
/** Backing store for the colours the user picked most recently. */
private val Context.recentColorsDataStore: DataStore<Preferences> by preferencesDataStore(name = "recent_colors")

/** How many custom picks [RecentColorPreferences] keeps, newest first. */
const val MAX_RECENT_COLORS = 3

/**
 * The last [MAX_RECENT_COLORS] colours the user mixed in the colour picker, newest first, shared by
 * every `ColorPickerCard` in the app so a colour chosen on one screen is one tap away on the next.
 *
 * Only picks made through the picker dialog are recorded — tapping a preset or a dynamic role isn't
 * a custom colour and doesn't push anything out of the list. Stored as one ordered string rather
 * than a string set, because a set loses the "newest first" ordering that makes the list useful.
 */
class RecentColorPreferences(private val context: Context) : JsonSerializable {

    val recentColors: Flow<List<Long>> = context.recentColorsDataStore.data.map { prefs ->
        prefs[RECENT_KEY].toArgbList()
    }

    /**
     * Records [argb] as the newest pick, dropping the oldest once the list is full. A colour already
     * in the list moves back to the front instead of being stored twice.
     */
    suspend fun record(argb: Long) = context.recentColorsDataStore.edit { prefs ->
        val current = prefs[RECENT_KEY].toArgbList()
        prefs[RECENT_KEY] = (listOf(argb) + current.filterNot { it == argb })
            .take(MAX_RECENT_COLORS)
            .joinToString(SEPARATOR)
    }

    /**
     * Parses the separated ARGB list, dropping anything unreadable and keeping at most
     * [MAX_RECENT_COLORS] so a corrupted value degrades to a shorter list instead of an error.
     */
    private fun String?.toArgbList(): List<Long> {
        if (isNullOrEmpty()) return emptyList()
        return split(SEPARATOR).mapNotNull { it.toLongOrNull() }.take(MAX_RECENT_COLORS)
    }

    private companion object {
        val RECENT_KEY = stringPreferencesKey("recent_colors")
        const val SEPARATOR = ","
    }

    /**
     * Exports the recent picks in a JSON string, newest first
     * { recentColors: number[] }
     */
    override suspend fun toJson(): String {
        val recents = recentColors.first()
        return JSONObject().apply {
            put("recentColors", JSONArray(recents))
        }.toString()
    }

    /**
     * Applies { recentColors: [...] } exported by [toJson] as a full replacement of the list. A
     * missing array clears it, matching the snapshot; anything past [MAX_RECENT_COLORS] is dropped,
     * so a document written by a build with a larger limit still imports cleanly.
     */
    override suspend fun fromJson(json: String) {
        val array = JSONObject(json).optJSONArray("recentColors")
        val recents = (0 until (array?.length() ?: 0))
            .mapNotNull { array?.opt(it)?.toString()?.toLongOrNull() }
            .take(MAX_RECENT_COLORS)
        context.recentColorsDataStore.edit { prefs ->
            prefs[RECENT_KEY] = recents.joinToString(SEPARATOR)
        }
    }
}
