package com.ekoehler.expressivecutout.events

import android.app.Notification
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.ekoehler.expressivecutout.core.CutoutSignal
import java.util.Locale

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
 *  - **Samsung Clock:** its ongoing timer shows a countdown string with Pause/Cancel actions instead
 *    of the standard chronometer metadata.
 *
 * The standard formats key off a counting-down time rather than a package allow-list, so calls whose
 * chronometers count up are never mistaken for timers.
 */
object TimerNotificationParser {

    /**
     * Whether this notification is a countdown timer in one of the supported clock formats.
     */
    fun isTimer(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val extras = notification.extras ?: return false
        return readMetricCountdown(extras) != null ||
            isClassicCountdown(notification, extras) ||
            readSamsungClockCountdown(sbn) != null
    }

    /**
     * Reads a timer notification into the shape the timer tile needs: remaining time, label, and
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

        val metric = extras?.let { readMetricCountdown(it) }
        if (metric != null) {
            return ParsedTimer(
                endElapsedRealtimeMs = metric.endElapsedRealtimeMs,
                pausedRemainingMs = metric.pausedRemainingMs,
                label = metric.label ?: extras.notificationTitle(),
                actions = actions,
            )
        }

        val samsungCountdown = readSamsungClockCountdown(sbn)
        if (samsungCountdown != null) {
            return ParsedTimer(
                endElapsedRealtimeMs = samsungCountdown.endElapsedRealtimeMs,
                pausedRemainingMs = samsungCountdown.pausedRemainingMs,
                label = samsungCountdown.label ?: extras?.notificationTitle()
                    ?.takeUnless { parseCountdownDuration(it) != null },
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
     * Reads Samsung Clock's ongoing timer, which publishes a visible countdown and Pause/Cancel
     * actions instead of the standard chronometer metadata.
     */
    private fun readSamsungClockCountdown(sbn: StatusBarNotification): MetricCountdown? {
        if (sbn.packageName != SAMSUNG_CLOCK_PACKAGE) return null

        val notification = sbn.notification ?: return null

        val extras = notification.extras ?: return null
        val actionLabels = notification.actions.orEmpty()
            .mapNotNull { it.title?.toString()?.lowercase(Locale.ROOT) }
        val hasPauseOrResume = actionLabels.any { "pause" in it || "resume" in it }
        val hasCancel = actionLabels.any { "cancel" in it }
        if (!hasPauseOrResume || !hasCancel) return null

        val remainingMs = extras.timerTextValues()
            .firstNotNullOfOrNull(::parseCountdownDuration) ?: return null
        val paused = actionLabels.any { "resume" in it }
        return MetricCountdown(
            endElapsedRealtimeMs = if (paused) null else SystemClock.elapsedRealtime() + remainingMs,
            pausedRemainingMs = remainingMs.takeIf { paused },
            label = extras.notificationTitle()?.takeUnless { parseCountdownDuration(it) != null },
        )
    }

    /**
     * Parses the visible minute/second or hour/minute/second value without interpreting any other
     * notification text as a countdown.
     */
    private fun parseCountdownDuration(text: CharSequence): Long? {
        val parts = text.toString().trim().split(':')
        if (parts.size !in 2..3 || parts.any { part ->
                part.isEmpty() || part.any { !it.isDigit() }
            }) {
            return null
        }

        val values = parts.map { it.toLongOrNull() ?: return null }
        val seconds = values.last()
        val minutes = values[values.lastIndex - 1]
        if (seconds >= SECONDS_PER_MINUTE || (parts.size == 3 && minutes >= MINUTES_PER_HOUR)) {
            return null
        }

        val hours = if (parts.size == 3) values.first() else 0L
        val totalSeconds = hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds
        return totalSeconds * MILLIS_PER_SECOND
    }

    /**
     * Returns the text-bearing fields Samsung Clock uses for its timer countdown.
     */
    private fun Bundle.timerTextValues(): List<String> {
        val values = mutableListOf<String>()
        listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_INFO_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
        ).forEach { key ->
            getCharSequence(key)?.toString()?.let(values::add)
        }
        getCharSequenceArrayList(Notification.EXTRA_TEXT_LINES)
            .orEmpty()
            .mapTo(values) { it.toString() }
        return values
    }

    private fun Bundle.notificationTitle(): String? =
        getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }

    /**
     * The timer state a modern clock app publishes directly, before it is turned into a tile.
     * Either running (with an end time) or paused (with the remaining time frozen).
     */
    private data class MetricCountdown(
        val endElapsedRealtimeMs: Long?,
        val pausedRemainingMs: Long?,
        val label: String?,
    )

    /**
     * Keys the MetricStyle template stores its data under (mirrored by androidx
     * NotificationCompat's MetricStyle). Read defensively — any missing key just means "not a
     * countdown we can read".
     */
    private const val KEY_METRICS = "android.metrics"
    private const val KEY_CRITICAL_INDEX = "android.metrics.criticalIndex"
    private const val KEY_LABEL = "label"
    private const val KEY_VALUE = "value"
    private const val KEY_COUNT_DOWN = "countDown"
    private const val KEY_ZERO_ELAPSED = "zeroElapsedRealtime"
    private const val KEY_PAUSED_DURATION = "pausedDuration"
    private const val SAMSUNG_CLOCK_PACKAGE = "com.sec.android.app.clockpackage"
    private const val MILLIS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
}
