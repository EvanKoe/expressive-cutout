package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import kotlin.math.roundToInt
import org.json.JSONObject

/** Backing store for every appearance setting: fills, strokes, icons and action buttons. */
private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_prefs")

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
    val strokeOpacity: Float = DEFAULT_STROKE_OPACITY,
    val strokeColor: CutoutColor = DEFAULT_STROKE_COLOR,
    val textColor: CutoutColor? = DEFAULT_TEXT_COLOR,
    val backgroundNormal: CutoutFill = DEFAULT_BACKGROUND_FILL,
    val backgroundExpanded: CutoutFill = DEFAULT_BACKGROUND_FILL,
    val sendButtonColor: CutoutColor? = DEFAULT_SEND_BUTTON_COLOR,
    val cancelButtonColor: CutoutColor? = DEFAULT_CANCEL_BUTTON_COLOR,
    val actionButtonStyle: ActionButtonStyle = DEFAULT_ACTION_BUTTON_STYLE,
    val actionButtonColor: CutoutColor? = DEFAULT_ACTION_BUTTON_COLOR,
    val actionButtonHeightDp: Int = DEFAULT_ACTION_BUTTON_HEIGHT_DP,
    val actionButtonAlignment: ActionButtonAlignment = DEFAULT_ACTION_BUTTON_ALIGNMENT,
    val replyInputStyle: ReplyInputStyle = DEFAULT_REPLY_INPUT_STYLE,
    val cancelButtonOnLeft: Boolean = DEFAULT_CANCEL_ON_LEFT,
    val sentAlignment: SentAlignment = DEFAULT_SENT_ALIGNMENT,
    val showFullNotificationText: Boolean = DEFAULT_SHOW_FULL_NOTIFICATION_TEXT,
) {
    companion object {
        const val DEFAULT_SHADOW_ENABLED = true
        const val DEFAULT_STROKE_ENABLED = false
        const val DEFAULT_STROKE_WIDTH_DP = 2
        const val MIN_STROKE_WIDTH_DP = 1
        const val MAX_STROKE_WIDTH_DP = 8
        const val DEFAULT_STROKE_OPACITY = 1f
        const val DEFAULT_SHOW_FULL_NOTIFICATION_TEXT = true

        /** Match the pill's historical look: near-black fill, white stroke. */
        val DEFAULT_BACKGROUND_FILL: CutoutFill = CutoutFill.Solid(ColorSpec.Fixed(0xFF0A0A0A))
        val DEFAULT_STROKE_COLOR: CutoutColor = CutoutColor.Solid(0xFFFFFFFF)
        /** null means automatic high-contrast text color based on background luminance. */
        val DEFAULT_TEXT_COLOR: CutoutColor? = null

        /**
         * null keeps the historical reply-button look: the send button matches the notification's
         * own accent and the cancel button stays a neutral tint.
         */
        val DEFAULT_SEND_BUTTON_COLOR: CutoutColor? = null
        val DEFAULT_CANCEL_BUTTON_COLOR: CutoutColor? = null

        /** Defaults reproduce the original action-button look exactly. */
        val DEFAULT_ACTION_BUTTON_STYLE = ActionButtonStyle.EXPRESSIVE_TONAL
        /** null follows the notification's own accent, as the chips historically did. */
        val DEFAULT_ACTION_BUTTON_COLOR: CutoutColor? = null
        /** Chips historically hugged the leading edge. */
        val DEFAULT_ACTION_BUTTON_ALIGNMENT = ActionButtonAlignment.LEFT
        val DEFAULT_REPLY_INPUT_STYLE = ReplyInputStyle.EXPRESSIVE
        const val DEFAULT_CANCEL_ON_LEFT = false
        /** The confirmation historically hugged the leading edge. */
        val DEFAULT_SENT_ALIGNMENT = SentAlignment.LEFT
        const val DEFAULT_ACTION_BUTTON_HEIGHT_DP = 44
        const val MIN_ACTION_BUTTON_HEIGHT_DP = 36
        const val MAX_ACTION_BUTTON_HEIGHT_DP = 56
    }
}

