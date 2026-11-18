package com.ekoehler.expressivecutout.events

import android.app.Notification
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.ekoehler.expressivecutout.core.CutoutSignal

/**
 * Everything the timer tile needs, pulled out of a clock app's ongoing timer or stopwatch
 * notification. A running one carries [endElapsedRealtimeMs], the [SystemClock.elapsedRealtime]
 * point of its zero (the end of a count-down, the start of a count-up); a paused one carries a
 * frozen [pausedRemainingMs] instead (the time left, or the time elapsed when [countsUp]). Keeps
 * the notification plumbing out of the listener service.
 */
data class ParsedTimer(
    val endElapsedRealtimeMs: Long?,
    val pausedRemainingMs: Long?,
    val label: String?,
    val actions: List<CutoutSignal.Notification.Action>,
    val countsUp: Boolean = false,
)

/**
 * Recognises and reads timer and stopwatch notifications across the two ways Android surfaces them:
 *
 *  - **Live Updates (Android 16+):** a "promoted ongoing" notification rendered with the
 *    `MetricStyle` template, whose critical metric is a time in either direction. Google Clock and
 *    TickTick use this; the time lives in the metric's `value` bundle (`zeroElapsedRealtime` while
 *    running, `pausedDuration` while paused) — not in any title/text or the classic chronometer
 *    extras.
 *  - **Classic chronometer:** the older format that sets a *counting-down* chronometer anchored to
 *    the notification's `when`. Kept as a fallback for clock apps that still use it; it stays
 *    countdown-only so a call's count-up chronometer is not mistaken for a timer.
() *  - **Clock's shared extras:** BlackyHawky Clock also publishes `timerIsRunning` and
 *    `timerRemainingMs` for third-party apps. Its paused timer carries no chronometer and no
 *    metric, so these extras are the only way to see it; only the paused form is read from them.
 *
 * Keying off the metric's time (rather than a package allow-list) works across clock apps.
 */
object TimerNotificationParser {

    /**
     * Whether this notification is a timer or stopwatch, by either the modern metric extras (any
     * direction) or the classic count-down chronometer.
     */
    fun isTimer(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val extras = notification.extras ?: return false
        return readMetricTimer(extras) != null ||
            isClassicCountdown(notification, extras) ||
            readClockPausedRemaining(sbn.packageName, extras) != null
    }

    /**
     * Whether a timer may claim the tile. Clock re-posts every timer's notification on each change,
     * so a paused timer must not steal the tile from the timer already shown under another key.
     */
    internal fun mayTakeTile(isPaused: Boolean, key: String, currentKey: String?): Boolean =
        !isPaused || currentKey == null || key == currentKey

    /**
     * Reads a timer notification into the shape the timer tile needs: time, direction, label, and
     * the pause/stop actions.
     */
    fun parse(sbn: StatusBarNotification): ParsedTimer {
        val notification = sbn.notification
        val extras = notification.extras
        val actions = notification.actions.orEmpty().mapNotNull { action ->
            val label = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val intent = action.actionIntent ?: return@mapNotNull null
            CutoutSignal.Notification.Action(label, intent)
        }

        val metric = extras?.let { readMetricTimer(it) }
        if (metric != null) {
            return ParsedTimer(
                endElapsedRealtimeMs = metric.endElapsedRealtimeMs,
                pausedRemainingMs = metric.pausedRemainingMs,
                label = metric.label ?: extras.notificationTitle(),
                actions = actions,
                countsUp = metric.countsUp,
            )
        }

        val clockPausedMs = extras?.let { readClockPausedRemaining(sbn.packageName, it) }
        
        if (clockPausedMs != null) {
            return ParsedTimer(
                endElapsedRealtimeMs = null,
                pausedRemainingMs = clockPausedMs,
                label = extras.notificationTitle(),
                actions = actions,
                countsUp = false,
            )
        }

        // Classic chronometer: `when` is the wall-clock zero point; convert to the elapsed-realtime
        // base the tile ticks against so both formats flow through the same running-timer state.
        val endWallMs = notification.`when`
        val endElapsedMs = SystemClock.elapsedRealtime() + (endWallMs - System.currentTimeMillis())
        return ParsedTimer(
            endElapsedRealtimeMs = endElapsedMs,
            pausedRemainingMs = null,
            label = extras?.notificationTitle(),
            actions = actions,
            countsUp = false,
        )
    }

