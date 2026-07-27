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
 * A snapshot of the countdown timer currently surfaced on the cutout. [endTimeMs] is the wall-clock
 * time (epoch millis) the timer reaches zero, so the tile can render a live-ticking remainder simply
 * as `endTimeMs - now`. [label] is the timer's name when the clock app supplies one, else null.
 */
data class RunningTimer(
    val endTimeMs: Long,
    val label: String?,
)
