package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Backing store for the permission-dot settings. */
private val Context.permissionDotDataStore: DataStore<Preferences> by preferencesDataStore(name = "permission_dot_prefs")

/**
 * Which end of the collapsed pill the permission dots sit on: [LEFT] tucks them between the pill's
 * icon and the camera hole, [RIGHT] puts them on the trailing edge, clear of both.
 */
enum class PermissionDotPosition { LEFT, RIGHT }

/**
 * Whether the island marks live microphone, camera and location use with a coloured dot, and where
 * on the pill that dot goes.
 *
 * Like [StatusBarPreferences] this stores the *wish* rather than the achieved state: reading which
 * app is using what needs shell privileges, so `PermissionUsageMonitor` only acts on [enabled] once
 * Shizuku is reachable. Leaving the wish saved is what lets the dots come back on their own after a
 * reboot has stopped Shizuku.
 */
class PermissionDotPreferences(private val context: Context) : JsonSerializable {

    val enabled: Flow<Boolean> = context.permissionDotDataStore.data.map { prefs ->
        prefs[ENABLED] ?: false
    }

    /** The chosen end of the pill, falling back to [DEFAULT_POSITION] for an unreadable value. */
    val position: Flow<PermissionDotPosition> = context.permissionDotDataStore.data.map { prefs ->
        prefs[POSITION]?.let { runCatching { PermissionDotPosition.valueOf(it) }.getOrNull() }
            ?: DEFAULT_POSITION
    }

    suspend fun setEnabled(enabled: Boolean) = context.permissionDotDataStore.edit { prefs ->
        prefs[ENABLED] = enabled
    }

    suspend fun setPosition(position: PermissionDotPosition) = context.permissionDotDataStore.edit { prefs ->
        prefs[POSITION] = position.name
    }

    /**
     * Exports the permission-dot settings in a JSON string
     * { enabled: boolean, position: "LEFT" | "RIGHT" }
     */
    override suspend fun toJson(): String {
        val enabled = enabled.first()
        val position = position.first()
        return JSONObject().apply {
            put("enabled", enabled)
            put("position", position.name)
        }.toString()
    }

    /**
     * Applies { enabled: boolean, position: "LEFT" | "RIGHT" } exported by [toJson]. Each missing or
     * unrecognised field leaves its setting untouched, so a document from a build without this
     * section can't silently turn the dots on.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        if (obj.has("enabled")) setEnabled(obj.optBoolean("enabled", false))
        if (obj.has("position")) {
            runCatching { PermissionDotPosition.valueOf(obj.optString("position")) }
                .getOrNull()
                ?.let { setPosition(it) }
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("permission_dot_enabled")
        val POSITION = stringPreferencesKey("permission_dot_position")

        val DEFAULT_POSITION = PermissionDotPosition.RIGHT
    }
}
