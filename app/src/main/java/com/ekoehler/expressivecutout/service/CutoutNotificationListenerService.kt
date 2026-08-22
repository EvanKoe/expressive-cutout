package com.ekoehler.expressivecutout.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.MediaArt
import com.ekoehler.expressivecutout.core.MediaArtBus
import com.ekoehler.expressivecutout.core.OnCall
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimer
import com.ekoehler.expressivecutout.core.RunningTimerBus
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

    override fun onListenerConnected() {
        instance = this
        _bound.value = true
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        _bound.value = false
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        _bound.value = false
        super.onDestroy()
    }

    /**
     * Checks if the notification is a progress one
     */
    fun isProgressNotification(sbn: StatusBarNotification): Boolean {
        return getProgressDataOrNull(sbn) != null
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
        onNotificationPosted(sbn, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
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

        val isSilent = isSilentNotification(notification, rankingMap)
        val extras = notification.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val progress = getProgressDataOrNull(sbn)
        val pm = packageManager
        val appName = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(notification.packageName, 0)).toString()
        }.getOrNull()
        val postTimeMs = if (notification.postTime > 0) notification.postTime else System.currentTimeMillis()

        IslandEventBus.emit(
            CutoutSignal.Notification(
                packageName = notification.packageName,
                title = title,
                text = text,
                appName = appName,
                postTimeMs = postTimeMs,
                key = notification.key,
                contentIntent = notification.notification.contentIntent,
                actions = notification.notification.surfaceableActions(),
                largeIcon = notification.notification.getLargeIcon(),
                smallIcon = notification.notification.smallIcon,
                progressData = progress,
                isSilent = isSilent,
            ),
        )
    }

    /**
     * Determines whether [sbn] represents a silent notification.
     */
    fun isSilentNotification(sbn: StatusBarNotification, rankingMap: RankingMap? = null): Boolean {
        val ranking = Ranking()
        val found = (rankingMap != null && rankingMap.getRanking(sbn.key, ranking)) ||
            (currentRanking != null && currentRanking.getRanking(sbn.key, ranking))
        if (found) {
            val importance = ranking.importance
            val isAmbient = ranking.isAmbient
            val priority = sbn.notification?.priority ?: Notification.PRIORITY_DEFAULT
            return NotificationClassifier.isSilent(
                importance = importance,
                isAmbient = isAmbient,
                priority = priority,
            )
        }
        val priority = sbn.notification?.priority ?: Notification.PRIORITY_DEFAULT
        return NotificationClassifier.isSilent(
            importance = null,
            isAmbient = false,
            priority = priority,
        )
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
            runCatching { service.cancelNotification(key) }
                .onFailure { Log.w(TAG, "Failed to cancel notification $key", it) }
        }
    }
}
