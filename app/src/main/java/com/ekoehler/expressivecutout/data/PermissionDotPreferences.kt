package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.runtime.Immutable
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
 * Which resources the user wants marked. A resource switched off is never polled for and never
 * drawn, so turning all three off is the same as turning the feature off — hence [any], which the
 * monitor uses to decide whether reading app ops is worth it at all.
 */
@Immutable
data class PermissionDotKinds(
    val location: Boolean = true,
    val camera: Boolean = true,
    val microphone: Boolean = true,
) {
    /** Whether at least one resource is still watched. */
    val any: Boolean get() = location || camera || microphone
}

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

    /**
     * Which resources are watched, each defaulting to on so an existing install that only ever saw
     * the single switch keeps marking all three.
     */
    val kinds: Flow<PermissionDotKinds> = context.permissionDotDataStore.data.map { prefs ->
        PermissionDotKinds(
            location = prefs[LOCATION] ?: true,
            camera = prefs[CAMERA] ?: true,
            microphone = prefs[MICROPHONE] ?: true,
        )
    }

    suspend fun setEnabled(enabled: Boolean) = context.permissionDotDataStore.edit { prefs ->
        prefs[ENABLED] = enabled
    }

    suspend fun setPosition(position: PermissionDotPosition) = context.permissionDotDataStore.edit { prefs ->
        prefs[POSITION] = position.name
    }

    suspend fun setLocation(enabled: Boolean) = context.permissionDotDataStore.edit { prefs ->
        prefs[LOCATION] = enabled
    }

    suspend fun setCamera(enabled: Boolean) = context.permissionDotDataStore.edit { prefs ->
        prefs[CAMERA] = enabled
    }

    suspend fun setMicrophone(enabled: Boolean) = context.permissionDotDataStore.edit { prefs ->
        prefs[MICROPHONE] = enabled
    }

    /**
     * Exports the permission-dot settings in a JSON string
     * { enabled: boolean, position: "LEFT" | "RIGHT", location, camera, microphone: boolean }
     */
    override suspend fun toJson(): String {
        val enabled = enabled.first()
        val position = position.first()
        val kinds = kinds.first()
        return JSONObject().apply {
            put("enabled", enabled)
            put("position", position.name)
            put("location", kinds.location)
            put("camera", kinds.camera)
            put("microphone", kinds.microphone)
        }.toString()
    }

    /**
     * Applies the document exported by [toJson]. Each missing or unrecognised field leaves its
     * setting untouched, so a document from a build without this section can't silently turn the
     * dots on.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        if (obj.has("enabled")) setEnabled(obj.optBoolean("enabled", false))
        if (obj.has("location")) setLocation(obj.optBoolean("location", true))
        if (obj.has("camera")) setCamera(obj.optBoolean("camera", true))
        if (obj.has("microphone")) setMicrophone(obj.optBoolean("microphone", true))
        if (obj.has("position")) {
            runCatching { PermissionDotPosition.valueOf(obj.optString("position")) }
                .getOrNull()
                ?.let { setPosition(it) }
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("permission_dot_enabled")
        val POSITION = stringPreferencesKey("permission_dot_position")
        val LOCATION = booleanPreferencesKey("permission_dot_location")
        val CAMERA = booleanPreferencesKey("permission_dot_camera")
        val MICROPHONE = booleanPreferencesKey("permission_dot_microphone")

        val DEFAULT_POSITION = PermissionDotPosition.RIGHT
    }
}
