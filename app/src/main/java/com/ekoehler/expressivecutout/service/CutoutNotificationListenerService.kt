package com.ekoehler.expressivecutout.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.OnCall
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimer
import com.ekoehler.expressivecutout.core.RunningTimerBus
import com.ekoehler.expressivecutout.events.CallNotificationParser
import com.ekoehler.expressivecutout.events.TimerNotificationParser

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

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        // Ongoing-call notifications drive the phone tile, not the normal notification pill (and
        // would be dropped by shouldSurface below as ongoing anyway), so handle them first.
        if (CallNotificationParser.isCall(notification)) {
            handleCall(notification)
            return
        }
        // A count-down notification drives the timer tile, not the normal pill (and is ongoing, so it
        // would be dropped by shouldSurface below anyway), so handle it before the generic path.
        if (TimerNotificationParser.isTimer(notification)) {
            handleTimer(notification)
            return
        }
        if (!notification.shouldSurface()) return

        val extras = notification.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        IslandEventBus.emit(
            CutoutSignal.Notification(
                packageName = notification.packageName,
                title = title,
                text = text,
                key = notification.key,
                contentIntent = notification.notification.contentIntent,
                actions = notification.notification.surfaceableActions(),
            ),
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // The dialer clears its notification when the call ends — drop the phone tile's live state.
        if (sbn?.key != null && sbn.key == currentCallKey) {
            currentCallKey = null
            OnCallBus.update(null)
        }
        // The clock clears its notification when the timer is reset or finishes — drop the tile.
        if (sbn?.key != null && sbn.key == currentTimerKey) {
            currentTimerKey = null
            RunningTimerBus.update(null)
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
        OnCallBus.update(
            OnCall(
                callerLabel = call.callerLabel,
                photo = call.photo,
                startTimeMs = call.startTimeMs,
                ongoing = call.ongoing,
            ),
        )
        if (sbn.key != currentCallKey) {
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
