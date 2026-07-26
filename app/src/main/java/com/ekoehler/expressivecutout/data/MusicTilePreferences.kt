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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.musicTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "music_tile_prefs")

/**
 * The look of one of the music tile's transport buttons. [color] is null to keep the button's
 * historical default (the skip buttons plain over the pill, the play/pause button the tile accent);
 * a non-null value fills the button with that colour. [opacity] (0f..1f) scales the fill and
 * [cornerPercent] (0..50) rounds its corners — 50 is a full circle, 0 a square. [filled] forces a
 * fill using the button's own default colour even when the user hasn't picked one, so a preset can
 * ask for a filled look without pinning it to a specific colour.
 */
data class MusicButtonStyle(
    val color: CutoutColor?,
    val opacity: Float,
    val cornerPercent: Int,
    val filled: Boolean = DEFAULT_FILLED,
) {
    companion object {
        const val DEFAULT_OPACITY = 1f
        const val DEFAULT_CORNER_PERCENT = 50
        const val MIN_CORNER_PERCENT = 0
        const val MAX_CORNER_PERCENT = 50
        const val DEFAULT_FILLED = false

        /** Corner rounding of the [ROUNDED] preset — a soft rounded rectangle rather than a pill. */
        const val ROUNDED_CORNER_PERCENT = 30

        val DEFAULT = MusicButtonStyle(
            color = null,
            opacity = DEFAULT_OPACITY,
            cornerPercent = DEFAULT_CORNER_PERCENT,
            filled = DEFAULT_FILLED,
        )

        /**
         * A filled, softly-rounded-rectangle button. [color] stays null so the fill uses the
         * user's chosen (or default) colour — the preset only fixes the shape and that it's filled.
         */
        val ROUNDED = MusicButtonStyle(
            color = null,
            opacity = DEFAULT_OPACITY,
            cornerPercent = ROUNDED_CORNER_PERCENT,
            filled = true,
        )

        /** A filled, fully-rounded pill (a circle on a square button). Colour follows the user's. */
        val PILL = MusicButtonStyle(
            color = null,
            opacity = DEFAULT_OPACITY,
            cornerPercent = MAX_CORNER_PERCENT,
            filled = true,
        )

        /** The selectable preset looks, in display order. */
        val PRESETS = listOf(ROUNDED, PILL)
    }
}

/** The music tile's own settings, edited on its dedicated settings screen. */
data class MusicTileSettings(
    val showAlbumArt: Boolean = DEFAULT_SHOW_ALBUM_ART,
    val rotateAlbumArt: Boolean = DEFAULT_ROTATE_ALBUM_ART,
    val showControls: Boolean = DEFAULT_SHOW_CONTROLS,
    /** Shared style of the previous / next (skip) buttons. */
    val skipButton: MusicButtonStyle = MusicButtonStyle.DEFAULT,
    /** Style of the central play / pause button. */
    val playPauseButton: MusicButtonStyle = MusicButtonStyle.DEFAULT,
) {
    companion object {
        const val DEFAULT_SHOW_ALBUM_ART = true
        const val DEFAULT_ROTATE_ALBUM_ART = false
        const val DEFAULT_SHOW_CONTROLS = true
    }
}

/** Persists the music tile's display options (album art, expanded controls) and button styling. */
class MusicTilePreferences(private val context: Context) {

