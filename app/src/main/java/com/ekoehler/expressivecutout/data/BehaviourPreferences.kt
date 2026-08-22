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

private val Context.behaviourDataStore: DataStore<Preferences> by preferencesDataStore(name = "behaviour_prefs")

/** How the cutout behaves when the device is in horizontal/landscape orientation. */
enum class HorizontalCutoutMode { HIDDEN, NORMAL_ONLY, STICK_TO_CAMERA, CENTER }

/** Which horizontal swipe directions dismiss the cutout, when swipe-to-dismiss is enabled. */
enum class SwipeDismissDirection { LEFT, RIGHT, BOTH }

/**
 * Which cutout state swipe-to-dismiss applies to. Ordered to match the settings selector
 * (Expanded / Both / Normal) so the ordinal doubles as the segment index.
 */
enum class SwipeDismissTarget { EXPANDED, BOTH, NORMAL }

/**
 * How the island's appear / expand / collapse motion is driven. [EXPRESSIVE] uses Material 3
 * MotionScheme-style spatial springs (its speed comes from [AnimationSpeed]); [EASE_IN_OUT] uses a
 * standard ease-in-out tween whose length is the animation-duration slider. Ordered to match the
 * settings selector so the ordinal doubles as the segment index.
 */
enum class AnimationStyle { EXPRESSIVE, EASE_IN_OUT }

/**
 * The spatial-spring speed used when [AnimationStyle.EXPRESSIVE] is active, mirroring MotionScheme's
 * slow / default / fast spatial specs. Ordered to match the settings selector.
 */
enum class AnimationSpeed { SLOW, DEFAULT, FAST }

/**
 * How much the expressive spatial springs overshoot (their damping): a big, obvious bounce, the
 * tuned normal, or the barely-there stock MotionScheme feel. Ordered to match the settings selector.
 */
enum class AnimationBounce { BIG, NORMAL, SMALL }

/**
 * How the action buttons (and reply buttons) react to a press. [SCALE] springs down and back like
 * a squish; [EXPAND] briefly widens the button by a few dp instead. Ordered to match the settings
 * selector so the ordinal doubles as the segment index.
 */
enum class ActionButtonAnimation { SCALE, EXPAND }

/**
 * What tapping the resting (event-less) empty pill does. [NONE] only plays the press animation;
 * [OPEN_APP] launches the app chosen in [BehaviourSettings.showsWhenEmptyClickPackage]; [OPEN_CENTER]
 * is reserved for a future feature. Ordered to match the settings selector.
 */
enum class EmptyClickAction { NONE, OPEN_APP, OPEN_CENTER }

/**
 * How the island behaves once expanded. [expandedAutoCollapse] chooses between collapsing after
 * [expandedCollapseSeconds] or staying until tapped. When that shrink happens,
 * [expandedDisappearOnShrink] decides whether the island disappears entirely (true) or returns
 * to the normal cutout (false).
 */
