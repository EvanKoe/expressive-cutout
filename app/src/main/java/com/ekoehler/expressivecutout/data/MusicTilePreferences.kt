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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_DEFAULT_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import org.json.JSONObject

/** Backing store for the music tile's settings. */
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

/** The transport action carried by the button on the trailing edge of the music normal cutout. */
enum class MusicRightButtonAction { PREVIOUS, PLAY_PAUSE, NEXT }

/** The music tile's own settings, edited on its dedicated settings screen. */
data class MusicTileSettings(
    val showAlbumArt: Boolean = DEFAULT_SHOW_ALBUM_ART,
    val showAlbumBackground: Boolean = DEFAULT_SHOW_ALBUM_BACKGROUND,
    val rotateAlbumArt: Boolean = DEFAULT_ROTATE_ALBUM_ART,
    /**
     * Crop the album cover to a full circle rather than a rounded square. Forced on while
     * [rotateAlbumArt] is set — a spinning square would swing its corners.
     */
    val circleCover: Boolean = DEFAULT_CIRCLE_COVER,
    /** Draw a ring around the album cover, separated from it by a small gap. */
    val albumArtStroke: Boolean = DEFAULT_ALBUM_ART_STROKE,
    /** Colour of that ring; null keeps the tile's own pink accent. */
    val albumArtStrokeColor: CutoutColor? = null,
    /**
     * Container colour of the note glyph standing in for a missing cover; null keeps the tile's
     * own faint accent-tinted disc.
     */
    val coverFallbackColor: CutoutColor? = null,
    /** Automatically expand the cutout when playback starts, rather than only opening the normal cutout. */
    val expandOnPlay: Boolean = DEFAULT_EXPAND_ON_PLAY,
    /** Keep the music cutout visible even while the app playing the music is in the foreground. */
    val visibleInPlayerApp: Boolean = DEFAULT_VISIBLE_IN_PLAYER_APP,
    val showControls: Boolean = DEFAULT_SHOW_CONTROLS,
    /** Shared style of the previous / next (skip) buttons. */
    val skipButton: MusicButtonStyle = MusicButtonStyle.DEFAULT,
    /** Let the previous button take the row's leftover width instead of its fixed square size. */
    val previousExpand: Boolean = DEFAULT_PREVIOUS_EXPAND,
    /** On an expanded previous button, label it "Previous" rather than drawing the icon. */
    val previousText: Boolean = DEFAULT_PREVIOUS_TEXT,
    /** Roboto Flex `wdth` axis of the previous button's label. */
    val previousTextWidth: Float = DEFAULT_TEXT_WIDTH,
    /** Let the next button take the row's leftover width instead of its fixed square size. */
    val nextExpand: Boolean = DEFAULT_NEXT_EXPAND,
    /** On an expanded next button, label it "Next" rather than drawing the icon. */
    val nextText: Boolean = DEFAULT_NEXT_TEXT,
    /** Roboto Flex `wdth` axis of the next button's label. */
    val nextTextWidth: Float = DEFAULT_TEXT_WIDTH,
    /** Style of the central play / pause button. */
    val playPauseButton: MusicButtonStyle = MusicButtonStyle.DEFAULT,
    /** Let the play/pause button take the row's leftover width instead of its fixed 16:9 size. */
    val playPauseExpand: Boolean = DEFAULT_PLAY_PAUSE_EXPAND,
    /** On an expanded play/pause button, label it "Play" / "Pause" rather than drawing the icon. */
    val playPauseText: Boolean = DEFAULT_PLAY_PAUSE_TEXT,
    /** Roboto Flex `wdth` axis of the play/pause button's label. */
    val playPauseTextWidth: Float = DEFAULT_TEXT_WIDTH,
    /** Show a playback progress bar under the transport controls. */
    val showProgress: Boolean = DEFAULT_SHOW_PROGRESS,
    /**
     * Replace the music tile's normal cutout with the tiny cutout: a small pill carrying only the
     * note glyph and the album cover. Tapping it still opens the expanded cutout.
     */
    val miniPlayer: Boolean = DEFAULT_MINI_PLAYER,
    /**
     * Draw a single transport button on the trailing edge of the music tile's normal cutout. Only
     * offered while [miniPlayer] is off — the tiny pill has no room beside the camera for it.
     */
    val rightButton: Boolean = DEFAULT_RIGHT_BUTTON,
    /** Which transport action that button carries. */
    val rightButtonAction: MusicRightButtonAction = DEFAULT_RIGHT_BUTTON_ACTION,
) {
    companion object {
        const val DEFAULT_SHOW_ALBUM_ART = true
        const val DEFAULT_SHOW_ALBUM_BACKGROUND = false
        const val DEFAULT_ROTATE_ALBUM_ART = false
        const val DEFAULT_CIRCLE_COVER = false
        const val DEFAULT_ALBUM_ART_STROKE = false
        const val DEFAULT_EXPAND_ON_PLAY = true
        const val DEFAULT_VISIBLE_IN_PLAYER_APP = true
        const val DEFAULT_SHOW_CONTROLS = true
        const val DEFAULT_SHOW_PROGRESS = false
        const val DEFAULT_MINI_PLAYER = false
        const val DEFAULT_RIGHT_BUTTON = false
        val DEFAULT_RIGHT_BUTTON_ACTION = MusicRightButtonAction.PLAY_PAUSE
        const val DEFAULT_PLAY_PAUSE_EXPAND = false
        const val DEFAULT_PLAY_PAUSE_TEXT = false
        const val DEFAULT_PREVIOUS_EXPAND = false
        const val DEFAULT_PREVIOUS_TEXT = false
        const val DEFAULT_NEXT_EXPAND = false
        const val DEFAULT_NEXT_TEXT = false

        /** Label width defaults to Roboto Flex's normal `wdth`; the sliders offer the whole axis. */
        const val DEFAULT_TEXT_WIDTH = ROBOTO_FLEX_DEFAULT_WIDTH
        const val MIN_TEXT_WIDTH = ROBOTO_FLEX_MIN_WIDTH
        const val MAX_TEXT_WIDTH = ROBOTO_FLEX_MAX_WIDTH
    }
}

