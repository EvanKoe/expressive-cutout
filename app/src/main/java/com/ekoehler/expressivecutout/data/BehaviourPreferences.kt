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

/** Which horizontal swipe directions dismiss the cutout, when swipe-to-dismiss is enabled. */
enum class SwipeDismissDirection { LEFT, RIGHT, BOTH }

/**
 * Which cutout state swipe-to-dismiss applies to. Ordered to match the settings selector
 * (Expanded / Both / Normal) so the ordinal doubles as the segment index.
 */
enum class SwipeDismissTarget { EXPANDED, BOTH, NORMAL }

/**
 * How the island behaves once expanded. [expandedAutoCollapse] chooses between collapsing after
 * [expandedCollapseSeconds] or staying until tapped. When that shrink happens,
 * [expandedDisappearOnShrink] decides whether the island disappears entirely (true) or returns
 * to the normal cutout (false).
 */
data class BehaviourSettings(
    val cutoutEnabled: Boolean = DEFAULT_CUTOUT_ENABLED,
    val normalDurationSeconds: Int = DEFAULT_NORMAL_SECONDS,
    val expandedAutoCollapse: Boolean = DEFAULT_AUTO_COLLAPSE,
    val expandedCollapseSeconds: Int = DEFAULT_COLLAPSE_SECONDS,
    val expandedDisappearOnShrink: Boolean = DEFAULT_DISAPPEAR_ON_SHRINK,
    val notificationsAutoExpand: Boolean = DEFAULT_NOTIFICATIONS_AUTO_EXPAND,
    val showActionButtons: Boolean = DEFAULT_SHOW_ACTION_BUTTONS,
    val shrinkOnSwipeUp: Boolean = DEFAULT_SHRINK_ON_SWIPE_UP,
    val swipeToDismiss: Boolean = DEFAULT_SWIPE_TO_DISMISS,
    val swipeDismissDirection: SwipeDismissDirection = DEFAULT_SWIPE_DISMISS_DIRECTION,
    val swipeDismissTarget: SwipeDismissTarget = DEFAULT_SWIPE_DISMISS_TARGET,
) {
    companion object {
        const val DEFAULT_CUTOUT_ENABLED = true
        const val DEFAULT_NORMAL_SECONDS = 3
        const val DEFAULT_AUTO_COLLAPSE = true
        const val DEFAULT_COLLAPSE_SECONDS = 5
        const val DEFAULT_DISAPPEAR_ON_SHRINK = false
        const val DEFAULT_NOTIFICATIONS_AUTO_EXPAND = false
        const val DEFAULT_SHOW_ACTION_BUTTONS = true
        const val DEFAULT_SHRINK_ON_SWIPE_UP = true
        const val DEFAULT_SWIPE_TO_DISMISS = true
        val DEFAULT_SWIPE_DISMISS_DIRECTION = SwipeDismissDirection.BOTH
        val DEFAULT_SWIPE_DISMISS_TARGET = SwipeDismissTarget.BOTH
        const val MIN_NORMAL_SECONDS = 1
        const val MAX_NORMAL_SECONDS = 10
        const val MIN_COLLAPSE_SECONDS = 1
        const val MAX_COLLAPSE_SECONDS = 15
    }
}

/** Persists [BehaviourSettings], always emitting a clamped collapse delay. */
class BehaviourPreferences(private val context: Context) {

    val settings: Flow<BehaviourSettings> = context.behaviourDataStore.data.map { prefs ->
        BehaviourSettings(
            cutoutEnabled = prefs[CUTOUT_ENABLED] ?: BehaviourSettings.DEFAULT_CUTOUT_ENABLED,
            normalDurationSeconds = (prefs[NORMAL_SECONDS] ?: BehaviourSettings.DEFAULT_NORMAL_SECONDS)
                .coerceIn(BehaviourSettings.MIN_NORMAL_SECONDS, BehaviourSettings.MAX_NORMAL_SECONDS),
            expandedAutoCollapse = prefs[AUTO_COLLAPSE] ?: BehaviourSettings.DEFAULT_AUTO_COLLAPSE,
            expandedCollapseSeconds = (prefs[COLLAPSE_SECONDS] ?: BehaviourSettings.DEFAULT_COLLAPSE_SECONDS)
                .coerceIn(BehaviourSettings.MIN_COLLAPSE_SECONDS, BehaviourSettings.MAX_COLLAPSE_SECONDS),
            expandedDisappearOnShrink = prefs[DISAPPEAR_ON_SHRINK] ?: BehaviourSettings.DEFAULT_DISAPPEAR_ON_SHRINK,
            notificationsAutoExpand = prefs[NOTIF_AUTO_EXPAND] ?: BehaviourSettings.DEFAULT_NOTIFICATIONS_AUTO_EXPAND,
            showActionButtons = prefs[SHOW_ACTION_BUTTONS] ?: BehaviourSettings.DEFAULT_SHOW_ACTION_BUTTONS,
            shrinkOnSwipeUp = prefs[SHRINK_ON_SWIPE_UP] ?: BehaviourSettings.DEFAULT_SHRINK_ON_SWIPE_UP,
            swipeToDismiss = prefs[SWIPE_TO_DISMISS] ?: BehaviourSettings.DEFAULT_SWIPE_TO_DISMISS,
            swipeDismissDirection = prefs[SWIPE_DISMISS_DIRECTION]
                ?.let { runCatching { SwipeDismissDirection.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_SWIPE_DISMISS_DIRECTION,
            swipeDismissTarget = prefs[SWIPE_DISMISS_TARGET]
                ?.let { runCatching { SwipeDismissTarget.valueOf(it) }.getOrNull() }
                ?: BehaviourSettings.DEFAULT_SWIPE_DISMISS_TARGET,
        )
    }

    suspend fun setCutoutEnabled(enabled: Boolean) = context.behaviourDataStore.edit {
        it[CUTOUT_ENABLED] = enabled
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

    private companion object {
        val CUTOUT_ENABLED = booleanPreferencesKey("cutout_enabled")
        val NORMAL_SECONDS = intPreferencesKey("normal_duration_seconds")
        val AUTO_COLLAPSE = booleanPreferencesKey("expanded_auto_collapse")
        val COLLAPSE_SECONDS = intPreferencesKey("expanded_collapse_seconds")
        val DISAPPEAR_ON_SHRINK = booleanPreferencesKey("expanded_disappear_on_shrink")
        val NOTIF_AUTO_EXPAND = booleanPreferencesKey("notifications_auto_expand")
        val SHOW_ACTION_BUTTONS = booleanPreferencesKey("show_action_buttons")
        val SHRINK_ON_SWIPE_UP = booleanPreferencesKey("shrink_on_swipe_up")
        val SWIPE_TO_DISMISS = booleanPreferencesKey("swipe_to_dismiss")
        val SWIPE_DISMISS_DIRECTION = stringPreferencesKey("swipe_dismiss_direction")
        val SWIPE_DISMISS_TARGET = stringPreferencesKey("swipe_dismiss_target")
    }
}