data class BehaviourSettings(
    val cutoutEnabled: Boolean = DEFAULT_CUTOUT_ENABLED,
    val hideOnLockscreen: Boolean = DEFAULT_HIDE_ON_LOCKSCREEN,
    val hideInLandscape: Boolean = DEFAULT_HIDE_IN_LANDSCAPE,
    val horizontalCutoutMode: HorizontalCutoutMode = DEFAULT_HORIZONTAL_CUTOUT_MODE,
    val animationStyle: AnimationStyle = DEFAULT_ANIMATION_STYLE,
    val animationSpeed: AnimationSpeed = DEFAULT_ANIMATION_SPEED,
    val animationBounce: AnimationBounce = DEFAULT_ANIMATION_BOUNCE,
    val actionButtonAnimation: ActionButtonAnimation = DEFAULT_ACTION_BUTTON_ANIMATION,
    val animationDurationMs: Int = DEFAULT_ANIMATION_DURATION_MS,
    val normalDurationSeconds: Int = DEFAULT_NORMAL_SECONDS,
    val expandedAutoCollapse: Boolean = DEFAULT_AUTO_COLLAPSE,
    val expandedCollapseSeconds: Int = DEFAULT_COLLAPSE_SECONDS,
    val expandedDisappearOnShrink: Boolean = DEFAULT_DISAPPEAR_ON_SHRINK,
    val notificationsAutoExpand: Boolean = DEFAULT_NOTIFICATIONS_AUTO_EXPAND,
    val ignoreSilentNotifications: Boolean = DEFAULT_IGNORE_SILENT_NOTIFICATIONS,
    val showActionButtons: Boolean = DEFAULT_SHOW_ACTION_BUTTONS,
    val toastOnAction: Boolean = DEFAULT_TOAST_ON_ACTION,
    val shrinkOnSwipeUp: Boolean = DEFAULT_SHRINK_ON_SWIPE_UP,
    val swipeToDismiss: Boolean = DEFAULT_SWIPE_TO_DISMISS,
    val swipeDismissDirection: SwipeDismissDirection = DEFAULT_SWIPE_DISMISS_DIRECTION,
    val swipeDismissTarget: SwipeDismissTarget = DEFAULT_SWIPE_DISMISS_TARGET,
    val showsWhenEmpty: Boolean = SHOWS_WHEN_EMPTY,
    val showsWhenEmptyShowIcon: Boolean = SHOWS_WHEN_EMPTY_SHOW_ICON,
    val showsWhenEmptyIcon: IconSource? = null,
    val showsWhenEmptyIconColor: CutoutColor? = null,
    val showsWhenEmptyClickAction: EmptyClickAction = DEFAULT_EMPTY_CLICK_ACTION,
    val showsWhenEmptyClickPackage: String? = null,
    val centerShortcuts: List<CenterShortcut> = CenterShortcut.DEFAULTS,
    val centerShowLabels: Boolean = CENTER_SHOW_LABELS,
    val centerFillContainers: Boolean = CENTER_FILL_CONTAINERS,
    val centerThemedIcons: Boolean = CENTER_THEMED_ICONS,
    val vibrateOnTap: Boolean = DEFAULT_VIBRATE_ON_TAP,
) {
    companion object {
        const val DEFAULT_CUTOUT_ENABLED = true
        const val DEFAULT_HIDE_ON_LOCKSCREEN = false
        const val DEFAULT_HIDE_IN_LANDSCAPE = false
        const val DEFAULT_VIBRATE_ON_TAP = true
        val DEFAULT_HORIZONTAL_CUTOUT_MODE = HorizontalCutoutMode.CENTER
        // Baseline for the island's primary expand/collapse transition; the reveal, background fade
        // and other animations scale in proportion to it. Matches the tuned defaults in DynamicIsland.
        const val DEFAULT_ANIMATION_DURATION_MS = 220
        val DEFAULT_ANIMATION_STYLE = AnimationStyle.EXPRESSIVE
        val DEFAULT_ANIMATION_SPEED = AnimationSpeed.DEFAULT
        val DEFAULT_ANIMATION_BOUNCE = AnimationBounce.NORMAL
        val DEFAULT_ACTION_BUTTON_ANIMATION = ActionButtonAnimation.SCALE
        const val DEFAULT_NORMAL_SECONDS = 3
        const val DEFAULT_AUTO_COLLAPSE = true
        const val DEFAULT_COLLAPSE_SECONDS = 5
        const val DEFAULT_DISAPPEAR_ON_SHRINK = false
        const val DEFAULT_NOTIFICATIONS_AUTO_EXPAND = false
        const val DEFAULT_IGNORE_SILENT_NOTIFICATIONS = false
        const val DEFAULT_SHOW_ACTION_BUTTONS = true
        const val DEFAULT_TOAST_ON_ACTION = true
        const val DEFAULT_SHRINK_ON_SWIPE_UP = true
        const val DEFAULT_SWIPE_TO_DISMISS = true
        val DEFAULT_SWIPE_DISMISS_DIRECTION = SwipeDismissDirection.BOTH
        val DEFAULT_SWIPE_DISMISS_TARGET = SwipeDismissTarget.BOTH
        const val MIN_ANIMATION_DURATION_MS = 0
        const val MAX_ANIMATION_DURATION_MS = 1000
        const val MIN_NORMAL_SECONDS = 1
        const val MAX_NORMAL_SECONDS = 10
        const val MIN_COLLAPSE_SECONDS = 1
        const val MAX_COLLAPSE_SECONDS = 15
        const val SHOWS_WHEN_EMPTY = false
        const val SHOWS_WHEN_EMPTY_SHOW_ICON = false
        val DEFAULT_EMPTY_CLICK_ACTION = EmptyClickAction.NONE
        const val CENTER_SHOW_LABELS = true
        const val CENTER_FILL_CONTAINERS = false
        const val CENTER_THEMED_ICONS = false
    }
}

