package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    /**
     * When on, every event drops its own accent colour and is drawn with the theme's primary /
     * on-primary pair instead. Absent means off.
     */
    val dynamicColor: Flow<Boolean> = context.eventDataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_KEY] ?: false
    }

    /**
     * Which Material You role (primary / secondary / tertiary) tints the badge when [dynamicColor]
     * is on. Absent means primary.
     */
    val dynamicColorRole: Flow<DynamicRole> = context.eventDataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_ROLE_KEY]?.let { name ->
            runCatching { DynamicRole.valueOf(name) }.getOrNull()
        } ?: DynamicRole.PRIMARY
    }

    /**
     * Opacity (0..1) of the role-coloured badge background painted when [dynamicColor] is on.
     * Absent means fully opaque.
     */
    val dynamicColorOpacity: Flow<Float> = context.eventDataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_OPACITY_KEY]?.coerceIn(0f, 1f) ?: 1f
    }

    suspend fun setEnabled(type: SystemEventType, enabled: Boolean) = context.eventDataStore.edit {
        it[type.key] = enabled
    }

    suspend fun setDynamicColor(enabled: Boolean) = context.eventDataStore.edit {
        it[DYNAMIC_COLOR_KEY] = enabled
    }

    suspend fun setDynamicColorRole(role: DynamicRole) = context.eventDataStore.edit {
        it[DYNAMIC_COLOR_ROLE_KEY] = role.name
    }

    suspend fun setDynamicColorOpacity(opacity: Float) = context.eventDataStore.edit {
        it[DYNAMIC_COLOR_OPACITY_KEY] = opacity.coerceIn(0f, 1f)
    }

    private val SystemEventType.key: Preferences.Key<Boolean>
        get() = booleanPreferencesKey("event_enabled_$name")

    private companion object {
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("events_dynamic_color")
        val DYNAMIC_COLOR_ROLE_KEY = stringPreferencesKey("events_dynamic_color_role")
        val DYNAMIC_COLOR_OPACITY_KEY = floatPreferencesKey("events_dynamic_color_opacity")
    }
}
