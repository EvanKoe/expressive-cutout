package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Backing store for the assistant tile's settings. */
private val Context.assistantTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_tile_prefs")

/** The assistant tile's settings, edited on its dedicated settings screen. */
data class AssistantTileSettings(
    /** Whether to display the text response of the assistant in the cutout. */
    val displayAnswerInCutout: Boolean = DEFAULT_DISPLAY_ANSWER_IN_CUTOUT,
    /** Maximum cutout height in percent of the screen height (10..80). */
    val maxCutoutHeightPercent: Int = DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT,
    /** Colour of the icon container (the disc behind the assistant glyph). Null = default. */
    val iconContainerColor: CutoutColor? = null,
    /** Whether to use the animated sparkles Lottie icon instead of the static glyph. */
    val useAnimatedIcon: Boolean = DEFAULT_USE_ANIMATED_ICON,
) {
    companion object {
        const val DEFAULT_DISPLAY_ANSWER_IN_CUTOUT = true
        const val DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT = 35
        const val DEFAULT_USE_ANIMATED_ICON = false
    }
}

/** Persists the assistant tile's options (answer display, max cutout height, container colour). */
class AssistantTilePreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<AssistantTileSettings> = context.assistantTileDataStore.data.map { prefs ->
        AssistantTileSettings(
            displayAnswerInCutout = prefs[DISPLAY_ANSWER_IN_CUTOUT] ?: AssistantTileSettings.DEFAULT_DISPLAY_ANSWER_IN_CUTOUT,
            maxCutoutHeightPercent = prefs[MAX_CUTOUT_HEIGHT_PERCENT] ?: AssistantTileSettings.DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT,
            iconContainerColor = CutoutColor.deserialize(prefs[ICON_CONTAINER_COLOR]),
            useAnimatedIcon = prefs[USE_ANIMATED_ICON] ?: AssistantTileSettings.DEFAULT_USE_ANIMATED_ICON,
        )
    }

    /** Exports the current [AssistantTileSettings] as a JSON string. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("displayAnswerInCutout", s.displayAnswerInCutout)
            put("maxCutoutHeightPercent", s.maxCutoutHeightPercent)
            put("iconContainerColor", s.iconContainerColor?.serialize() ?: JSONObject.NULL)
            put("useAnimatedIcon", s.useAnimatedIcon)
        }.toString()
    }

    /** Applies the [AssistantTileSettings] object exported by [toJson]; absent fields are left as-is. */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.assistantTileDataStore.edit {
            if (obj.has("displayAnswerInCutout")) it[DISPLAY_ANSWER_IN_CUTOUT] = obj.getBoolean("displayAnswerInCutout")
            if (obj.has("maxCutoutHeightPercent")) it[MAX_CUTOUT_HEIGHT_PERCENT] = obj.getInt("maxCutoutHeightPercent").coerceIn(10, 80)
            if (obj.has("useAnimatedIcon")) it[USE_ANIMATED_ICON] = obj.getBoolean("useAnimatedIcon")
            if (obj.has("iconContainerColor")) {
                val raw = if (obj.isNull("iconContainerColor")) null else obj.optString("iconContainerColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
            }
        }
    }

    suspend fun setDisplayAnswerInCutout(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[DISPLAY_ANSWER_IN_CUTOUT] = enabled
    }

    /**
     * Clamps to 10..80 percent: below that the tile has no room to draw, above it the island would
     * swallow most of the screen.
     */
    suspend fun setMaxCutoutHeightPercent(percent: Int) = context.assistantTileDataStore.edit {
        it[MAX_CUTOUT_HEIGHT_PERCENT] = percent.coerceIn(10, 80)
    }

    /**
     * Stores the icon container colour, or removes the key entirely for null so the tile falls back
     * to the theme default.
     */
    suspend fun setIconContainerColor(color: CutoutColor?) = context.assistantTileDataStore.edit {
        if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
    }

    suspend fun setUseAnimatedIcon(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[USE_ANIMATED_ICON] = enabled
    }

    private companion object {
        val DISPLAY_ANSWER_IN_CUTOUT = booleanPreferencesKey("display_answer_in_cutout")
        val MAX_CUTOUT_HEIGHT_PERCENT = intPreferencesKey("max_cutout_height_percent")
        val ICON_CONTAINER_COLOR = stringPreferencesKey("icon_container_color")
        val USE_ANIMATED_ICON = booleanPreferencesKey("use_animated_icon")
    }
}
