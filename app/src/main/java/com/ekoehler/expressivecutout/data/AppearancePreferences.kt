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
 * Visual treatment of the expanded island's action chips. Stored by [name] so it fits a single
 * preference key; a mix of Material 3 Expressive and Material You looks.
 */
enum class ActionButtonStyle {
    /** Material 3 Expressive: a translucent accent pill (the original look). */
    EXPRESSIVE_TONAL,
    /** Material 3 Expressive: a solid, fully-filled accent pill. */
    EXPRESSIVE_FILLED,
    /** Material You: a softly-rounded tonal container. */
    MATERIAL_YOU,
    /** An outlined chip over a transparent fill. */
    OUTLINED,
    ;

    companion object {
        fun deserialize(value: String?): ActionButtonStyle? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}

/** Visual treatment of the inline reply text field. */
enum class ReplyInputStyle {
    /** Material 3 Expressive: a fully-rounded (pill) field. */
    EXPRESSIVE,
    /** Material You: a generously 16dp-rounded field. */
    MATERIAL_YOU,
    /** Material 2: a lightly 4dp-rounded field. */
    MATERIAL_2,
    ;

    companion object {
        fun deserialize(value: String?): ReplyInputStyle? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}

/**
 * Visual styling of the island that is independent of its geometry: whether it casts a shadow,
 * an optional outline stroke (width + colour), and the fill colour. Colours may be [CutoutColor.Dynamic].
 *
 * The action-button block ([actionButtonStyle] … [cancelButtonOnLeft]) styles the chips and inline
 * reply field shown in the expanded cutout; whether they appear at all is [BehaviourSettings.showActionButtons].
 */
data class AppearanceSettings(
    val shadowEnabled: Boolean = DEFAULT_SHADOW_ENABLED,
    val strokeEnabled: Boolean = DEFAULT_STROKE_ENABLED,
    val strokeWidthDp: Int = DEFAULT_STROKE_WIDTH_DP,
    val strokeColor: CutoutColor = DEFAULT_STROKE_COLOR,
    val backgroundColor: CutoutColor = DEFAULT_BACKGROUND_COLOR,
    val sendButtonColor: CutoutColor? = DEFAULT_SEND_BUTTON_COLOR,
    val cancelButtonColor: CutoutColor? = DEFAULT_CANCEL_BUTTON_COLOR,
    val actionButtonStyle: ActionButtonStyle = DEFAULT_ACTION_BUTTON_STYLE,
    val actionButtonColor: CutoutColor? = DEFAULT_ACTION_BUTTON_COLOR,
    val actionButtonHeightDp: Int = DEFAULT_ACTION_BUTTON_HEIGHT_DP,
    val replyInputStyle: ReplyInputStyle = DEFAULT_REPLY_INPUT_STYLE,
    val cancelButtonOnLeft: Boolean = DEFAULT_CANCEL_ON_LEFT,
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

        // Defaults reproduce the original action-button look exactly.
        val DEFAULT_ACTION_BUTTON_STYLE = ActionButtonStyle.EXPRESSIVE_TONAL
        // null follows the notification's own accent, as the chips historically did.
        val DEFAULT_ACTION_BUTTON_COLOR: CutoutColor? = null
        val DEFAULT_REPLY_INPUT_STYLE = ReplyInputStyle.EXPRESSIVE
        const val DEFAULT_CANCEL_ON_LEFT = false
        const val DEFAULT_ACTION_BUTTON_HEIGHT_DP = 44
        const val MIN_ACTION_BUTTON_HEIGHT_DP = 36
        const val MAX_ACTION_BUTTON_HEIGHT_DP = 56
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
            actionButtonStyle = ActionButtonStyle.deserialize(prefs[ACTION_BUTTON_STYLE])
                ?: AppearanceSettings.DEFAULT_ACTION_BUTTON_STYLE,
            actionButtonColor = CutoutColor.deserialize(prefs[ACTION_BUTTON_COLOR]),
            actionButtonHeightDp = (prefs[ACTION_BUTTON_HEIGHT] ?: AppearanceSettings.DEFAULT_ACTION_BUTTON_HEIGHT_DP)
                .coerceIn(AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP, AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP),
            replyInputStyle = ReplyInputStyle.deserialize(prefs[REPLY_INPUT_STYLE])
                ?: AppearanceSettings.DEFAULT_REPLY_INPUT_STYLE,
            cancelButtonOnLeft = prefs[CANCEL_ON_LEFT] ?: AppearanceSettings.DEFAULT_CANCEL_ON_LEFT,
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

    suspend fun setActionButtonStyle(style: ActionButtonStyle) = context.appearanceDataStore.edit {
        it[ACTION_BUTTON_STYLE] = style.name
    }

    /** A null [color] clears the override, restoring the accent-following default. */
    suspend fun setActionButtonColor(color: CutoutColor?) = context.appearanceDataStore.edit {
        if (color == null) it.remove(ACTION_BUTTON_COLOR) else it[ACTION_BUTTON_COLOR] = color.serialize()
    }

    suspend fun setActionButtonHeight(heightDp: Int) = context.appearanceDataStore.edit {
        it[ACTION_BUTTON_HEIGHT] = heightDp.coerceIn(
            AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP,
            AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP,
        )
    }

    suspend fun setReplyInputStyle(style: ReplyInputStyle) = context.appearanceDataStore.edit {
        it[REPLY_INPUT_STYLE] = style.name
    }

    suspend fun setCancelButtonOnLeft(onLeft: Boolean) = context.appearanceDataStore.edit {
        it[CANCEL_ON_LEFT] = onLeft
    }

    private companion object {
        val SHADOW_ENABLED = booleanPreferencesKey("shadow_enabled")
        val STROKE_ENABLED = booleanPreferencesKey("stroke_enabled")
        val STROKE_WIDTH = intPreferencesKey("stroke_width_dp")
        val STROKE_COLOR = stringPreferencesKey("stroke_color")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val SEND_BUTTON_COLOR = stringPreferencesKey("send_button_color")
        val CANCEL_BUTTON_COLOR = stringPreferencesKey("cancel_button_color")
        val ACTION_BUTTON_STYLE = stringPreferencesKey("action_button_style")
        val ACTION_BUTTON_COLOR = stringPreferencesKey("action_button_color")
        val ACTION_BUTTON_HEIGHT = intPreferencesKey("action_button_height_dp")
        val REPLY_INPUT_STYLE = stringPreferencesKey("reply_input_style")
        val CANCEL_ON_LEFT = booleanPreferencesKey("cancel_button_on_left")
    }
}
