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
) {
    companion object {
        const val DEFAULT_CUTOUT_ENABLED = true
        const val DEFAULT_HIDE_ON_LOCKSCREEN = false
        const val DEFAULT_HIDE_IN_LANDSCAPE = false
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
    }
}

/** Persists [BehaviourSettings], always emitting a clamped collapse delay. */
class BehaviourPreferences(private val context: Context) {

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
        )
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
    }
}
