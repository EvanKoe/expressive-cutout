package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live countdown-timer state, kept up to date by the notification listener (which reads the
 * clock app's ongoing count-down notification) and read by the overlay so the timer tile can show a
 * remaining time that keeps ticking down. Deliberately separate from the transient [CutoutSignal]
 * flow — mirroring [NowPlayingBus] and [OnCallBus] — because a signal makes the island *appear*
 * while this holds the *current* countdown that keeps changing while it is shown.
 */
object RunningTimerBus {

    private val _state = MutableStateFlow<RunningTimer?>(null)
    val state: StateFlow<RunningTimer?> = _state.asStateFlow()

    fun update(state: RunningTimer?) {
        _state.value = state
    }
}

/**
 * A snapshot of the countdown timer currently surfaced on the cutout.
 *
 * A running timer carries [endElapsedRealtimeMs] — the point on the [android.os.SystemClock.elapsedRealtime]
 * clock at which it reaches zero — so the tile renders a live-ticking remainder as
 * `endElapsedRealtimeMs - elapsedRealtime()` (the elapsed-realtime base is used, not wall time, so the
 * countdown stays correct across clock changes and device sleep). A paused timer carries a frozen
 * [pausedRemainingMs] instead, and [endElapsedRealtimeMs] is null. [label] is the clock app's label
 * for the timer (e.g. "Timer" or "Timer paused"), or null.
 */
data class RunningTimer(
    val endElapsedRealtimeMs: Long?,
    val pausedRemainingMs: Long?,
    val label: String?,
)
