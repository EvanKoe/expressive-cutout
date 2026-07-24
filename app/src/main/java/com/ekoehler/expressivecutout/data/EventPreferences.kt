package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.core.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.eventDataStore: DataStore<Preferences> by preferencesDataStore(name = "event_prefs")

/**
 * Persists whether each system event is allowed to appear on the island. Absent means enabled,
 * so events show by default and only explicit opt-outs are stored.
 */
class EventPreferences(private val context: Context) {

    val enabled: Flow<Map<SystemEventType, Boolean>> = context.eventDataStore.data.map { prefs ->
        SystemEventType.entries.associateWith { type -> prefs[type.key] ?: true }
    }

    /** Whether the music tile (now-playing media) is allowed on the island. Enabled by default. */
    val musicEnabled: Flow<Boolean> = context.eventDataStore.data.map { it[MUSIC_ENABLED] ?: true }

    suspend fun setEnabled(type: SystemEventType, enabled: Boolean) = context.eventDataStore.edit {
        it[type.key] = enabled
    }

    suspend fun setMusicEnabled(enabled: Boolean) = context.eventDataStore.edit {
        it[MUSIC_ENABLED] = enabled
    }

    private val SystemEventType.key: Preferences.Key<Boolean>
        get() = booleanPreferencesKey("event_enabled_$name")

    private companion object {
        val MUSIC_ENABLED = booleanPreferencesKey("music_tile_enabled")
    }
}
