package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.core.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Backing store for the user's per-event icon overrides. */
private val Context.iconDataStore: DataStore<Preferences> by preferencesDataStore(name = "icon_prefs")

/**
 * Persists the user's per-event icon overrides as tagged [IconSource] strings. An absent
 * entry means "use the built-in default icon" for that event type.
 */
class IconPreferences(private val context: Context) : JsonSerializable {

    /** Emits the current map of overridden event types to their chosen icon source. */
    val customIcons: Flow<Map<SystemEventType, IconSource>> =
        context.iconDataStore.data.map { prefs ->
            SystemEventType.entries.mapNotNull { type ->
                prefs[type.preferenceKey]?.let(IconSource::decode)?.let { source -> type to source }
            }.toMap()
        }

    /**
     * Exports the custom icons in JSON { customIcons: { systemEventType: string, iconSource: string }[] }.
     * The iconSource is the tagged [IconSource.encode] string; read it back with [IconSource.decode].
     */
    override suspend fun toJson(): String {
        fun serializePair(pair: Map.Entry<SystemEventType, IconSource>): JSONObject =
            JSONObject().apply {
                put("systemEventType", pair.key.name)
                put("iconSource", pair.value.encode())
            }

        val i = customIcons.first()
        return JSONObject().apply {
            put("customIcons", JSONArray(i.map { serializePair(it) }))
        }.toString()
    }

    /**
     * Applies the { customIcons: [...] } array exported by [toJson] as a full replacement: every
     * event type listed with a valid source is overridden, and every event type not listed is reset
     * to its built-in default, so the imported set matches the document exactly. Applied in one edit.
     */
    override suspend fun fromJson(json: String) {
        val arr = JSONObject(json).optJSONArray("customIcons") ?: return
        val decoded = buildMap {
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                val type = runCatching { SystemEventType.valueOf(entry.optString("systemEventType")) }
                    .getOrNull() ?: continue
                val source = IconSource.decode(entry.optString("iconSource")) ?: continue
                put(type, source)
            }
        }
        context.iconDataStore.edit { prefs ->
            SystemEventType.entries.forEach { type ->
                val source = decoded[type]
                if (source != null) prefs[type.preferenceKey] = source.encode()
                else prefs.remove(type.preferenceKey)
            }
        }
    }

    /** Overrides the icon shown for [type]. Paired with [clearIcon]. */
    suspend fun setIcon(type: SystemEventType, source: IconSource) {
        context.iconDataStore.edit { prefs -> prefs[type.preferenceKey] = source.encode() }
    }

    /** Drops the override for [type], so the event falls back to its built-in icon. */
    suspend fun clearIcon(type: SystemEventType) {
        context.iconDataStore.edit { prefs -> prefs.remove(type.preferenceKey) }
    }

    private val SystemEventType.preferenceKey: Preferences.Key<String>
        get() = stringPreferencesKey("icon_source_$name")
}
