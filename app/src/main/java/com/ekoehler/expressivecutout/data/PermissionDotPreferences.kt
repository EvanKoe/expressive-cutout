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
 * The colour each dot is drawn in: green for the camera, red for the microphone, blue for location.
 * The user can recolour any of them, so these are only the starting point.
 */
@Immutable
data class PermissionDotColors(
    val location: CutoutColor = DEFAULT_LOCATION_DOT_COLOR,
    val camera: CutoutColor = DEFAULT_CAMERA_DOT_COLOR,
    val microphone: CutoutColor = DEFAULT_MICROPHONE_DOT_COLOR,
)

/** The stock dot colours, also used as the color picker's "default" swatch. */
val DEFAULT_LOCATION_DOT_COLOR: CutoutColor = CutoutColor.Solid(0xFF3B82F6)
val DEFAULT_CAMERA_DOT_COLOR: CutoutColor = CutoutColor.Solid(0xFF19C337)
val DEFAULT_MICROPHONE_DOT_COLOR: CutoutColor = CutoutColor.Solid(0xFFE5484D)

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

    /**
     * The colour of each dot, falling back to that resource's stock colour for an unset or
     * unreadable value. Kept apart from [kinds] so recolouring a dot doesn't restart the app-op
     * polling loop that flow gates.
     */
    val colors: Flow<PermissionDotColors> = context.permissionDotDataStore.data.map { prefs ->
        PermissionDotColors(
            location = CutoutColor.deserialize(prefs[LOCATION_COLOR]) ?: DEFAULT_LOCATION_DOT_COLOR,
            camera = CutoutColor.deserialize(prefs[CAMERA_COLOR]) ?: DEFAULT_CAMERA_DOT_COLOR,
            microphone = CutoutColor.deserialize(prefs[MICROPHONE_COLOR]) ?: DEFAULT_MICROPHONE_DOT_COLOR,
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

    suspend fun setLocationColor(color: CutoutColor) = context.permissionDotDataStore.edit { prefs ->
        prefs[LOCATION_COLOR] = color.serialize()
    }

    suspend fun setCameraColor(color: CutoutColor) = context.permissionDotDataStore.edit { prefs ->
        prefs[CAMERA_COLOR] = color.serialize()
    }

    suspend fun setMicrophoneColor(color: CutoutColor) = context.permissionDotDataStore.edit { prefs ->
        prefs[MICROPHONE_COLOR] = color.serialize()
    }

    /**
     * Exports the permission-dot settings in a JSON string
     * { enabled: boolean, position: "LEFT" | "RIGHT", location, camera, microphone: boolean }
     */
    override suspend fun toJson(): String {
        val enabled = enabled.first()
        val position = position.first()
        val kinds = kinds.first()
        val colors = colors.first()
        return JSONObject().apply {
            put("enabled", enabled)
            put("position", position.name)
            put("location", kinds.location)
            put("camera", kinds.camera)
            put("microphone", kinds.microphone)
            put("locationColor", colors.location.serialize())
            put("cameraColor", colors.camera.serialize())
            put("microphoneColor", colors.microphone.serialize())
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
        CutoutColor.deserialize(obj.optString("locationColor"))?.let { setLocationColor(it) }
        CutoutColor.deserialize(obj.optString("cameraColor"))?.let { setCameraColor(it) }
        CutoutColor.deserialize(obj.optString("microphoneColor"))?.let { setMicrophoneColor(it) }
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
        val LOCATION_COLOR = stringPreferencesKey("permission_dot_location_color")
        val CAMERA_COLOR = stringPreferencesKey("permission_dot_camera_color")
        val MICROPHONE_COLOR = stringPreferencesKey("permission_dot_microphone_color")

        val DEFAULT_POSITION = PermissionDotPosition.RIGHT
    }
}
