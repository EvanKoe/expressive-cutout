package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.core.DynamicTile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Backing store for which dynamic tiles the user has enabled. */
private val Context.dynamicTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "dynamic_tile_prefs")

/**
 * Persists whether each dynamic tile is allowed to appear on the cutout. Absent means enabled,
 * so tiles show by default and only explicit opt-outs are stored — mirroring [EventPreferences]
 * but kept separate because tiles are a distinct concept from system events.
 */
class DynamicTilePreferences(private val context: Context) : JsonSerializable {

    val enabled: Flow<Map<DynamicTile, Boolean>> = context.dynamicTileDataStore.data.map { prefs ->
        DynamicTile.entries.associateWith { tile -> prefs[tile.key] ?: true }
    }

    suspend fun setEnabled(tile: DynamicTile, enabled: Boolean) = context.dynamicTileDataStore.edit {
        it[tile.key] = enabled
    }

    /**
     * Exports the per-tile enabled flags as JSON { enabled: { TILE_NAME: true, ... } }. The map is
     * keyed by [DynamicTile], so build the object by hand with each tile's name as the key — a
     * JSONObject key must be a String.
     */
    override suspend fun toJson(): String {
        val e = enabled.first()
        return JSONObject().apply {
            put("enabled", JSONObject().apply { e.forEach { (tile, on) -> put(tile.name, on) } })
        }.toString()
    }

    /**
     * Applies { enabled: { TILE_NAME: bool, ... } } exported by [toJson]. Every known tile is set
     * from the document, defaulting an absent entry to enabled (the store's own default), in one edit.
     */
    override suspend fun fromJson(json: String) {
        val enabledObj = JSONObject(json).optJSONObject("enabled") ?: return
        context.dynamicTileDataStore.edit { prefs ->
            DynamicTile.entries.forEach { tile ->
                prefs[tile.key] = enabledObj.optBoolean(tile.name, true)
            }
        }
    }

    private val DynamicTile.key: Preferences.Key<Boolean>
        get() = booleanPreferencesKey("tile_enabled_$name")
}