/** Persists [BehaviourSettings], always emitting a clamped collapse delay. */
class BehaviourPreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<BehaviourSettings> = context.behaviourDataStore.data.map { prefs ->
        val rawMode = prefs[HORIZONTAL_CUTOUT_MODE]
        val hideLandscape = prefs[HIDE_IN_LANDSCAPE] ?: BehaviourSettings.DEFAULT_HIDE_IN_LANDSCAPE
        val horizontalCutoutMode = if (rawMode != null) {
            runCatching { HorizontalCutoutMode.valueOf(rawMode) }.getOrDefault(BehaviourSettings.DEFAULT_HORIZONTAL_CUTOUT_MODE)
        } else if (hideLandscape) {
            HorizontalCutoutMode.HIDDEN
        } else {
            BehaviourSettings.DEFAULT_HORIZONTAL_CUTOUT_MODE
        }

        BehaviourSettings(
            cutoutEnabled = prefs[CUTOUT_ENABLED] ?: BehaviourSettings.DEFAULT_CUTOUT_ENABLED,
            hideOnLockscreen = prefs[HIDE_ON_LOCKSCREEN] ?: BehaviourSettings.DEFAULT_HIDE_ON_LOCKSCREEN,
            hideInLandscape = hideLandscape || (horizontalCutoutMode == HorizontalCutoutMode.HIDDEN),
            horizontalCutoutMode = horizontalCutoutMode,
            animationStyle = prefs[ANIMATION_STYLE]
                ?.let { runCatching { AnimationStyle.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_ANIMATION_STYLE,
            animationSpeed = prefs[ANIMATION_SPEED]
                ?.let { runCatching { AnimationSpeed.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_ANIMATION_SPEED,
            animationBounce = prefs[ANIMATION_BOUNCE]
                ?.let { runCatching { AnimationBounce.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_ANIMATION_BOUNCE,
            actionButtonAnimation = prefs[ACTION_BUTTON_ANIMATION]
                ?.let { runCatching { ActionButtonAnimation.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_ACTION_BUTTON_ANIMATION,
            animationDurationMs = (prefs[ANIMATION_DURATION_MS] ?: BehaviourSettings.DEFAULT_ANIMATION_DURATION_MS)
                .coerceIn(BehaviourSettings.MIN_ANIMATION_DURATION_MS, BehaviourSettings.MAX_ANIMATION_DURATION_MS),
            normalDurationSeconds = (prefs[NORMAL_SECONDS] ?: BehaviourSettings.DEFAULT_NORMAL_SECONDS)
                .coerceIn(BehaviourSettings.MIN_NORMAL_SECONDS, BehaviourSettings.MAX_NORMAL_SECONDS),
            expandedAutoCollapse = prefs[AUTO_COLLAPSE] ?: BehaviourSettings.DEFAULT_AUTO_COLLAPSE,
            expandedCollapseSeconds = (prefs[COLLAPSE_SECONDS] ?: BehaviourSettings.DEFAULT_COLLAPSE_SECONDS)
                .coerceIn(BehaviourSettings.MIN_COLLAPSE_SECONDS, BehaviourSettings.MAX_COLLAPSE_SECONDS),
            expandedDisappearOnShrink = prefs[DISAPPEAR_ON_SHRINK] ?: BehaviourSettings.DEFAULT_DISAPPEAR_ON_SHRINK,
            notificationsAutoExpand = prefs[NOTIF_AUTO_EXPAND] ?: BehaviourSettings.DEFAULT_NOTIFICATIONS_AUTO_EXPAND,
            ignoreSilentNotifications = prefs[IGNORE_SILENT_NOTIFICATIONS] ?: BehaviourSettings.DEFAULT_IGNORE_SILENT_NOTIFICATIONS,
            showActionButtons = prefs[SHOW_ACTION_BUTTONS] ?: BehaviourSettings.DEFAULT_SHOW_ACTION_BUTTONS,
            toastOnAction = prefs[TOAST_ON_ACTION] ?: BehaviourSettings.DEFAULT_TOAST_ON_ACTION,
            shrinkOnSwipeUp = prefs[SHRINK_ON_SWIPE_UP] ?: BehaviourSettings.DEFAULT_SHRINK_ON_SWIPE_UP,
            swipeToDismiss = prefs[SWIPE_TO_DISMISS] ?: BehaviourSettings.DEFAULT_SWIPE_TO_DISMISS,
            swipeDismissDirection = prefs[SWIPE_DISMISS_DIRECTION]
                ?.let { runCatching { SwipeDismissDirection.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_SWIPE_DISMISS_DIRECTION,
            swipeDismissTarget = prefs[SWIPE_DISMISS_TARGET]
                ?.let { runCatching { SwipeDismissTarget.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_SWIPE_DISMISS_TARGET,
            showsWhenEmpty = prefs[SHOWS_WHEN_EMPTY] ?: BehaviourSettings.SHOWS_WHEN_EMPTY,
            showsWhenEmptyShowIcon = prefs[SHOWS_WHEN_EMPTY_SHOW_ICON] ?: BehaviourSettings.SHOWS_WHEN_EMPTY_SHOW_ICON,
            showsWhenEmptyIcon = prefs[SHOWS_WHEN_EMPTY_ICON]?.let { IconSource.decode(it) },
            showsWhenEmptyIconColor = CutoutColor.deserialize(prefs[SHOWS_WHEN_EMPTY_ICON_COLOR]),
            showsWhenEmptyClickAction = prefs[SHOWS_WHEN_EMPTY_CLICK_ACTION]
                ?.let { runCatching { EmptyClickAction.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_EMPTY_CLICK_ACTION,
            showsWhenEmptyClickPackage = prefs[SHOWS_WHEN_EMPTY_CLICK_PACKAGE],
            centerShortcuts = CenterShortcut.decodeList(prefs[CENTER_SHORTCUTS]),
            centerShowLabels = prefs[CENTER_SHOW_LABELS] ?: BehaviourSettings.CENTER_SHOW_LABELS,
            centerFillContainers = prefs[CENTER_FILL_CONTAINERS] ?: BehaviourSettings.CENTER_FILL_CONTAINERS,
            centerThemedIcons = prefs[CENTER_THEMED_ICONS] ?: BehaviourSettings.CENTER_THEMED_ICONS,
            vibrateOnTap = prefs[VIBRATE_ON_TAP] ?: BehaviourSettings.DEFAULT_VIBRATE_ON_TAP
        )
    }

    /** Exports the current, fully-resolved [BehaviourSettings] (defaults and clamping applied) as a JSON string. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("cutoutEnabled", s.cutoutEnabled)
            put("hideOnLockscreen", s.hideOnLockscreen)
            put("hideInLandscape", s.hideInLandscape)
            put("horizontalCutoutMode", s.horizontalCutoutMode.name)
            put("animationStyle", s.animationStyle.name)
            put("animationSpeed", s.animationSpeed.name)
            put("animationBounce", s.animationBounce.name)
            put("actionButtonAnimation", s.actionButtonAnimation.name)
            put("animationDurationMs", s.animationDurationMs)
            put("normalDurationSeconds", s.normalDurationSeconds)
            put("expandedAutoCollapse", s.expandedAutoCollapse)
            put("expandedCollapseSeconds", s.expandedCollapseSeconds)
            put("expandedDisappearOnShrink", s.expandedDisappearOnShrink)
            put("notificationsAutoExpand", s.notificationsAutoExpand)
            put("ignoreSilentNotifications", s.ignoreSilentNotifications)
            put("showActionButtons", s.showActionButtons)
            put("toastOnAction", s.toastOnAction)
            put("shrinkOnSwipeUp", s.shrinkOnSwipeUp)
            put("swipeToDismiss", s.swipeToDismiss)
            put("swipeDismissDirection", s.swipeDismissDirection.name)
            put("swipeDismissTarget", s.swipeDismissTarget.name)
            put("showsWhenEmpty", s.showsWhenEmpty)
            put("showsWhenEmptyShowIcon", s.showsWhenEmptyShowIcon)
            put("showsWhenEmptyIcon", s.showsWhenEmptyIcon?.encode() ?: JSONObject.NULL)
            put("showsWhenEmptyIconColor", s.showsWhenEmptyIconColor?.serialize() ?: JSONObject.NULL)
            put("showsWhenEmptyClickAction", s.showsWhenEmptyClickAction.name)
            put("showsWhenEmptyClickPackage", s.showsWhenEmptyClickPackage ?: JSONObject.NULL)
            put("centerShortcuts", CenterShortcut.encodeList(s.centerShortcuts))
            put("centerShowLabels", s.centerShowLabels)
            put("centerFillContainers", s.centerFillContainers)
            put("centerThemedIcons", s.centerThemedIcons)
            put("vibrateOnTap", s.vibrateOnTap)
        }.toString()
    }

    /**
     * Applies the [BehaviourSettings] object exported by [toJson]; absent fields are left as-is.
     * Enums are validated (an unknown name is skipped), ranges are clamped, and the nullable icon /
     * colour / package fields clear their key on a JSON null. The [HIDE_IN_LANDSCAPE] key is written
     * alongside [HORIZONTAL_CUTOUT_MODE] to keep the pair the read path derives from consistent.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.behaviourDataStore.edit {
            if (obj.has("cutoutEnabled")) it[CUTOUT_ENABLED] = obj.getBoolean("cutoutEnabled")
            if (obj.has("hideOnLockscreen")) it[HIDE_ON_LOCKSCREEN] = obj.getBoolean("hideOnLockscreen")
            if (obj.has("hideInLandscape")) it[HIDE_IN_LANDSCAPE] = obj.getBoolean("hideInLandscape")
            parseEnum<HorizontalCutoutMode>(obj, "horizontalCutoutMode")?.let { mode ->
                it[HORIZONTAL_CUTOUT_MODE] = mode.name
                it[HIDE_IN_LANDSCAPE] = (mode == HorizontalCutoutMode.HIDDEN) || (it[HIDE_IN_LANDSCAPE] ?: false)
            }
            parseEnum<AnimationStyle>(obj, "animationStyle")?.let { s -> it[ANIMATION_STYLE] = s.name }
            parseEnum<AnimationSpeed>(obj, "animationSpeed")?.let { s -> it[ANIMATION_SPEED] = s.name }
            parseEnum<AnimationBounce>(obj, "animationBounce")?.let { b -> it[ANIMATION_BOUNCE] = b.name }
            parseEnum<ActionButtonAnimation>(obj, "actionButtonAnimation")?.let { a -> it[ACTION_BUTTON_ANIMATION] = a.name }
            if (obj.has("animationDurationMs")) it[ANIMATION_DURATION_MS] = obj.getInt("animationDurationMs")
                .coerceIn(BehaviourSettings.MIN_ANIMATION_DURATION_MS, BehaviourSettings.MAX_ANIMATION_DURATION_MS)
            if (obj.has("normalDurationSeconds")) it[NORMAL_SECONDS] = obj.getInt("normalDurationSeconds")
                .coerceIn(BehaviourSettings.MIN_NORMAL_SECONDS, BehaviourSettings.MAX_NORMAL_SECONDS)
            if (obj.has("expandedAutoCollapse")) it[AUTO_COLLAPSE] = obj.getBoolean("expandedAutoCollapse")
            if (obj.has("expandedCollapseSeconds")) it[COLLAPSE_SECONDS] = obj.getInt("expandedCollapseSeconds")
                .coerceIn(BehaviourSettings.MIN_COLLAPSE_SECONDS, BehaviourSettings.MAX_COLLAPSE_SECONDS)
            if (obj.has("expandedDisappearOnShrink")) it[DISAPPEAR_ON_SHRINK] = obj.getBoolean("expandedDisappearOnShrink")
            if (obj.has("notificationsAutoExpand")) it[NOTIF_AUTO_EXPAND] = obj.getBoolean("notificationsAutoExpand")
            if (obj.has("ignoreSilentNotifications")) it[IGNORE_SILENT_NOTIFICATIONS] = obj.getBoolean("ignoreSilentNotifications")
            if (obj.has("showActionButtons")) it[SHOW_ACTION_BUTTONS] = obj.getBoolean("showActionButtons")
            if (obj.has("toastOnAction")) it[TOAST_ON_ACTION] = obj.getBoolean("toastOnAction")
            if (obj.has("shrinkOnSwipeUp")) it[SHRINK_ON_SWIPE_UP] = obj.getBoolean("shrinkOnSwipeUp")
            if (obj.has("swipeToDismiss")) it[SWIPE_TO_DISMISS] = obj.getBoolean("swipeToDismiss")
            parseEnum<SwipeDismissDirection>(obj, "swipeDismissDirection")?.let { d -> it[SWIPE_DISMISS_DIRECTION] = d.name }
            parseEnum<SwipeDismissTarget>(obj, "swipeDismissTarget")?.let { t -> it[SWIPE_DISMISS_TARGET] = t.name }
            if (obj.has("showsWhenEmpty")) it[SHOWS_WHEN_EMPTY] = obj.getBoolean("showsWhenEmpty")
            if (obj.has("showsWhenEmptyShowIcon")) it[SHOWS_WHEN_EMPTY_SHOW_ICON] = obj.getBoolean("showsWhenEmptyShowIcon")
            if (obj.has("showsWhenEmptyIcon")) {
                val raw = if (obj.isNull("showsWhenEmptyIcon")) null else obj.optString("showsWhenEmptyIcon")
                val icon = raw?.let { s -> IconSource.decode(s) }
                if (icon == null) it.remove(SHOWS_WHEN_EMPTY_ICON) else it[SHOWS_WHEN_EMPTY_ICON] = icon.encode()
            }
            if (obj.has("showsWhenEmptyIconColor")) {
                val raw = if (obj.isNull("showsWhenEmptyIconColor")) null else obj.optString("showsWhenEmptyIconColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) it.remove(SHOWS_WHEN_EMPTY_ICON_COLOR) else it[SHOWS_WHEN_EMPTY_ICON_COLOR] = color.serialize()
            }
            parseEnum<EmptyClickAction>(obj, "showsWhenEmptyClickAction")?.let { a -> it[SHOWS_WHEN_EMPTY_CLICK_ACTION] = a.name }
            if (obj.has("showsWhenEmptyClickPackage")) {
                val pkg = if (obj.isNull("showsWhenEmptyClickPackage")) null
                else obj.optString("showsWhenEmptyClickPackage").takeIf { s -> s.isNotEmpty() }
                if (pkg == null) it.remove(SHOWS_WHEN_EMPTY_CLICK_PACKAGE) else it[SHOWS_WHEN_EMPTY_CLICK_PACKAGE] = pkg
            }
            if (obj.has("centerShortcuts") && !obj.isNull("centerShortcuts")) {
                it[CENTER_SHORTCUTS] = obj.getString("centerShortcuts")
            }
            if (obj.has("centerShowLabels")) it[CENTER_SHOW_LABELS] = obj.getBoolean("centerShowLabels")
            if (obj.has("centerFillContainers")) it[CENTER_FILL_CONTAINERS] = obj.getBoolean("centerFillContainers")
            if (obj.has("centerThemedIcons")) it[CENTER_THEMED_ICONS] = obj.getBoolean("centerThemedIcons")
            if (obj.has("vibrateOnTap")) it[VIBRATE_ON_TAP] = obj.getBoolean("vibrateOnTap")
        }
    }

    /** Reads [field] as the name of enum [T]; returns null if absent, JSON-null, or an unknown name. */
    private inline fun <reified T : Enum<T>> parseEnum(obj: JSONObject, field: String): T? {
        if (!obj.has(field) || obj.isNull(field)) return null
        return runCatching { enumValueOf<T>(obj.optString(field)) }.getOrNull()
    }

    suspend fun setCutoutEnabled(enabled: Boolean) = context.behaviourDataStore.edit {
        it[CUTOUT_ENABLED] = enabled
    }

    suspend fun setHideOnLockscreen(enabled: Boolean) = context.behaviourDataStore.edit {
        it[HIDE_ON_LOCKSCREEN] = enabled
    }

    suspend fun setHideInLandscape(enabled: Boolean) = context.behaviourDataStore.edit {
        it[HIDE_IN_LANDSCAPE] = enabled
        if (enabled) {
            it[HORIZONTAL_CUTOUT_MODE] = HorizontalCutoutMode.HIDDEN.name
        }
    }

    suspend fun setHorizontalCutoutMode(mode: HorizontalCutoutMode) = context.behaviourDataStore.edit {
        it[HORIZONTAL_CUTOUT_MODE] = mode.name
        it[HIDE_IN_LANDSCAPE] = (mode == HorizontalCutoutMode.HIDDEN)
    }

    suspend fun setAnimationStyle(style: AnimationStyle) = context.behaviourDataStore.edit {
        it[ANIMATION_STYLE] = style.name
    }

    suspend fun setAnimationSpeed(speed: AnimationSpeed) = context.behaviourDataStore.edit {
        it[ANIMATION_SPEED] = speed.name
    }

    suspend fun setAnimationBounce(bounce: AnimationBounce) = context.behaviourDataStore.edit {
        it[ANIMATION_BOUNCE] = bounce.name
    }

    suspend fun setActionButtonAnimation(animation: ActionButtonAnimation) = context.behaviourDataStore.edit {
        it[ACTION_BUTTON_ANIMATION] = animation.name
    }

    suspend fun setAnimationDurationMs(ms: Int) = context.behaviourDataStore.edit {
        it[ANIMATION_DURATION_MS] = ms.coerceIn(
            BehaviourSettings.MIN_ANIMATION_DURATION_MS,
            BehaviourSettings.MAX_ANIMATION_DURATION_MS,
        )
    }

    suspend fun setNormalDurationSeconds(seconds: Int) = context.behaviourDataStore.edit {
        it[NORMAL_SECONDS] = seconds.coerceIn(
            BehaviourSettings.MIN_NORMAL_SECONDS,
            BehaviourSettings.MAX_NORMAL_SECONDS,
        )
    }

    suspend fun setAutoCollapse(enabled: Boolean) = context.behaviourDataStore.edit {
        it[AUTO_COLLAPSE] = enabled
    }

    suspend fun setCollapseSeconds(seconds: Int) = context.behaviourDataStore.edit {
        it[COLLAPSE_SECONDS] = seconds.coerceIn(
            BehaviourSettings.MIN_COLLAPSE_SECONDS,
            BehaviourSettings.MAX_COLLAPSE_SECONDS,
        )
    }

    suspend fun setDisappearOnShrink(enabled: Boolean) = context.behaviourDataStore.edit {
        it[DISAPPEAR_ON_SHRINK] = enabled
    }

    suspend fun setNotificationsAutoExpand(enabled: Boolean) = context.behaviourDataStore.edit {
        it[NOTIF_AUTO_EXPAND] = enabled
    }

    suspend fun setIgnoreSilentNotifications(enabled: Boolean) = context.behaviourDataStore.edit {
        it[IGNORE_SILENT_NOTIFICATIONS] = enabled
    }

    suspend fun setShowActionButtons(enabled: Boolean) = context.behaviourDataStore.edit {
        it[SHOW_ACTION_BUTTONS] = enabled
    }

    suspend fun setToastOnAction(enabled: Boolean) = context.behaviourDataStore.edit {
        it[TOAST_ON_ACTION] = enabled
    }

    suspend fun setShrinkOnSwipeUp(enabled: Boolean) = context.behaviourDataStore.edit {
        it[SHRINK_ON_SWIPE_UP] = enabled
    }

    suspend fun setSwipeToDismiss(enabled: Boolean) = context.behaviourDataStore.edit {
        it[SWIPE_TO_DISMISS] = enabled
    }

    suspend fun setSwipeDismissDirection(direction: SwipeDismissDirection) = context.behaviourDataStore.edit {
        it[SWIPE_DISMISS_DIRECTION] = direction.name
    }

    suspend fun setSwipeDismissTarget(target: SwipeDismissTarget) = context.behaviourDataStore.edit {
        it[SWIPE_DISMISS_TARGET] = target.name
    }

    suspend fun setShowsWhenEmpty(enabled: Boolean) = context.behaviourDataStore.edit {
        it[SHOWS_WHEN_EMPTY] = enabled
    }

    suspend fun setShowsWhenEmptyShowIcon(enabled: Boolean) = context.behaviourDataStore.edit {
        it[SHOWS_WHEN_EMPTY_SHOW_ICON] = enabled
    }

    suspend fun setShowsWhenEmptyIcon(icon: IconSource) = context.behaviourDataStore.edit {
        it[SHOWS_WHEN_EMPTY_ICON] = icon.encode()
    }

    /** Drop the chosen icon so the empty pill shows no glyph. */
    suspend fun clearShowsWhenEmptyIcon() = context.behaviourDataStore.edit {
        it.remove(SHOWS_WHEN_EMPTY_ICON)
    }

    suspend fun setShowsWhenEmptyIconColor(color: CutoutColor?) = context.behaviourDataStore.edit {
        if (color == null) it.remove(SHOWS_WHEN_EMPTY_ICON_COLOR)
        else it[SHOWS_WHEN_EMPTY_ICON_COLOR] = color.serialize()
    }

    suspend fun setShowsWhenEmptyClickAction(action: EmptyClickAction) = context.behaviourDataStore.edit {
        it[SHOWS_WHEN_EMPTY_CLICK_ACTION] = action.name
    }

    suspend fun setShowsWhenEmptyClickPackage(packageName: String?) = context.behaviourDataStore.edit {
        if (packageName == null) it.remove(SHOWS_WHEN_EMPTY_CLICK_PACKAGE)
        else it[SHOWS_WHEN_EMPTY_CLICK_PACKAGE] = packageName
    }

    /** Persist the ordered set of shortcuts shown in the expanded "center". */
    suspend fun setCenterShortcuts(shortcuts: List<CenterShortcut>) = context.behaviourDataStore.edit {
        it[CENTER_SHORTCUTS] = CenterShortcut.encodeList(shortcuts)
    }

    /** Whether each center shortcut shows its name beneath it. */
    suspend fun setCenterShowLabels(enabled: Boolean) = context.behaviourDataStore.edit {
        it[CENTER_SHOW_LABELS] = enabled
    }

    /** Whether each center shortcut's coloured container fills its slot (pill) or stays a disc. */
    suspend fun setCenterFillContainers(enabled: Boolean) = context.behaviourDataStore.edit {
        it[CENTER_FILL_CONTAINERS] = enabled
    }

    /** Whether app shortcuts in the center use their themed (monochrome) icon. */
    suspend fun setCenterThemedIcons(enabled: Boolean) = context.behaviourDataStore.edit {
        it[CENTER_THEMED_ICONS] = enabled
    }

    /** Sets whether the cutout vibrates on tap */
    suspend fun setVibrateOnTap(enabled: Boolean) = context.behaviourDataStore.edit {
        it[VIBRATE_ON_TAP] = enabled
    }

    private companion object {
        val CUTOUT_ENABLED = booleanPreferencesKey("cutout_enabled")
        val HIDE_ON_LOCKSCREEN = booleanPreferencesKey("hide_on_lockscreen")
        val HIDE_IN_LANDSCAPE = booleanPreferencesKey("hide_in_landscape")
        val HORIZONTAL_CUTOUT_MODE = stringPreferencesKey("horizontal_cutout_mode")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        val ANIMATION_BOUNCE = stringPreferencesKey("animation_bounce")
        val ACTION_BUTTON_ANIMATION = stringPreferencesKey("action_button_animation")
        val ANIMATION_DURATION_MS = intPreferencesKey("animation_duration_ms")
        val NORMAL_SECONDS = intPreferencesKey("normal_duration_seconds")
        val AUTO_COLLAPSE = booleanPreferencesKey("expanded_auto_collapse")
        val COLLAPSE_SECONDS = intPreferencesKey("expanded_collapse_seconds")
        val DISAPPEAR_ON_SHRINK = booleanPreferencesKey("expanded_disappear_on_shrink")
        val NOTIF_AUTO_EXPAND = booleanPreferencesKey("notifications_auto_expand")
        val IGNORE_SILENT_NOTIFICATIONS = booleanPreferencesKey("ignore_silent_notifications")
        val SHOW_ACTION_BUTTONS = booleanPreferencesKey("show_action_buttons")
        val TOAST_ON_ACTION = booleanPreferencesKey("toast_on_action")
        val SHRINK_ON_SWIPE_UP = booleanPreferencesKey("shrink_on_swipe_up")
        val SWIPE_TO_DISMISS = booleanPreferencesKey("swipe_to_dismiss")
        val SWIPE_DISMISS_DIRECTION = stringPreferencesKey("swipe_dismiss_direction")
        val SWIPE_DISMISS_TARGET = stringPreferencesKey("swipe_dismiss_target")
        val SHOWS_WHEN_EMPTY = booleanPreferencesKey("shows_when_empty")
        val SHOWS_WHEN_EMPTY_SHOW_ICON = booleanPreferencesKey("shows_when_empty_show_icon")
        val SHOWS_WHEN_EMPTY_ICON = stringPreferencesKey("shows_when_empty_icon")
        val SHOWS_WHEN_EMPTY_ICON_COLOR = stringPreferencesKey("shows_when_empty_icon_color")
        val SHOWS_WHEN_EMPTY_CLICK_ACTION = stringPreferencesKey("shows_when_empty_click_action")
        val SHOWS_WHEN_EMPTY_CLICK_PACKAGE = stringPreferencesKey("shows_when_empty_click_package")
        val CENTER_SHORTCUTS = stringPreferencesKey("center_shortcuts")
        val CENTER_SHOW_LABELS = booleanPreferencesKey("center_show_labels")
        val CENTER_FILL_CONTAINERS = booleanPreferencesKey("center_fill_containers")
        val CENTER_THEMED_ICONS = booleanPreferencesKey("center_themed_icons")
        val VIBRATE_ON_TAP = booleanPreferencesKey("vibrate_on_tap")
    }
}