    /**
     * The critical metric of a `MetricStyle` notification as a timer (count-down or count-up), or
     * null if it isn't one.
     */
    private fun readMetricTimer(extras: Bundle): MetricTimer? {
        @Suppress("DEPRECATION")
        val metrics = extras.get(KEY_METRICS) as? List<*> ?: return null
        if (metrics.isEmpty()) return null
        val criticalIndex = extras.getInt(KEY_CRITICAL_INDEX, 0)
        val metric = (metrics.getOrNull(criticalIndex) ?: metrics.firstOrNull()) as? Bundle ?: return null
        val value = metric.getBundle(KEY_VALUE) ?: return null
        val countsUp = !value.getBoolean(KEY_COUNT_DOWN, false)

        val label = metric.getString(KEY_LABEL)?.takeIf { it.isNotBlank() }
        val zeroElapsed = value.getLong(KEY_ZERO_ELAPSED, -1L)
        if (zeroElapsed > 0L) {
            return MetricTimer(endElapsedRealtimeMs = zeroElapsed, pausedRemainingMs = null, label = label, countsUp = countsUp)
        }
        if (value.containsKey(KEY_PAUSED_DURATION)) {
            val paused = value.getLong(KEY_PAUSED_DURATION, 0L).coerceAtLeast(0L)
            return MetricTimer(endElapsedRealtimeMs = null, pausedRemainingMs = paused, label = label, countsUp = countsUp)
        }
        return null
    }

    /**
     * Recognises the pre-metric form of a timer: a chronometer counting *down* from an anchored
     * start. Both flags are needed, since a count-up chronometer is a call and a hidden one is a
     * plain notification.
     */
    private fun isClassicCountdown(notification: Notification, extras: Bundle): Boolean {
        val countsDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val showsChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        return countsDown && showsChronometer && notification.`when` > 0L
    }

    /**
     * The frozen remaining time of a paused BlackyHawky Clock timer, read from its shared extras, or
     * null when either extra is missing or the timer is running (the other formats cover that).
     */
    private fun readClockPausedRemaining(packageName: String, extras: Bundle): Long? {
        val runningKey = packageName + SUFFIX_TIMER_IS_RUNNING
        val remainingKey = packageName + SUFFIX_TIMER_REMAINING_MS
        
        if (!extras.containsKey(runningKey) || !extras.containsKey(remainingKey)) {
            return null
        }

        if (extras.getBoolean(runningKey)) {
            return null
        }
        
        return extras.getLong(remainingKey).coerceAtLeast(0L)
    }

    private fun Bundle.notificationTitle(): String? =
        getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }

    /**
     * The timer state a modern clock app publishes directly, before it is turned into a tile.
     * Either running (with its zero point: the end of a count-down, the start of a count-up) or
     * paused (with the remaining or elapsed time frozen), plus the direction it counts.
     */
    private data class MetricTimer(
        val endElapsedRealtimeMs: Long?,
        val pausedRemainingMs: Long?,
        val label: String?,
        val countsUp: Boolean,
    )

    /**
     * Keys the MetricStyle template stores its data under (mirrored by androidx
     * NotificationCompat's MetricStyle). Read defensively — any missing key just means "not a
     * timer we can read".
     */
    private const val KEY_METRICS = "android.metrics"
    private const val KEY_CRITICAL_INDEX = "android.metrics.criticalIndex"
    private const val KEY_LABEL = "label"
    private const val KEY_VALUE = "value"
    private const val KEY_COUNT_DOWN = "countDown"
    private const val KEY_ZERO_ELAPSED = "zeroElapsedRealtime"
    private const val KEY_PAUSED_DURATION = "pausedDuration"

    /**
     * BlackyHawky Clock's shared timer extras. Each key is the posting app's package name followed
     * by one of these suffixes, e.g. `com.best.deskclock.nightly.timerIsRunning`.
     */
    private const val SUFFIX_TIMER_IS_RUNNING = ".timerIsRunning"
    private const val SUFFIX_TIMER_REMAINING_MS = ".timerRemainingMs"
}