/** Persists the music tile's display options (album art, expanded controls) and button styling. */
class MusicTilePreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<MusicTileSettings> = context.musicTileDataStore.data.map { prefs ->
        MusicTileSettings(
            showAlbumArt = prefs[SHOW_ALBUM_ART] ?: MusicTileSettings.DEFAULT_SHOW_ALBUM_ART,
            showAlbumBackground = prefs[SHOW_ALBUM_BACKGROUND] ?: MusicTileSettings.DEFAULT_SHOW_ALBUM_BACKGROUND,
            rotateAlbumArt = prefs[ROTATE_ALBUM_ART] ?: MusicTileSettings.DEFAULT_ROTATE_ALBUM_ART,
            circleCover = prefs[CIRCLE_COVER] ?: MusicTileSettings.DEFAULT_CIRCLE_COVER,
            albumArtStroke = prefs[ALBUM_ART_STROKE] ?: MusicTileSettings.DEFAULT_ALBUM_ART_STROKE,
            albumArtStrokeColor = CutoutColor.deserialize(prefs[ALBUM_ART_STROKE_COLOR]),
            coverFallbackColor = CutoutColor.deserialize(prefs[COVER_FALLBACK_COLOR]),
            expandOnPlay = prefs[EXPAND_ON_PLAY] ?: MusicTileSettings.DEFAULT_EXPAND_ON_PLAY,
            visibleInPlayerApp = prefs[VISIBLE_IN_PLAYER_APP]
                ?: MusicTileSettings.DEFAULT_VISIBLE_IN_PLAYER_APP,
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
            showProgress = prefs[SHOW_PROGRESS] ?: MusicTileSettings.DEFAULT_SHOW_PROGRESS,
            miniPlayer = prefs[MINI_PLAYER] ?: MusicTileSettings.DEFAULT_MINI_PLAYER,
            rightButton = prefs[RIGHT_BUTTON] ?: MusicTileSettings.DEFAULT_RIGHT_BUTTON,
            rightButtonAction = prefs[RIGHT_BUTTON_ACTION]
                ?.let { runCatching { MusicRightButtonAction.valueOf(it) }.getOrNull() }
                ?: MusicTileSettings.DEFAULT_RIGHT_BUTTON_ACTION,
            playPauseExpand = prefs[PLAY_PAUSE_EXPAND] ?: MusicTileSettings.DEFAULT_PLAY_PAUSE_EXPAND,
            playPauseText = prefs[PLAY_PAUSE_TEXT] ?: MusicTileSettings.DEFAULT_PLAY_PAUSE_TEXT,
            playPauseTextWidth = (prefs[PLAY_PAUSE_TEXT_WIDTH] ?: MusicTileSettings.DEFAULT_TEXT_WIDTH)
                .coerceIn(MusicTileSettings.MIN_TEXT_WIDTH, MusicTileSettings.MAX_TEXT_WIDTH),
            previousExpand = prefs[PREVIOUS_EXPAND] ?: MusicTileSettings.DEFAULT_PREVIOUS_EXPAND,
            previousText = prefs[PREVIOUS_TEXT] ?: MusicTileSettings.DEFAULT_PREVIOUS_TEXT,
            previousTextWidth = (prefs[PREVIOUS_TEXT_WIDTH] ?: MusicTileSettings.DEFAULT_TEXT_WIDTH)
                .coerceIn(MusicTileSettings.MIN_TEXT_WIDTH, MusicTileSettings.MAX_TEXT_WIDTH),
            nextExpand = prefs[NEXT_EXPAND] ?: MusicTileSettings.DEFAULT_NEXT_EXPAND,
            nextText = prefs[NEXT_TEXT] ?: MusicTileSettings.DEFAULT_NEXT_TEXT,
            nextTextWidth = (prefs[NEXT_TEXT_WIDTH] ?: MusicTileSettings.DEFAULT_TEXT_WIDTH)
                .coerceIn(MusicTileSettings.MIN_TEXT_WIDTH, MusicTileSettings.MAX_TEXT_WIDTH),
        )
    }

    /** Exports the current [MusicTileSettings] (including both button styles) as a JSON string. */
    override suspend fun toJson(): String {
        fun MusicButtonStyle.toJsonObject(): JSONObject = JSONObject().apply {
            put("color", color?.serialize() ?: JSONObject.NULL)
            put("opacity", opacity.toDouble())
            put("cornerPercent", cornerPercent)
            put("filled", filled)
        }

        val s = settings.first()
        return JSONObject().apply {
            put("showAlbumArt", s.showAlbumArt)
            put("showAlbumBackground", s.showAlbumBackground)
            put("rotateAlbumArt", s.rotateAlbumArt)
            put("circleCover", s.circleCover)
            put("albumArtStroke", s.albumArtStroke)
            put("albumArtStrokeColor", s.albumArtStrokeColor?.serialize() ?: JSONObject.NULL)
            put("coverFallbackColor", s.coverFallbackColor?.serialize() ?: JSONObject.NULL)
            put("expandOnPlay", s.expandOnPlay)
            put("visibleInPlayerApp", s.visibleInPlayerApp)
            put("showControls", s.showControls)
            put("showProgress", s.showProgress)
            put("miniPlayer", s.miniPlayer)
            put("rightButton", s.rightButton)
            put("rightButtonAction", s.rightButtonAction.name)
            put("playPauseExpand", s.playPauseExpand)
            put("playPauseText", s.playPauseText)
            put("playPauseTextWidth", s.playPauseTextWidth.toDouble())
            put("previousExpand", s.previousExpand)
            put("previousText", s.previousText)
            put("previousTextWidth", s.previousTextWidth.toDouble())
            put("nextExpand", s.nextExpand)
            put("nextText", s.nextText)
            put("nextTextWidth", s.nextTextWidth.toDouble())
            put("skipButton", s.skipButton.toJsonObject())
            put("playPauseButton", s.playPauseButton.toJsonObject())
        }.toString()
    }

    /**
     * Applies the [MusicTileSettings] object exported by [toJson], including both nested button
     * styles (skip / play-pause). Absent fields are left as-is; a null colour clears its override.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.musicTileDataStore.edit { prefs ->
            if (obj.has("showAlbumArt")) prefs[SHOW_ALBUM_ART] = obj.getBoolean("showAlbumArt")
            if (obj.has("showAlbumBackground")) prefs[SHOW_ALBUM_BACKGROUND] = obj.getBoolean("showAlbumBackground")
            if (obj.has("rotateAlbumArt")) prefs[ROTATE_ALBUM_ART] = obj.getBoolean("rotateAlbumArt")
            if (obj.has("circleCover")) prefs[CIRCLE_COVER] = obj.getBoolean("circleCover")
            if (obj.has("albumArtStroke")) prefs[ALBUM_ART_STROKE] = obj.getBoolean("albumArtStroke")
            if (obj.has("albumArtStrokeColor")) {
                val raw = if (obj.isNull("albumArtStrokeColor")) null else obj.optString("albumArtStrokeColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) prefs.remove(ALBUM_ART_STROKE_COLOR) else prefs[ALBUM_ART_STROKE_COLOR] = color.serialize()
            }
            if (obj.has("coverFallbackColor")) {
                val raw = if (obj.isNull("coverFallbackColor")) null else obj.optString("coverFallbackColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) prefs.remove(COVER_FALLBACK_COLOR) else prefs[COVER_FALLBACK_COLOR] = color.serialize()
            }
            if (obj.has("expandOnPlay")) prefs[EXPAND_ON_PLAY] = obj.getBoolean("expandOnPlay")
            if (obj.has("visibleInPlayerApp")) prefs[VISIBLE_IN_PLAYER_APP] = obj.getBoolean("visibleInPlayerApp")
            if (obj.has("showControls")) prefs[SHOW_CONTROLS] = obj.getBoolean("showControls")
            if (obj.has("showProgress")) prefs[SHOW_PROGRESS] = obj.getBoolean("showProgress")
            if (obj.has("miniPlayer")) prefs[MINI_PLAYER] = obj.getBoolean("miniPlayer")
            if (obj.has("rightButton")) prefs[RIGHT_BUTTON] = obj.getBoolean("rightButton")
            runCatching { MusicRightButtonAction.valueOf(obj.optString("rightButtonAction")) }
                .getOrNull()
                ?.let { prefs[RIGHT_BUTTON_ACTION] = it.name }
            if (obj.has("playPauseExpand")) prefs[PLAY_PAUSE_EXPAND] = obj.getBoolean("playPauseExpand")
            if (obj.has("playPauseText")) prefs[PLAY_PAUSE_TEXT] = obj.getBoolean("playPauseText")
            if (obj.has("playPauseTextWidth")) prefs[PLAY_PAUSE_TEXT_WIDTH] = obj.textWidth("playPauseTextWidth")
            if (obj.has("previousExpand")) prefs[PREVIOUS_EXPAND] = obj.getBoolean("previousExpand")
            if (obj.has("previousText")) prefs[PREVIOUS_TEXT] = obj.getBoolean("previousText")
            if (obj.has("previousTextWidth")) prefs[PREVIOUS_TEXT_WIDTH] = obj.textWidth("previousTextWidth")
            if (obj.has("nextExpand")) prefs[NEXT_EXPAND] = obj.getBoolean("nextExpand")
            if (obj.has("nextText")) prefs[NEXT_TEXT] = obj.getBoolean("nextText")
            if (obj.has("nextTextWidth")) prefs[NEXT_TEXT_WIDTH] = obj.textWidth("nextTextWidth")

            obj.optJSONObject("skipButton")?.applyButton(prefs, SKIP_COLOR, SKIP_OPACITY, SKIP_CORNER, SKIP_FILLED)
            obj.optJSONObject("playPauseButton")
                ?.applyButton(prefs, PLAY_PAUSE_COLOR, PLAY_PAUSE_OPACITY, PLAY_PAUSE_CORNER, PLAY_PAUSE_FILLED)
        }
    }

    /** Reads a label-width axis value, clamped to the range the slider offers. */
    private fun JSONObject.textWidth(name: String): Float = optDouble(name).toFloat()
        .coerceIn(MusicTileSettings.MIN_TEXT_WIDTH, MusicTileSettings.MAX_TEXT_WIDTH)

    /** Writes one [MusicButtonStyle] object into the given transport button's keys. */
    private fun JSONObject.applyButton(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        colorKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        opacityKey: androidx.datastore.preferences.core.Preferences.Key<Float>,
        cornerKey: androidx.datastore.preferences.core.Preferences.Key<Int>,
        filledKey: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
    ) {
        if (has("color")) {
            val raw = if (isNull("color")) null else optString("color")
            val color = CutoutColor.deserialize(raw)
            if (color == null) prefs.remove(colorKey) else prefs[colorKey] = color.serialize()
        }
        if (has("opacity")) prefs[opacityKey] = optDouble("opacity").toFloat().coerceIn(0f, 1f)
        if (has("cornerPercent")) prefs[cornerKey] = getInt("cornerPercent")
            .coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT)
        if (has("filled")) prefs[filledKey] = getBoolean("filled")
    }

    suspend fun setShowAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_ALBUM_ART] = enabled
    }

    suspend fun setShowAlbumBackground(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_ALBUM_BACKGROUND] = enabled
    }

    suspend fun setRotateAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit {
        it[ROTATE_ALBUM_ART] = enabled
    }

    suspend fun setCircleCover(enabled: Boolean) = context.musicTileDataStore.edit {
        it[CIRCLE_COVER] = enabled
    }

    suspend fun setAlbumArtStroke(enabled: Boolean) = context.musicTileDataStore.edit {
        it[ALBUM_ART_STROKE] = enabled
    }

    /** A null [color] clears the override, restoring the ring's tile-accent default. */
    suspend fun setAlbumArtStrokeColor(color: CutoutColor?) = context.musicTileDataStore.edit {
        if (color == null) {
            it.remove(ALBUM_ART_STROKE_COLOR)
        } else {
            it[ALBUM_ART_STROKE_COLOR] = color.serialize()
        }
    }

    /** A null [color] clears the override, restoring the glyph's faint accent-tinted disc. */
    suspend fun setCoverFallbackColor(color: CutoutColor?) = context.musicTileDataStore.edit {
        if (color == null) {
            it.remove(COVER_FALLBACK_COLOR)
        } else {
            it[COVER_FALLBACK_COLOR] = color.serialize()
        }
    }

    suspend fun setExpandOnPlay(enabled: Boolean) = context.musicTileDataStore.edit {
        it[EXPAND_ON_PLAY] = enabled
    }

    suspend fun setVisibleInPlayerApp(enabled: Boolean) = context.musicTileDataStore.edit {
        it[VISIBLE_IN_PLAYER_APP] = enabled
    }

    suspend fun setShowControls(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_CONTROLS] = enabled
    }

    /** A null [color] clears the override, restoring the skip buttons' plain default look. */
    suspend fun setSkipColor(color: CutoutColor?) = context.musicTileDataStore.edit {
        if (color == null) it.remove(SKIP_COLOR) else it[SKIP_COLOR] = color.serialize()
    }

    /** Clamps to 0f..1f, the range the opacity slider offers. */
    suspend fun setSkipOpacity(opacity: Float) = context.musicTileDataStore.edit {
        it[SKIP_OPACITY] = opacity.coerceIn(0f, 1f)
    }

    /**
     * Clamps to the range the corner slider offers, so an imported settings file can't leave a
     * shape the UI has no way to correct.
     */
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

    /** Clamps to 0f..1f, the range the opacity slider offers. */
    suspend fun setPlayPauseOpacity(opacity: Float) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_OPACITY] = opacity.coerceIn(0f, 1f)
    }

    suspend fun setShowProgress(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_PROGRESS] = enabled
    }

    suspend fun setMiniPlayer(enabled: Boolean) = context.musicTileDataStore.edit {
        it[MINI_PLAYER] = enabled
    }

    suspend fun setRightButton(enabled: Boolean) = context.musicTileDataStore.edit {
        it[RIGHT_BUTTON] = enabled
    }

    suspend fun setRightButtonAction(action: MusicRightButtonAction) = context.musicTileDataStore.edit {
        it[RIGHT_BUTTON_ACTION] = action.name
    }

    /**
     * Clamps to the range the corner slider offers, so an imported settings file can't leave a
     * shape the UI has no way to correct.
     */
    suspend fun setPlayPauseCornerPercent(percent: Int) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_CORNER] = percent.coerceIn(
            MusicButtonStyle.MIN_CORNER_PERCENT,
            MusicButtonStyle.MAX_CORNER_PERCENT,
        )
    }

    suspend fun setPreviousExpand(enabled: Boolean) = context.musicTileDataStore.edit {
        it[PREVIOUS_EXPAND] = enabled
    }

    suspend fun setPreviousText(enabled: Boolean) = context.musicTileDataStore.edit {
        it[PREVIOUS_TEXT] = enabled
    }

    /** Clamps to the Roboto Flex `wdth` axis range the slider offers. */
    suspend fun setPreviousTextWidth(width: Float) = context.musicTileDataStore.edit {
        it[PREVIOUS_TEXT_WIDTH] = width.coerceIn(
            MusicTileSettings.MIN_TEXT_WIDTH,
            MusicTileSettings.MAX_TEXT_WIDTH,
        )
    }

    suspend fun setNextExpand(enabled: Boolean) = context.musicTileDataStore.edit {
        it[NEXT_EXPAND] = enabled
    }

    suspend fun setNextText(enabled: Boolean) = context.musicTileDataStore.edit {
        it[NEXT_TEXT] = enabled
    }

    /** Clamps to the Roboto Flex `wdth` axis range the slider offers. */
    suspend fun setNextTextWidth(width: Float) = context.musicTileDataStore.edit {
        it[NEXT_TEXT_WIDTH] = width.coerceIn(
            MusicTileSettings.MIN_TEXT_WIDTH,
            MusicTileSettings.MAX_TEXT_WIDTH,
        )
    }

    suspend fun setPlayPauseExpand(enabled: Boolean) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_EXPAND] = enabled
    }

    suspend fun setPlayPauseText(enabled: Boolean) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_TEXT] = enabled
    }

    /** Clamps to the Roboto Flex `wdth` axis range the slider offers. */
    suspend fun setPlayPauseTextWidth(width: Float) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_TEXT_WIDTH] = width.coerceIn(
            MusicTileSettings.MIN_TEXT_WIDTH,
            MusicTileSettings.MAX_TEXT_WIDTH,
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
        val SHOW_ALBUM_BACKGROUND = booleanPreferencesKey("show_album_background")
        val ROTATE_ALBUM_ART = booleanPreferencesKey("rotate_album_art")
        val CIRCLE_COVER = booleanPreferencesKey("circle_cover")
        val ALBUM_ART_STROKE = booleanPreferencesKey("album_art_stroke")
        val ALBUM_ART_STROKE_COLOR = stringPreferencesKey("album_art_stroke_color")
        val COVER_FALLBACK_COLOR = stringPreferencesKey("cover_fallback_color")
        val EXPAND_ON_PLAY = booleanPreferencesKey("expand_on_play")
        val VISIBLE_IN_PLAYER_APP = booleanPreferencesKey("visible_in_player_app")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls")
        val SKIP_COLOR = stringPreferencesKey("skip_button_color")
        val SKIP_OPACITY = floatPreferencesKey("skip_button_opacity")
        val SKIP_CORNER = intPreferencesKey("skip_button_corner_percent")
        val SKIP_FILLED = booleanPreferencesKey("skip_button_filled")
        val PLAY_PAUSE_COLOR = stringPreferencesKey("play_pause_button_color")
        val PLAY_PAUSE_OPACITY = floatPreferencesKey("play_pause_button_opacity")
        val PLAY_PAUSE_CORNER = intPreferencesKey("play_pause_button_corner_percent")
        val PLAY_PAUSE_FILLED = booleanPreferencesKey("play_pause_button_filled")
        val SHOW_PROGRESS = booleanPreferencesKey("show_current_progress")
        val MINI_PLAYER = booleanPreferencesKey("mini_player")
        val RIGHT_BUTTON = booleanPreferencesKey("right_button")
        val RIGHT_BUTTON_ACTION = stringPreferencesKey("right_button_action")
        val PLAY_PAUSE_EXPAND = booleanPreferencesKey("play_pause_button_expand")
        val PLAY_PAUSE_TEXT = booleanPreferencesKey("play_pause_button_text")
        val PREVIOUS_EXPAND = booleanPreferencesKey("previous_button_expand")
        val PREVIOUS_TEXT = booleanPreferencesKey("previous_button_text")
        val NEXT_EXPAND = booleanPreferencesKey("next_button_expand")
        val NEXT_TEXT = booleanPreferencesKey("next_button_text")
        val PLAY_PAUSE_TEXT_WIDTH = floatPreferencesKey("play_pause_button_text_width")
        val PREVIOUS_TEXT_WIDTH = floatPreferencesKey("previous_button_text_width")
        val NEXT_TEXT_WIDTH = floatPreferencesKey("next_button_text_width")
    }
}