/** Persists [AppearanceSettings], always emitting a clamped stroke width. */
class AppearancePreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        AppearanceSettings(
            shadowEnabled = prefs[SHADOW_ENABLED] ?: AppearanceSettings.DEFAULT_SHADOW_ENABLED,
            strokeEnabled = prefs[STROKE_ENABLED] ?: AppearanceSettings.DEFAULT_STROKE_ENABLED,
            strokeWidthDp = (prefs[STROKE_WIDTH] ?: AppearanceSettings.DEFAULT_STROKE_WIDTH_DP)
                .coerceIn(AppearanceSettings.MIN_STROKE_WIDTH_DP, AppearanceSettings.MAX_STROKE_WIDTH_DP),
            strokeOpacity = (prefs[STROKE_OPACITY] ?: AppearanceSettings.DEFAULT_STROKE_OPACITY).coerceIn(0f, 1f),
            strokeColor = CutoutColor.deserialize(prefs[STROKE_COLOR]) ?: AppearanceSettings.DEFAULT_STROKE_COLOR,
            textColor = CutoutColor.deserialize(prefs[TEXT_COLOR]),
            // Fall back to the legacy single background colour so existing installs migrate into
            // both states, then to the built-in default.
            backgroundNormal = CutoutFill.deserialize(prefs[BACKGROUND_NORMAL] ?: prefs[BACKGROUND_COLOR])
                ?: AppearanceSettings.DEFAULT_BACKGROUND_FILL,
            backgroundExpanded = CutoutFill.deserialize(prefs[BACKGROUND_EXPANDED] ?: prefs[BACKGROUND_COLOR])
                ?: AppearanceSettings.DEFAULT_BACKGROUND_FILL,
            sendButtonColor = CutoutColor.deserialize(prefs[SEND_BUTTON_COLOR]),
            cancelButtonColor = CutoutColor.deserialize(prefs[CANCEL_BUTTON_COLOR]),
            actionButtonStyle = ActionButtonStyle.deserialize(prefs[ACTION_BUTTON_STYLE])
                ?: AppearanceSettings.DEFAULT_ACTION_BUTTON_STYLE,
            actionButtonColor = CutoutColor.deserialize(prefs[ACTION_BUTTON_COLOR]),
            actionButtonHeightDp = (prefs[ACTION_BUTTON_HEIGHT] ?: AppearanceSettings.DEFAULT_ACTION_BUTTON_HEIGHT_DP)
                .coerceIn(AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP, AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP),
            actionButtonAlignment = ActionButtonAlignment.deserialize(prefs[ACTION_BUTTON_ALIGNMENT])
                ?: AppearanceSettings.DEFAULT_ACTION_BUTTON_ALIGNMENT,
            replyInputStyle = ReplyInputStyle.deserialize(prefs[REPLY_INPUT_STYLE])
                ?: AppearanceSettings.DEFAULT_REPLY_INPUT_STYLE,
            cancelButtonOnLeft = prefs[CANCEL_ON_LEFT] ?: AppearanceSettings.DEFAULT_CANCEL_ON_LEFT,
            sentAlignment = SentAlignment.deserialize(prefs[SENT_ALIGNMENT])
                ?: AppearanceSettings.DEFAULT_SENT_ALIGNMENT,
            showFullNotificationText = prefs[SHOW_FULL_NOTIFICATION_TEXT]
                ?: AppearanceSettings.DEFAULT_SHOW_FULL_NOTIFICATION_TEXT,
        )
    }

    /** Exports the current, fully-resolved [AppearanceSettings] (defaults and clamping applied) as a JSON string. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("shadowEnabled", s.shadowEnabled)
            put("strokeEnabled", s.strokeEnabled)
            put("strokeWidthDp", s.strokeWidthDp)
            put("strokeOpacity", s.strokeOpacity.toDouble())
            put("strokeColor", s.strokeColor.serialize())
            put("textColor", s.textColor?.serialize() ?: JSONObject.NULL)
            put("backgroundNormal", s.backgroundNormal.serialize())
            put("backgroundExpanded", s.backgroundExpanded.serialize())
            put("sendButtonColor", s.sendButtonColor?.serialize() ?: JSONObject.NULL)
            put("cancelButtonColor", s.cancelButtonColor?.serialize() ?: JSONObject.NULL)
            put("actionButtonStyle", s.actionButtonStyle.name)
            put("actionButtonColor", s.actionButtonColor?.serialize() ?: JSONObject.NULL)
            put("actionButtonHeightDp", s.actionButtonHeightDp)
            put("actionButtonAlignment", s.actionButtonAlignment.name)
            put("replyInputStyle", s.replyInputStyle.name)
            put("cancelButtonOnLeft", s.cancelButtonOnLeft)
            put("sentAlignment", s.sentAlignment.name)
            put("showFullNotificationText", s.showFullNotificationText)
        }.toString()
    }

    /**
     * Applies the [AppearanceSettings] object exported by [toJson]; absent fields are left as-is.
     * Colours and fills are re-parsed (and re-serialised) so a malformed value is skipped rather
     * than written back verbatim, and a null nullable-colour clears its override.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.appearanceDataStore.edit {
            if (obj.has("shadowEnabled")) it[SHADOW_ENABLED] = obj.getBoolean("shadowEnabled")
            if (obj.has("strokeEnabled")) it[STROKE_ENABLED] = obj.getBoolean("strokeEnabled")
            if (obj.has("strokeWidthDp")) it[STROKE_WIDTH] = obj.getInt("strokeWidthDp")
                .coerceIn(AppearanceSettings.MIN_STROKE_WIDTH_DP, AppearanceSettings.MAX_STROKE_WIDTH_DP)
            if (obj.has("strokeOpacity")) it[STROKE_OPACITY] = obj.getDouble("strokeOpacity").toFloat().coerceIn(0f, 1f)
            if (obj.has("strokeColor") && !obj.isNull("strokeColor")) {
                CutoutColor.deserialize(obj.optString("strokeColor"))?.let { c -> it[STROKE_COLOR] = c.serialize() }
            }
            it.applyNullableColor(obj, "textColor", TEXT_COLOR)
            if (obj.has("backgroundNormal") && !obj.isNull("backgroundNormal")) {
                CutoutFill.deserialize(obj.optString("backgroundNormal"))?.let { f -> it[BACKGROUND_NORMAL] = f.serialize() }
            }
            if (obj.has("backgroundExpanded") && !obj.isNull("backgroundExpanded")) {
                CutoutFill.deserialize(obj.optString("backgroundExpanded"))?.let { f -> it[BACKGROUND_EXPANDED] = f.serialize() }
            }
            it.applyNullableColor(obj, "sendButtonColor", SEND_BUTTON_COLOR)
            it.applyNullableColor(obj, "cancelButtonColor", CANCEL_BUTTON_COLOR)
            it.applyNullableColor(obj, "actionButtonColor", ACTION_BUTTON_COLOR)
            if (obj.has("actionButtonStyle")) {
                ActionButtonStyle.deserialize(obj.optString("actionButtonStyle"))?.let { s -> it[ACTION_BUTTON_STYLE] = s.name }
            }
            if (obj.has("actionButtonHeightDp")) it[ACTION_BUTTON_HEIGHT] = obj.getInt("actionButtonHeightDp")
                .coerceIn(AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP, AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP)
            if (obj.has("actionButtonAlignment")) {
                ActionButtonAlignment.deserialize(obj.optString("actionButtonAlignment"))?.let { a -> it[ACTION_BUTTON_ALIGNMENT] = a.name }
            }
            if (obj.has("replyInputStyle")) {
                ReplyInputStyle.deserialize(obj.optString("replyInputStyle"))?.let { s -> it[REPLY_INPUT_STYLE] = s.name }
            }
            if (obj.has("cancelButtonOnLeft")) it[CANCEL_ON_LEFT] = obj.getBoolean("cancelButtonOnLeft")
            if (obj.has("sentAlignment")) {
                SentAlignment.deserialize(obj.optString("sentAlignment"))?.let { a -> it[SENT_ALIGNMENT] = a.name }
            }
            if (obj.has("showFullNotificationText")) {
                it[SHOW_FULL_NOTIFICATION_TEXT] = obj.getBoolean("showFullNotificationText")
            }
        }
    }

    /** Sets [key] from a nullable-colour field: a JSON null (or missing colour) clears the override. */
    private fun MutablePreferences.applyNullableColor(
        obj: JSONObject,
        field: String,
        key: Preferences.Key<String>,
    ) {
        if (!obj.has(field)) return
        val raw = if (obj.isNull(field)) null else obj.optString(field)
        val color = CutoutColor.deserialize(raw)
        if (color == null) remove(key) else this[key] = color.serialize()
    }

    suspend fun setShadowEnabled(enabled: Boolean) = context.appearanceDataStore.edit {
        it[SHADOW_ENABLED] = enabled
    }

    suspend fun setStrokeEnabled(enabled: Boolean) = context.appearanceDataStore.edit {
        it[STROKE_ENABLED] = enabled
    }

    /**
     * Clamps to the range the settings slider offers, so an imported settings file can't leave a
     * stroke width the UI has no way to correct.
     */
    suspend fun setStrokeWidth(widthDp: Int) = context.appearanceDataStore.edit {
        it[STROKE_WIDTH] = widthDp.coerceIn(
            AppearanceSettings.MIN_STROKE_WIDTH_DP,
            AppearanceSettings.MAX_STROKE_WIDTH_DP,
        )
    }

    suspend fun setStrokeOpacity(opacity: Float) = context.appearanceDataStore.edit {
        it[STROKE_OPACITY] = opacity.coerceIn(0f, 1f)
    }

    suspend fun setStrokeColor(color: CutoutColor) = context.appearanceDataStore.edit {
        it[STROKE_COLOR] = color.serialize()
    }

    /** A null [color] clears the override, restoring automatic contrast-based text color. */
    suspend fun setTextColor(color: CutoutColor?) = context.appearanceDataStore.edit {
        if (color == null) it.remove(TEXT_COLOR) else it[TEXT_COLOR] = color.serialize()
    }

    suspend fun setBackgroundNormal(fill: CutoutFill) = context.appearanceDataStore.edit {
        it[BACKGROUND_NORMAL] = fill.serialize()
    }

    suspend fun setBackgroundExpanded(fill: CutoutFill) = context.appearanceDataStore.edit {
        it[BACKGROUND_EXPANDED] = fill.serialize()
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

    /**
     * Clamps to the range the settings slider offers, so an imported settings file can't leave a
     * button height the UI has no way to correct.
     */
    suspend fun setActionButtonHeight(heightDp: Int) = context.appearanceDataStore.edit {
        it[ACTION_BUTTON_HEIGHT] = heightDp.coerceIn(
            AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP,
            AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP,
        )
    }

    suspend fun setActionButtonAlignment(alignment: ActionButtonAlignment) = context.appearanceDataStore.edit {
        it[ACTION_BUTTON_ALIGNMENT] = alignment.name
    }

    suspend fun setReplyInputStyle(style: ReplyInputStyle) = context.appearanceDataStore.edit {
        it[REPLY_INPUT_STYLE] = style.name
    }

    suspend fun setCancelButtonOnLeft(onLeft: Boolean) = context.appearanceDataStore.edit {
        it[CANCEL_ON_LEFT] = onLeft
    }

    suspend fun setSentAlignment(alignment: SentAlignment) = context.appearanceDataStore.edit {
        it[SENT_ALIGNMENT] = alignment.name
    }

    suspend fun setShowFullNotificationText(enabled: Boolean) = context.appearanceDataStore.edit {
        it[SHOW_FULL_NOTIFICATION_TEXT] = enabled
    }

    private companion object {
        val SHADOW_ENABLED = booleanPreferencesKey("shadow_enabled")
        val STROKE_ENABLED = booleanPreferencesKey("stroke_enabled")
        val STROKE_WIDTH = intPreferencesKey("stroke_width_dp")
        val STROKE_OPACITY = floatPreferencesKey("stroke_opacity")
        val STROKE_COLOR = stringPreferencesKey("stroke_color")
        val TEXT_COLOR = stringPreferencesKey("text_color")
        /**
         * Legacy single-colour key, still read to migrate existing installs into the two new keys.
         */
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val BACKGROUND_NORMAL = stringPreferencesKey("background_normal")
        val BACKGROUND_EXPANDED = stringPreferencesKey("background_expanded")
        val SEND_BUTTON_COLOR = stringPreferencesKey("send_button_color")
        val CANCEL_BUTTON_COLOR = stringPreferencesKey("cancel_button_color")
        val ACTION_BUTTON_STYLE = stringPreferencesKey("action_button_style")
        val ACTION_BUTTON_COLOR = stringPreferencesKey("action_button_color")
        val ACTION_BUTTON_HEIGHT = intPreferencesKey("action_button_height_dp")
        val ACTION_BUTTON_ALIGNMENT = stringPreferencesKey("action_button_alignment")
        val REPLY_INPUT_STYLE = stringPreferencesKey("reply_input_style")
        val CANCEL_ON_LEFT = booleanPreferencesKey("cancel_button_on_left")
        val SENT_ALIGNMENT = stringPreferencesKey("sent_alignment")
        val SHOW_FULL_NOTIFICATION_TEXT = booleanPreferencesKey("show_full_notification_text")
    }
}
