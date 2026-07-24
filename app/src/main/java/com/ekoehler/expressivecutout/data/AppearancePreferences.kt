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
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_prefs")

/**
 * A user-selectable colour for the island. Either a fixed ARGB value or [Dynamic], which resolves
 * to the system's Material You accent at render time (so it follows the wallpaper on Android 12+).
 * Stored as a short string so it can live in a single preference key.
 */
sealed interface CutoutColor {
    data object Dynamic : CutoutColor
    data class Solid(val argb: Long) : CutoutColor

    fun serialize(): String = when (this) {
        Dynamic -> DYNAMIC
        is Solid -> argb.toString()
    }

    companion object {
        private const val DYNAMIC = "dynamic"

        fun deserialize(value: String?): CutoutColor? = when {
            value == null -> null
            value == DYNAMIC -> Dynamic
            else -> value.toLongOrNull()?.let(::Solid)
        }
    }
}

/**
 * Visual styling of the island that is independent of its geometry: whether it casts a shadow,
 * an optional outline stroke (width + colour), and the fill colour. Colours may be [CutoutColor.Dynamic].
 */
data class AppearanceSettings(
    val shadowEnabled: Boolean = DEFAULT_SHADOW_ENABLED,
    val strokeEnabled: Boolean = DEFAULT_STROKE_ENABLED,
    val strokeWidthDp: Int = DEFAULT_STROKE_WIDTH_DP,
    val strokeColor: CutoutColor = DEFAULT_STROKE_COLOR,
    val backgroundColor: CutoutColor = DEFAULT_BACKGROUND_COLOR,
    val sendButtonColor: CutoutColor? = DEFAULT_SEND_BUTTON_COLOR,
    val cancelButtonColor: CutoutColor? = DEFAULT_CANCEL_BUTTON_COLOR,
) {
    companion object {
        const val DEFAULT_SHADOW_ENABLED = true
        const val DEFAULT_STROKE_ENABLED = false
        const val DEFAULT_STROKE_WIDTH_DP = 2
        const val MIN_STROKE_WIDTH_DP = 1
        const val MAX_STROKE_WIDTH_DP = 8

        // Match the pill's historical look: near-black fill, white stroke.
        val DEFAULT_BACKGROUND_COLOR: CutoutColor = CutoutColor.Solid(0xFF0A0A0A)
        val DEFAULT_STROKE_COLOR: CutoutColor = CutoutColor.Solid(0xFFFFFFFF)

        // null keeps the historical reply-button look: the send button matches the
        // notification's own accent and the cancel button stays a neutral tint.
        val DEFAULT_SEND_BUTTON_COLOR: CutoutColor? = null
        val DEFAULT_CANCEL_BUTTON_COLOR: CutoutColor? = null
    }
}

/** Persists [AppearanceSettings], always emitting a clamped stroke width. */
class AppearancePreferences(private val context: Context) {

    val settings: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        AppearanceSettings(
            shadowEnabled = prefs[SHADOW_ENABLED] ?: AppearanceSettings.DEFAULT_SHADOW_ENABLED,
            strokeEnabled = prefs[STROKE_ENABLED] ?: AppearanceSettings.DEFAULT_STROKE_ENABLED,
            strokeWidthDp = (prefs[STROKE_WIDTH] ?: AppearanceSettings.DEFAULT_STROKE_WIDTH_DP)
                .coerceIn(AppearanceSettings.MIN_STROKE_WIDTH_DP, AppearanceSettings.MAX_STROKE_WIDTH_DP),
            strokeColor = CutoutColor.deserialize(prefs[STROKE_COLOR]) ?: AppearanceSettings.DEFAULT_STROKE_COLOR,
            backgroundColor = CutoutColor.deserialize(prefs[BACKGROUND_COLOR]) ?: AppearanceSettings.DEFAULT_BACKGROUND_COLOR,
            sendButtonColor = CutoutColor.deserialize(prefs[SEND_BUTTON_COLOR]),
            cancelButtonColor = CutoutColor.deserialize(prefs[CANCEL_BUTTON_COLOR]),
        )
    }

    suspend fun setShadowEnabled(enabled: Boolean) = context.appearanceDataStore.edit {
        it[SHADOW_ENABLED] = enabled
    }

    suspend fun setStrokeEnabled(enabled: Boolean) = context.appearanceDataStore.edit {
        it[STROKE_ENABLED] = enabled
    }

    suspend fun setStrokeWidth(widthDp: Int) = context.appearanceDataStore.edit {
        it[STROKE_WIDTH] = widthDp.coerceIn(
            AppearanceSettings.MIN_STROKE_WIDTH_DP,
            AppearanceSettings.MAX_STROKE_WIDTH_DP,
        )
    }

    suspend fun setStrokeColor(color: CutoutColor) = context.appearanceDataStore.edit {
        it[STROKE_COLOR] = color.serialize()
    }

    suspend fun setBackgroundColor(color: CutoutColor) = context.appearanceDataStore.edit {
        it[BACKGROUND_COLOR] = color.serialize()
    }

    /** A null [color] clears the override, restoring the accent-following default. */
    suspend fun setSendButtonColor(color: CutoutColor?) = context.appearanceDataStore.edit {
        if (color == null) it.remove(SEND_BUTTON_COLOR) else it[SEND_BUTTON_COLOR] = color.serialize()
    }

    /** A null [color] clears the override, restoring the neutral default. */
    suspend fun setCancelButtonColor(color: CutoutColor?) = context.appearanceDataStore.edit {
        if (color == null) it.remove(CANCEL_BUTTON_COLOR) else it[CANCEL_BUTTON_COLOR] = color.serialize()
    }

    private companion object {
        val SHADOW_ENABLED = booleanPreferencesKey("shadow_enabled")
        val STROKE_ENABLED = booleanPreferencesKey("stroke_enabled")
        val STROKE_WIDTH = intPreferencesKey("stroke_width_dp")
        val STROKE_COLOR = stringPreferencesKey("stroke_color")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val SEND_BUTTON_COLOR = stringPreferencesKey("send_button_color")
        val CANCEL_BUTTON_COLOR = stringPreferencesKey("cancel_button_color")
    }
}
