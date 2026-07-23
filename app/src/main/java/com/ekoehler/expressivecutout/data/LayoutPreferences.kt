package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_prefs")

/** Persists the island's size and position, always emitting values clamped to valid ranges. */
class LayoutPreferences(private val context: Context) {

    val layout: Flow<IslandLayout> = context.layoutDataStore.data.map { prefs ->
        IslandLayout.of(
            widthDp = prefs[WIDTH] ?: IslandLayout.DEFAULT_WIDTH_DP,
            heightDp = prefs[HEIGHT] ?: IslandLayout.DEFAULT_HEIGHT_DP,
            offsetXDp = prefs[OFFSET_X] ?: IslandLayout.DEFAULT_OFFSET_X_DP,
            offsetYDp = prefs[OFFSET_Y] ?: IslandLayout.DEFAULT_OFFSET_Y_DP,
        )
    }

    suspend fun setWidth(widthDp: Int) = context.layoutDataStore.edit { prefs ->
        prefs[WIDTH] = widthDp.coerceIn(IslandLayout.MIN_WIDTH_DP, IslandLayout.MAX_WIDTH_DP)
    }

    suspend fun setHeight(heightDp: Int) = context.layoutDataStore.edit { prefs ->
        prefs[HEIGHT] = heightDp.coerceIn(IslandLayout.MIN_HEIGHT_DP, IslandLayout.MAX_HEIGHT_DP)
    }

    suspend fun setOffsetX(offsetXDp: Int) = context.layoutDataStore.edit { prefs ->
        prefs[OFFSET_X] = offsetXDp.coerceIn(IslandLayout.MIN_OFFSET_X_DP, IslandLayout.MAX_OFFSET_X_DP)
    }

    suspend fun setOffsetY(offsetYDp: Int) = context.layoutDataStore.edit { prefs ->
        prefs[OFFSET_Y] = offsetYDp.coerceIn(IslandLayout.MIN_OFFSET_Y_DP, IslandLayout.MAX_OFFSET_Y_DP)
    }

    suspend fun reset() = context.layoutDataStore.edit { it.clear() }

    private companion object {
        val WIDTH = intPreferencesKey("island_width")
        val HEIGHT = intPreferencesKey("island_height")
        val OFFSET_X = intPreferencesKey("island_offset_x")
        val OFFSET_Y = intPreferencesKey("island_offset_y")
    }
}
