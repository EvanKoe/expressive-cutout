package com.ekoehler.expressivecutout.events

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live battery state, kept up to date by [SystemEventMonitor] and read by the overlay so a
 * battery tile can show the current level. Separate from the transient event flow, like
 * [com.ekoehler.expressivecutout.core.OnCallBus]: this holds the *current* value, not a one-off signal.
 */
object BatteryBus {

    private val _status = MutableStateFlow<BatteryStatus?>(null)
    val status: StateFlow<BatteryStatus?> = _status.asStateFlow()

    /** Publishes the latest battery state, or null once the monitor has stopped. */
    fun publish(status: BatteryStatus?) {
        _status.value = status
    }
}

/** A battery snapshot: [level] is 0-100 and [charging] is true while on AC, USB or wireless power. */
data class BatteryStatus(val level: Int, val charging: Boolean)
