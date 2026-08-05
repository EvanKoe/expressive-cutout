package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.core.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.eventDataStore: DataStore<Preferences> by preferencesDataStore(name = "event_prefs")

/**
 * Persists whether each system event is allowed to appear on the island. Absent means enabled,
 * so events show by default and only explicit opt-outs are stored.
 */
class EventPreferences(private val context: Context) : JsonSerializable {

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

    /**
     * Per-event override for how long the event's cutout stays before auto-dismissing. Only events
     * the user has explicitly tuned appear here; an absent entry means the event follows the global
     * "normal cutout duration" from Behaviour.
     */
    val durations: Flow<Map<SystemEventType, Int>> = context.eventDataStore.data.map { prefs ->
        SystemEventType.entries.mapNotNull { type ->
            prefs[type.durationKey]?.let { seconds -> type to seconds }
        }.toMap()
    }

    /**
     * Per-event choice (for events that ship a Lottie animation) between the animated icon and the
     * plain default glyph. Only events the user has explicitly toggled appear here; absent means on.
     */
    val animatedIcons: Flow<Map<SystemEventType, Boolean>> = context.eventDataStore.data.map { prefs ->
        SystemEventType.entries.mapNotNull { type ->
            prefs[type.animatedKey]?.let { enabled -> type to enabled }
        }.toMap()
    }

    /**
     * Per-event choice for whether the animated icon loops (else it plays once and holds). Absent
     * means the event's own built-in default (see [animationLoopsByDefault]).
     */
    val animatedIconLoops: Flow<Map<SystemEventType, Boolean>> = context.eventDataStore.data.map { prefs ->
        SystemEventType.entries.mapNotNull { type ->
            prefs[type.loopKey]?.let { loop -> type to loop }
        }.toMap()
    }

    /**
     * Per-event colour override. When set it wins over both the event's own accent and the global
     * "Dynamic color for all events" role. Only events the user has explicitly recoloured appear
     * here; an absent entry means the event follows the default accent (or the dynamic role).
     */
    val colors: Flow<Map<SystemEventType, CutoutColor>> = context.eventDataStore.data.map { prefs ->
        SystemEventType.entries.mapNotNull { type ->
            CutoutColor.deserialize(prefs[type.colorKey])?.let { color -> type to color }
        }.toMap()
    }

    /**
     * Exports every per-event setting as JSON. The four [SystemEventType]-keyed maps become nested
     * objects whose keys are the event-type names (a JSONObject key must be a String, so the enum
     * key is converted with [Enum.name]); per-event colours are their [CutoutColor.serialize] strings.
     */
    override suspend fun toJson(): String {
        fun <V> Map<SystemEventType, V>.toJsonObject(transform: (V) -> Any): JSONObject =
            JSONObject().apply { forEach { (type, value) -> put(type.name, transform(value)) } }

        return JSONObject().apply {
            put("enabled", enabled.first().toJsonObject { it })
            put("dynamicColor", dynamicColor.first())
            put("dynamicColorRole", dynamicColorRole.first().name)
            put("dynamicColorOpacity", dynamicColorOpacity.first().toDouble())
            put("durations", durations.first().toJsonObject { it })
            put("animatedIcons", animatedIcons.first().toJsonObject { it })
            put("animatedIconLoops", animatedIconLoops.first().toJsonObject { it })
            put("colors", colors.first().toJsonObject { it.serialize() })
        }.toString()
    }

    /**
     * Applies every per-event setting exported by [toJson]. The [enabled] map is dense (one entry
     * per event, defaulting an absent name to enabled); the sparse override maps (durations, animated
     * icons, loops, colours) are applied as a full replacement — an event present in the map gets its
     * override, an event absent has any existing override cleared — so the imported state matches the
     * document exactly. Done in one edit.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        val enabled = obj.optJSONObject("enabled")
        val durations = obj.optJSONObject("durations")
        val animated = obj.optJSONObject("animatedIcons")
        val loops = obj.optJSONObject("animatedIconLoops")
        val colors = obj.optJSONObject("colors")

        context.eventDataStore.edit { prefs ->
            SystemEventType.entries.forEach { type ->
                val name = type.name
                enabled?.let { prefs[type.key] = it.optBoolean(name, true) }
                durations?.let {
                    if (it.has(name)) prefs[type.durationKey] = it.getInt(name) else prefs.remove(type.durationKey)
                }
                animated?.let {
                    if (it.has(name)) prefs[type.animatedKey] = it.getBoolean(name) else prefs.remove(type.animatedKey)
                }
                loops?.let {
                    if (it.has(name)) prefs[type.loopKey] = it.getBoolean(name) else prefs.remove(type.loopKey)
                }
                colors?.let {
                    val color = if (it.has(name) && !it.isNull(name)) CutoutColor.deserialize(it.optString(name)) else null
                    if (color == null) prefs.remove(type.colorKey) else prefs[type.colorKey] = color.serialize()
                }
            }
            if (obj.has("dynamicColor")) prefs[DYNAMIC_COLOR_KEY] = obj.getBoolean("dynamicColor")
            if (obj.has("dynamicColorRole") && !obj.isNull("dynamicColorRole")) {
                runCatching { DynamicRole.valueOf(obj.optString("dynamicColorRole")) }.getOrNull()
                    ?.let { prefs[DYNAMIC_COLOR_ROLE_KEY] = it.name }
            }
            if (obj.has("dynamicColorOpacity")) {
                prefs[DYNAMIC_COLOR_OPACITY_KEY] = obj.getDouble("dynamicColorOpacity").toFloat().coerceIn(0f, 1f)
            }
        }
    }

    suspend fun setEnabled(type: SystemEventType, enabled: Boolean) = context.eventDataStore.edit {
        it[type.key] = enabled
    }

    suspend fun setDuration(type: SystemEventType, seconds: Int) = context.eventDataStore.edit {
        it[type.durationKey] = seconds
    }

    /** Drop the override so the event falls back to the global normal cutout duration. */
    suspend fun clearDuration(type: SystemEventType) = context.eventDataStore.edit {
        it.remove(type.durationKey)
    }

    suspend fun setColor(type: SystemEventType, color: CutoutColor) = context.eventDataStore.edit {
        it[type.colorKey] = color.serialize()
    }

    /** Drop the override so the event falls back to its default accent (or the dynamic role). */
    suspend fun clearColor(type: SystemEventType) = context.eventDataStore.edit {
        it.remove(type.colorKey)
    }

    suspend fun setAnimatedIcon(type: SystemEventType, enabled: Boolean) = context.eventDataStore.edit {
        it[type.animatedKey] = enabled
    }

    suspend fun setAnimatedIconLoop(type: SystemEventType, loop: Boolean) = context.eventDataStore.edit {
        it[type.loopKey] = loop
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

    private val SystemEventType.durationKey: Preferences.Key<Int>
        get() = intPreferencesKey("event_duration_$name")

    private val SystemEventType.animatedKey: Preferences.Key<Boolean>
        get() = booleanPreferencesKey("event_animated_$name")

    private val SystemEventType.loopKey: Preferences.Key<Boolean>
        get() = booleanPreferencesKey("event_animated_loop_$name")

    private val SystemEventType.colorKey: Preferences.Key<String>
        get() = stringPreferencesKey("event_color_$name")

    private companion object {
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("events_dynamic_color")
        val DYNAMIC_COLOR_ROLE_KEY = stringPreferencesKey("events_dynamic_color_role")
        val DYNAMIC_COLOR_OPACITY_KEY = floatPreferencesKey("events_dynamic_color_opacity")
    }
}
