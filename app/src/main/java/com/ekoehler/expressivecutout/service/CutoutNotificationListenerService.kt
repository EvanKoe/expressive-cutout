package com.ekoehler.expressivecutout.service

import android.app.KeyguardManager
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.MediaArt
import com.ekoehler.expressivecutout.core.MediaArtBus
import com.ekoehler.expressivecutout.core.OnCall
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimer
import com.ekoehler.expressivecutout.core.RunningTimerBus
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.events.CallNotificationParser
import com.ekoehler.expressivecutout.events.TimerNotificationParser
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull


/**
 * This is a progress data class to store progress notification extra data
 */
data class ProgressData(
    val max: Int = 0,
    val current: Int = 0,
    val isIndeterminate: Boolean = false,
    val title: String? = null
)

/**
 * Mirrors freshly posted notifications onto the island. It keeps only the posting package,
 * title and text (shown when the island is expanded) and filters out noise (its own posts,
 * group summaries, and ongoing/system-managed notifications).
 *
 * It also lets the overlay dismiss the real notification (not just the pill): the connected
 * instance is published statically so [dismiss] can cancel a notification by key on the user's
 * swipe, mirroring a swipe-away in the shade.
 *
 * When the user asked for the shade to be cleared automatically, a mirrored notification is
 * *snoozed* rather than cancelled — see [snooze]. Cancelling outright loses the notification for
 * good if nobody happened to be looking at the island while the pill was up; snoozing keeps the
 * shade quiet during that window and hands the notification back afterwards, so only an explicit
 * interaction ([dismiss] or [settle]) really destroys it.
 */
class CutoutNotificationListenerService : NotificationListenerService() {

    // Key of the call notification currently driving the phone tile, so we pop the island only once
    // per call and can clear the tile when that exact notification is removed. Main-thread only.
    private var currentCallKey: String? = null

    // Key of the count-down notification currently driving the timer tile, mirroring [currentCallKey].
    private var currentTimerKey: String? = null

    // Key of the media notification the current album cover was lifted from, so the cover is
    // dropped when that exact notification goes away. Mirrors [currentCallKey].
    private var currentMediaArtKey: String? = null

    // Key of the assistant notification currently driving the assistant tile.
    private var currentAssistantKey: String? = null

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val behaviourPreferences by lazy { BehaviourPreferences(this) }

    private var behaviourJob: Job? = null

    // Mirror of BehaviourSettings.dismissNotifications, cached because onNotificationPosted runs on
    // the main thread and must not wait on a DataStore read. Main-thread only, like the key fields.
    private var dismissNotifications = BehaviourSettings.DEFAULT_DISMISS_NOTIFICATIONS

    // Mirror of BehaviourSettings.displayWhileDnd, cached alongside [dismissNotifications] and for
    // the same reason. Main-thread only, like the key fields.
    private var displayWhileDnd = BehaviourSettings.DEFAULT_DISPLAY_WHILE_DND

    // How long a mirrored notification is kept out of the shade, derived from how long the island
    // actually shows it. Cached alongside [dismissNotifications] and for the same reason.
    private var snoozeDurationMs = SNOOZE_GRACE_MS

    // Keys we snoozed ourselves mapped to the elapsed-realtime by which the framework's re-post is
    // due, so that re-post can be told apart from a fresh notification and passed through to the
    // shade untouched. Timestamped rather than a bare set because notification keys are recycled:
    // an entry that outlived its window must not swallow the pill of a genuinely new notification
    // reusing the key. Insertion-ordered and capped at [MAX_TRACKED_SNOOZES]. Main-thread only.
    private val snoozedUntil = LinkedHashMap<String, Long>()

