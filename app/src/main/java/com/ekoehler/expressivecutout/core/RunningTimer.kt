package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live timer state, kept up to date by the notification listener (which reads the clock app's
 * ongoing timer or stopwatch notification) and read by the overlay so the timer tile can show a time
 * that keeps ticking, down for a countdown or up for a count-up. Deliberately separate from the
 * transient [CutoutSignal] flow — mirroring [NowPlayingBus] and [OnCallBus] — because a signal makes
 * the island *appear* while this holds the *current* timer that keeps changing while it is shown.
 */
object RunningTimerBus {

    private val _state = MutableStateFlow<RunningTimer?>(null)
    val state: StateFlow<RunningTimer?> = _state.asStateFlow()

    fun update(state: RunningTimer?) {
        _state.value = state
    }
}

/**
 * A snapshot of the timer (countdown or count-up) currently surfaced on the cutout.
 *
 * A running timer carries [endElapsedRealtimeMs] — the zero point on the [android.os.SystemClock.elapsedRealtime]
 * clock. A countdown reaches zero there, so the tile renders the live remainder as
 * `endElapsedRealtimeMs - elapsedRealtime()`; a count-up ([countsUp]) started there, so it renders
 * the elapsed time as `elapsedRealtime() - endElapsedRealtimeMs`. The elapsed-realtime base is used,
 * not wall time, so the shown time stays correct across clock changes and device sleep. A paused
 * timer carries a frozen [pausedRemainingMs] instead — the remaining time for a countdown, the
 * elapsed time for a count-up — and [endElapsedRealtimeMs] is null. [label] is the clock app's label
 * for the timer (e.g. "Timer" or "Timer paused"), or null.
 */
data class RunningTimer(
    val endElapsedRealtimeMs: Long?,
    val pausedRemainingMs: Long?,
    val label: String?,
    /**
     * The clock app's current action buttons. These change with the timer's state (running exposes
     * Pause / Add 1 min, paused exposes Resume / Reset), and the clock re-posts the notification —
     * updating this bus — on every such change, so the overlay can keep the shown chips in step.
     */
    val actions: List<CutoutSignal.Notification.Action> = emptyList(),
    /** True for a count-up timer (stopwatch) that shows elapsed time, false for a countdown. */
    val countsUp: Boolean = false,
)
