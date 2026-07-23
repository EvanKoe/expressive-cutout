package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.core.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.iconDataStore: DataStore<Preferences> by preferencesDataStore(name = "icon_prefs")

/**
 * Persists the user's per-event icon overrides as tagged [IconSource] strings. An absent
 * entry means "use the built-in default icon" for that event type.
 */
class IconPreferences(private val context: Context) {

    /** Emits the current map of overridden event types to their chosen icon source. */
    val customIcons: Flow<Map<SystemEventType, IconSource>> =
        context.iconDataStore.data.map { prefs ->
            SystemEventType.entries.mapNotNull { type ->
                prefs[type.preferenceKey]?.let(IconSource::decode)?.let { source -> type to source }
            }.toMap()
        }

    suspend fun setIcon(type: SystemEventType, source: IconSource) {
        context.iconDataStore.edit { prefs -> prefs[type.preferenceKey] = source.encode() }
    }

    suspend fun clearIcon(type: SystemEventType) {
        context.iconDataStore.edit { prefs -> prefs.remove(type.preferenceKey) }
    }

    private val SystemEventType.preferenceKey: Preferences.Key<String>
        get() = stringPreferencesKey("icon_source_$name")
}
