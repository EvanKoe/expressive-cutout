package com.ekoehler.expressivecutout.events

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.ekoehler.expressivecutout.core.CutoutSignal

/**
 * Everything the timer tile needs, pulled out of a clock app's ongoing count-down notification.
 * Keeps the Android chronometer extras plumbing out of the listener service.
 */
data class ParsedTimer(
    /** Wall-clock time (epoch millis) the timer reaches zero. */
    val endTimeMs: Long,
    /** The timer's name when the notification carries one, else null. */
    val label: String?,
    val actions: List<CutoutSignal.Notification.Action>,
)

/**
 * Recognises and reads count-down timer notifications. A timer is any notification the clock app
 * renders with a *counting-down* chronometer anchored to its `when` — the standard way Android shows
 * a live remaining time. Keying off that (rather than a package allow-list) works across clock apps
 * and never mistakes a call, whose chronometer counts *up*, for a timer.
 */
object TimerNotificationParser {

    fun isTimer(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val extras = notification.extras ?: return false
        val countsDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val showsChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        return countsDown && showsChronometer && notification.`when` > 0L
    }

    fun parse(sbn: StatusBarNotification): ParsedTimer {
        val notification = sbn.notification
        val extras = notification.extras
        // A count-down chronometer ticks toward `when`, so that is the timer's zero point.
        val endTimeMs = notification.`when`
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }

        val actions = notification.actions.orEmpty().mapNotNull { action ->
            val label = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val intent = action.actionIntent ?: return@mapNotNull null
            CutoutSignal.Notification.Action(label, intent)
        }

        return ParsedTimer(endTimeMs = endTimeMs, label = title, actions = actions)
    }
}
