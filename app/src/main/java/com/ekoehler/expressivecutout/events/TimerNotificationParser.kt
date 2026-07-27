package com.ekoehler.expressivecutout.events

import android.app.Notification
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.ekoehler.expressivecutout.core.CutoutSignal

/**
 * Everything the timer tile needs, pulled out of a clock app's ongoing count-down notification.
 * A running timer carries [endElapsedRealtimeMs] (the [SystemClock.elapsedRealtime] point it hits
 * zero); a paused one carries a frozen [pausedRemainingMs] instead. Keeps the notification plumbing
 * out of the listener service.
 */
data class ParsedTimer(
    val endElapsedRealtimeMs: Long?,
    val pausedRemainingMs: Long?,
    val label: String?,
    val actions: List<CutoutSignal.Notification.Action>,
)

/**
 * Recognises and reads count-down timer notifications across the two ways Android surfaces them:
 *
 *  - **Live Updates (Android 16+):** a "promoted ongoing" notification rendered with the
 *    `MetricStyle` template, whose critical metric is a counting-down time. Google Clock uses this;
 *    the remaining time lives in the metric's `value` bundle (`zeroElapsedRealtime` while running,
 *    `pausedDuration` while paused) — not in any title/text or the classic chronometer extras.
 *  - **Classic chronometer:** the older format that sets a *counting-down* chronometer anchored to
 *    the notification's `when`. Kept as a fallback for clock apps that still use it.
 *
 * Keying off "a counting-down time" (rather than a package allow-list) works across clock apps and
 * never mistakes a call, whose chronometer counts *up*, for a timer.
 */
object TimerNotificationParser {

    fun isTimer(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val extras = notification.extras ?: return false
        return readMetricCountdown(extras) != null || isClassicCountdown(notification, extras)
    }

    fun parse(sbn: StatusBarNotification): ParsedTimer {
        val notification = sbn.notification
        val extras = notification.extras
        val actions = notification.actions.orEmpty().mapNotNull { action ->
            val label = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val intent = action.actionIntent ?: return@mapNotNull null
            CutoutSignal.Notification.Action(label, intent)
        }

        val metric = extras?.let { readMetricCountdown(it) }
        if (metric != null) {
            return ParsedTimer(
                endElapsedRealtimeMs = metric.endElapsedRealtimeMs,
                pausedRemainingMs = metric.pausedRemainingMs,
                label = metric.label ?: extras.notificationTitle(),
                actions = actions,
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
        )
    }

    /** The counting-down critical metric of a `MetricStyle` notification, or null if it isn't one. */
    private fun readMetricCountdown(extras: Bundle): MetricCountdown? {
        @Suppress("DEPRECATION")
        val metrics = extras.get(KEY_METRICS) as? List<*> ?: return null
        if (metrics.isEmpty()) return null
        val criticalIndex = extras.getInt(KEY_CRITICAL_INDEX, 0)
        val metric = (metrics.getOrNull(criticalIndex) ?: metrics.firstOrNull()) as? Bundle ?: return null
        val value = metric.getBundle(KEY_VALUE) ?: return null
        if (!value.getBoolean(KEY_COUNT_DOWN, false)) return null

        val label = metric.getString(KEY_LABEL)?.takeIf { it.isNotBlank() }
        val zeroElapsed = value.getLong(KEY_ZERO_ELAPSED, -1L)
        if (zeroElapsed > 0L) {
            return MetricCountdown(endElapsedRealtimeMs = zeroElapsed, pausedRemainingMs = null, label = label)
        }
        if (value.containsKey(KEY_PAUSED_DURATION)) {
            val paused = value.getLong(KEY_PAUSED_DURATION, 0L).coerceAtLeast(0L)
            return MetricCountdown(endElapsedRealtimeMs = null, pausedRemainingMs = paused, label = label)
        }
        return null
    }

    private fun isClassicCountdown(notification: Notification, extras: Bundle): Boolean {
        val countsDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val showsChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        return countsDown && showsChronometer && notification.`when` > 0L
    }

    private fun Bundle.notificationTitle(): String? =
        getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }

    private data class MetricCountdown(
        val endElapsedRealtimeMs: Long?,
        val pausedRemainingMs: Long?,
        val label: String?,
    )

    // Keys the MetricStyle template stores its data under (mirrored by androidx NotificationCompat's
    // MetricStyle). Read defensively — any missing key just means "not a countdown we can read".
    private const val KEY_METRICS = "android.metrics"
    private const val KEY_CRITICAL_INDEX = "android.metrics.criticalIndex"
    private const val KEY_LABEL = "label"
    private const val KEY_VALUE = "value"
    private const val KEY_COUNT_DOWN = "countDown"
    private const val KEY_ZERO_ELAPSED = "zeroElapsedRealtime"
    private const val KEY_PAUSED_DURATION = "pausedDuration"
}