    val settings: Flow<MusicTileSettings> = context.musicTileDataStore.data.map { prefs ->
        MusicTileSettings(
            showAlbumArt = prefs[SHOW_ALBUM_ART] ?: MusicTileSettings.DEFAULT_SHOW_ALBUM_ART,
            rotateAlbumArt = prefs[ROTATE_ALBUM_ART] ?: MusicTileSettings.DEFAULT_ROTATE_ALBUM_ART,
            showControls = prefs[SHOW_CONTROLS] ?: MusicTileSettings.DEFAULT_SHOW_CONTROLS,
            skipButton = MusicButtonStyle(
                color = CutoutColor.deserialize(prefs[SKIP_COLOR]),
                opacity = (prefs[SKIP_OPACITY] ?: MusicButtonStyle.DEFAULT_OPACITY).coerceIn(0f, 1f),
                cornerPercent = (prefs[SKIP_CORNER] ?: MusicButtonStyle.DEFAULT_CORNER_PERCENT)
                    .coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT),
                filled = prefs[SKIP_FILLED] ?: MusicButtonStyle.DEFAULT_FILLED,
            ),
            playPauseButton = MusicButtonStyle(
                color = CutoutColor.deserialize(prefs[PLAY_PAUSE_COLOR]),
                opacity = (prefs[PLAY_PAUSE_OPACITY] ?: MusicButtonStyle.DEFAULT_OPACITY).coerceIn(0f, 1f),
                cornerPercent = (prefs[PLAY_PAUSE_CORNER] ?: MusicButtonStyle.DEFAULT_CORNER_PERCENT)
                    .coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT),
                filled = prefs[PLAY_PAUSE_FILLED] ?: MusicButtonStyle.DEFAULT_FILLED,
            ),
        )
    }

    suspend fun setShowAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_ALBUM_ART] = enabled
    }

    suspend fun setRotateAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit {
        it[ROTATE_ALBUM_ART] = enabled
    }

    suspend fun setShowControls(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_CONTROLS] = enabled
    }

    /** A null [color] clears the override, restoring the skip buttons' plain default look. */
    suspend fun setSkipColor(color: CutoutColor?) = context.musicTileDataStore.edit {
        if (color == null) it.remove(SKIP_COLOR) else it[SKIP_COLOR] = color.serialize()
    }

    suspend fun setSkipOpacity(opacity: Float) = context.musicTileDataStore.edit {
        it[SKIP_OPACITY] = opacity.coerceIn(0f, 1f)
    }

    suspend fun setSkipCornerPercent(percent: Int) = context.musicTileDataStore.edit {
        it[SKIP_CORNER] = percent.coerceIn(
            MusicButtonStyle.MIN_CORNER_PERCENT,
            MusicButtonStyle.MAX_CORNER_PERCENT,
        )
    }

    suspend fun setSkipFilled(filled: Boolean) = context.musicTileDataStore.edit {
        it[SKIP_FILLED] = filled
    }

    /** A null [color] clears the override, restoring the play/pause button's accent default. */
    suspend fun setPlayPauseColor(color: CutoutColor?) = context.musicTileDataStore.edit {
        if (color == null) it.remove(PLAY_PAUSE_COLOR) else it[PLAY_PAUSE_COLOR] = color.serialize()
    }

    suspend fun setPlayPauseOpacity(opacity: Float) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_OPACITY] = opacity.coerceIn(0f, 1f)
    }

    suspend fun setPlayPauseCornerPercent(percent: Int) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_CORNER] = percent.coerceIn(
            MusicButtonStyle.MIN_CORNER_PERCENT,
            MusicButtonStyle.MAX_CORNER_PERCENT,
        )
    }

    suspend fun setPlayPauseFilled(filled: Boolean) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_FILLED] = filled
    }

    /** Applies a preset's shape and fill to the skip buttons, keeping their current colour. */
    suspend fun applySkipPreset(preset: MusicButtonStyle) = context.musicTileDataStore.edit {
        it[SKIP_OPACITY] = preset.opacity.coerceIn(0f, 1f)
        it[SKIP_CORNER] = preset.cornerPercent.coerceIn(
            MusicButtonStyle.MIN_CORNER_PERCENT,
            MusicButtonStyle.MAX_CORNER_PERCENT,
        )
        it[SKIP_FILLED] = preset.filled
    }

    /** Applies a preset's shape and fill to the play/pause button, keeping its current colour. */
    suspend fun applyPlayPausePreset(preset: MusicButtonStyle) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_OPACITY] = preset.opacity.coerceIn(0f, 1f)
        it[PLAY_PAUSE_CORNER] = preset.cornerPercent.coerceIn(
            MusicButtonStyle.MIN_CORNER_PERCENT,
            MusicButtonStyle.MAX_CORNER_PERCENT,
        )
        it[PLAY_PAUSE_FILLED] = preset.filled
    }

    private companion object {
        val SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")
        val ROTATE_ALBUM_ART = booleanPreferencesKey("rotate_album_art")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls")
        val SKIP_COLOR = stringPreferencesKey("skip_button_color")
        val SKIP_OPACITY = floatPreferencesKey("skip_button_opacity")
        val SKIP_CORNER = intPreferencesKey("skip_button_corner_percent")
        val SKIP_FILLED = booleanPreferencesKey("skip_button_filled")
        val PLAY_PAUSE_COLOR = stringPreferencesKey("play_pause_button_color")
        val PLAY_PAUSE_OPACITY = floatPreferencesKey("play_pause_button_opacity")
        val PLAY_PAUSE_CORNER = intPreferencesKey("play_pause_button_corner_percent")
        val PLAY_PAUSE_FILLED = booleanPreferencesKey("play_pause_button_filled")
    }
}