    override fun onListenerConnected() {
        instance = this
        _bound.value = true
        observeBehaviour()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        _bound.value = false
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        _bound.value = false
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Keep [dismissNotifications] and [snoozeDurationMs] in step with the stored behaviour settings.
     * The collection outlives a disconnect (the framework re-uses the same instance on rebind, and a
     * cancelled scope could not be restarted), so it is only torn down in [onDestroy] and re-entry is
     * a no-op.
     */
    private fun observeBehaviour() {
        if (behaviourJob?.isActive == true) return
        behaviourJob = scope.launch {
            behaviourPreferences.settings
                .distinctUntilChanged()
                .collect { settings ->
                    dismissNotifications = settings.dismissNotifications
                    snoozeDurationMs = settings.snoozeDurationMs()
                    displayWhileDnd = settings.displayWhileDnd
                }
        }
    }

    /**
     * How long to hold a notification out of the shade: the island's own pill lifetime plus
     * [SNOOZE_GRACE_MS], so the shade stays quiet for exactly as long as the pill is up and the
     * notification lands moments after it fades rather than a minute later. An auto-expanded pill
     * shows expanded first and only then counts down as a collapsed one, so both spans are counted.
     */
    private fun BehaviourSettings.snoozeDurationMs(): Long {
        val expandedMs = if (notificationsAutoExpand && expandedAutoCollapse) {
            expandedCollapseSeconds * 1000L
        } else {
            0L
        }
        return expandedMs + normalDurationSeconds * 1000L + SNOOZE_GRACE_MS
    }

    /**
     * Whether Do Not Disturb should keep this notification off the island. Reads the effective
     * interruption filter rather than Settings.Global.zen_mode: that global only tracks the manual
     * toggle, so a filter in force through a schedule, an automatic rule or a Mode leaves it at zero
     * and the gate never closes. The filter is matched explicitly because
     * [INTERRUPTION_FILTER_UNKNOWN] sorts *below* [INTERRUPTION_FILTER_ALL] — "not yet known" must
     * not read as either "no DND" or "DND on".
     *
     * Calls and timers are deliberately handled before this check ever runs: they break through DND
     * by design, and an island that hid an incoming call would be worse than no island at all.
     */
    private fun suppressedByDnd(): Boolean {
        if (displayWhileDnd) return false
        return when (currentInterruptionFilter) {
            INTERRUPTION_FILTER_PRIORITY,
            INTERRUPTION_FILTER_ALARMS,
            INTERRUPTION_FILTER_NONE -> true
            else -> false
        }
    }

    /**
     * Whether the user could plausibly have been looking at the island when a notification landed.
     * A dark or locked screen never showed the pill, so those notifications are left in the shade
     * untouched rather than snoozed — there is nothing to have missed them *from*.
     */
    private fun isUserPresent(): Boolean {
        val power = getSystemService(PowerManager::class.java) ?: return false
        val keyguard = getSystemService(KeyguardManager::class.java) ?: return false
        return power.isInteractive && !keyguard.isKeyguardLocked
    }

    /**
     * Pull [key] out of the shade for [snoozeDurationMs] while the island mirrors it. The framework
     * re-posts it afterwards, so an unnoticed notification comes back on its own; only an explicit
     * interaction on the pill ([dismiss] or [settle]) cancels it for good. The key is remembered so
     * that re-post can be recognised, and only on success — a refused snooze leaves the notification
     * exactly where it is.
     */
    private fun snooze(key: String) {
        val duration = snoozeDurationMs
        runCatching { snoozeNotification(key, duration) }
            .onSuccess { rememberSnooze(key, SystemClock.elapsedRealtime() + duration) }
            .onFailure { Log.w(TAG, "Failed to snooze notification $key", it) }
    }

    /**
     * Track [key] until [dueAt], dropping entries whose window has already passed so a recycled key
     * can never be mistaken for a snooze that never came back.
     */
    private fun rememberSnooze(key: String, dueAt: Long) {
        val now = SystemClock.elapsedRealtime()
        snoozedUntil.entries.removeAll { it.value < now }
        snoozedUntil[key] = dueAt
        while (snoozedUntil.size > MAX_TRACKED_SNOOZES) {
            snoozedUntil.remove(snoozedUntil.keys.first())
        }
    }

    /**
     * Whether this post is the framework handing back a notification we snoozed, rather than a new
     * one. Consumes the entry either way: a key that turns up later than its window is a fresh
     * notification that happens to reuse it, and deserves its pill.
     */
    private fun isSnoozeReturning(key: String): Boolean {
        val dueAt = snoozedUntil.remove(key) ?: return false
        return SystemClock.elapsedRealtime() <= dueAt + SNOOZE_GRACE_MS
    }

    /**
     * Returns progress data from a notification (or null if no progress).
     *
     * The progress extras are written by [Notification.Builder] for every notification, whether or
     * not setProgress() was ever called, so their presence proves nothing. Only a positive max
     * (determinate) or the indeterminate flag marks a notification as actually carrying progress.
     */
    fun getProgressDataOrNull(sbn: StatusBarNotification): ProgressData? {
        val extras = sbn.notification?.extras ?: return null
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        if (!isIndeterminate && max <= 0) return null

        return ProgressData(
            max = max,
            current = current.coerceIn(0, max.coerceAtLeast(0)),
            isIndeterminate = isIndeterminate,
            title = title
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return

        // Phone call
        if (CallNotificationParser.isCall(notification)) {
            handleCall(notification)
            return
        }

        // Timer
        if (TimerNotificationParser.isTimer(notification)) {
            handleTimer(notification)
            return
        }

        // Music
        notification.publishMediaArt()

        if (!notification.shouldSurface()) return

        if (suppressedByDnd()) return

        // Our own snooze coming back: the island already had its turn with this one, so let it
        // settle into the shade without popping a second pill moments after the first.
        if (isSnoozeReturning(notification.key)) return

        val extras = notification.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val progress = getProgressDataOrNull(sbn)
        val islandEvent = CutoutSignal.Notification(
            packageName = notification.packageName,
            title = title,
            text = text,
            key = notification.key,
            contentIntent = notification.notification.contentIntent,
            actions = notification.notification.surfaceableActions(),
            largeIcon = notification.notification.getLargeIcon(),
            smallIcon = notification.notification.smallIcon,
            progressData = progress
        )

        IslandEventBus.emit(islandEvent)

        if (dismissNotifications && isUserPresent()) {
            snooze(notification.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Clears call cutout when call ends
        if (sbn?.key != null && sbn.key == currentCallKey) {
            currentCallKey = null
            OnCallBus.update(null)
        }
        // Clears time cutout when timer finishes or reset
        if (sbn?.key != null && sbn.key == currentTimerKey) {
            currentTimerKey = null
            RunningTimerBus.update(null)
        }
        // Clears music cutout when music is paused/stopped
        if (sbn?.key != null && sbn.key == currentMediaArtKey) {
            currentMediaArtKey = null
            MediaArtBus.update(null)
        }
        super.onNotificationRemoved(sbn)
    }

    /**
     * Drive the phone tile from a call notification: keep [OnCallBus] fresh on every update (so the
     * caller and duration stay current), and pop the island only the first time a given call
     * appears — later re-posts refresh the state without re-popping, mirroring the media monitor.
     */
    private fun handleCall(sbn: StatusBarNotification) {
        val call = CallNotificationParser.parse(sbn, this)
        val prevOngoing = OnCallBus.state.value?.ongoing

        OnCallBus.update(
            OnCall(
                callerLabel = call.callerLabel,
                callerNumber = call.callerNumber,
                photo = call.photo,
                startTimeMs = call.startTimeMs,
                ongoing = call.ongoing,
                packageName = sbn.packageName,
            ),
        )

        if (sbn.key != currentCallKey || prevOngoing != call.ongoing) {
            currentCallKey = sbn.key
            IslandEventBus.emit(
                CutoutSignal.Call(
                    packageName = sbn.packageName,
                    callerLabel = call.callerLabel,
                    key = sbn.key,
                    contentIntent = sbn.notification.contentIntent,
                    actions = call.actions,
                    ongoing = call.ongoing,
                ),
            )
        }
    }

    /**
     * Drive the timer tile from a count-down notification: keep [RunningTimerBus] fresh on every
     * update (so the remaining time re-syncs when the user adds a minute) and pop the island only the
     * first time a given timer appears — later re-posts refresh the countdown without re-popping.
     * Mirrors [handleCall].
     */
    private fun handleTimer(sbn: StatusBarNotification) {
        val timer = TimerNotificationParser.parse(sbn)
        RunningTimerBus.update(
            RunningTimer(
                endElapsedRealtimeMs = timer.endElapsedRealtimeMs,
                pausedRemainingMs = timer.pausedRemainingMs,
                label = timer.label,
                actions = timer.actions,
            ),
        )
        if (sbn.key != currentTimerKey) {
            currentTimerKey = sbn.key
            IslandEventBus.emit(
                CutoutSignal.Timer(
                    packageName = sbn.packageName,
                    label = timer.label,
                    key = sbn.key,
                    contentIntent = sbn.notification.contentIntent,
                    actions = timer.actions,
                ),
            )
        }
    }

    /**
     * The action buttons worth mirroring: those with a label and a fireable intent. Reply-style
     * actions (those declaring a free-form [android.app.RemoteInput]) keep a [ReplyInput] so the
     * island can offer an inline text field.
     */
    private fun Notification.surfaceableActions(): List<CutoutSignal.Notification.Action> =
        actions.orEmpty().mapNotNull { action ->
            val title = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val intent = action.actionIntent ?: return@mapNotNull null
            CutoutSignal.Notification.Action(title, intent, action.toReplyInput())
        }

    /** Builds a [ReplyInput] if the action accepts free-form typed text, else null. */
    private fun Notification.Action.toReplyInput(): CutoutSignal.Notification.ReplyInput? {
        val inputs = remoteInputs?.toList().orEmpty()
        val freeForm = inputs.firstOrNull { it.allowFreeFormInput } ?: return null
        return CutoutSignal.Notification.ReplyInput(
            resultKey = freeForm.resultKey,
            remoteInputs = inputs,
            hint = freeForm.label?.toString(),
        )
    }

    /**
     * Publish this notification's large icon as the current album cover, if it is a media
     * notification carrying one. Detected by the media-session extra rather than the template
     * string, so it covers both the platform and the AndroidX MediaStyle. The icon is usually a
     * plain bitmap, so no package lookup is involved.
     */
    private fun StatusBarNotification.publishMediaArt() {
        if (notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) != true) return
        val art = notification.getLargeIcon()
            ?.loadImageBitmapOrNull(this@CutoutNotificationListenerService)
            ?: return
        currentMediaArtKey = key
        MediaArtBus.update(MediaArt(packageName = packageName, art = art))
    }

    private fun StatusBarNotification.shouldSurface(): Boolean {
        if (packageName == this@CutoutNotificationListenerService.packageName) return false
        val flags = notification.flags
        val isSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
        val isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        return isClearable && !isSummary && !isOngoing
    }

    companion object {
        private const val TAG = "CutoutNotifListener"

        // Padding on top of the pill's own lifetime, covering the island's animations and giving the
        // user a moment to reach a pill that has only just faded. Doubles as the slack allowed on a
        // re-post arriving late.
        private const val SNOOZE_GRACE_MS = 3_000L

        // Upper bound on [snoozedUntil]. Far above the handful that can be in flight within one
        // snooze window; purely a guard against runaway growth.
        private const val MAX_TRACKED_SNOOZES = 64

        // The currently connected listener, or null when unbound. Only touched on the main thread
        // (the framework's listener callbacks and the overlay both run there).
        @Volatile
        private var instance: CutoutNotificationListenerService? = null

        private val _bound = MutableStateFlow(false)

        /**
         * True only while Android actually has this listener bound — i.e. while notifications and
         * media sessions are really flowing. Deliberately separate from
         * [com.ekoehler.expressivecutout.permissions.Permissions.isNotificationAccessGranted], which
         * reads the user's *consent* out of Settings.Secure: that stays "enabled" across a reinstall
         * or an app update while the binding is dead, so every dynamic tile (music, phone, timer) is
         * silently starved while the grant still reads green. Mirrors
         * [com.ekoehler.expressivecutout.service.CutoutAccessibilityService.bound].
         */
        val bound: StateFlow<Boolean> = _bound.asStateFlow()

        /**
         * Ask the framework to (re)bind this listener. Android often leaves the binding dead after
         * an app update while the grant survives, and there is nothing the app can do from inside a
         * service that never connected — so this is called from the UI on resume when the grant
         * reads green but [bound] is still false, healing the stale binding without making the user
         * toggle the permission off and on by hand. A no-op if the grant isn't actually held.
         */
        fun requestRebind(context: Context) {
            val component = ComponentName(context, CutoutNotificationListenerService::class.java)
            runCatching { requestRebind(component) }
                .onFailure { Log.w(TAG, "Failed to request listener rebind", it) }
        }

        /**
         * Cancel the notification with [key] from the system, exactly as swiping it away in the
         * shade would. A no-op if the listener isn't connected (nothing we can do without it).
         */
        fun dismiss(key: String) {
            val service = instance ?: return
            service.snoozedUntil.remove(key)
            runCatching { service.cancelNotification(key) }
                .onFailure { Log.w(TAG, "Failed to cancel notification $key", it) }
        }

        /**
         * The user acted on the pill mirroring [key] — tapped it, fired an action, sent a reply — so
         * a notification we had merely snoozed has served its purpose and must not resurface. Only
         * touches keys we snoozed ourselves: one the app still owns is the app's to clear, exactly as
         * before the island got involved.
         */
        fun settle(key: String) {
            val service = instance ?: return
            if (service.snoozedUntil.remove(key) == null) return
            runCatching { service.cancelNotification(key) }
                .onFailure { Log.w(TAG, "Failed to cancel snoozed notification $key", it) }
        }
    }
}
