package com.ekoehler.expressivecutout.events

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live Wi-Fi state, kept up to date by [SystemEventMonitor] and read by the overlay so a
 * Wi-Fi tile can show the current signal. Like [BatteryBus] it holds the *current* value, not a
 * one-off signal.
 */
object WifiBus {

    private val _status = MutableStateFlow<WifiStatus?>(null)
    val status: StateFlow<WifiStatus?> = _status.asStateFlow()

    /** Publishes the latest Wi-Fi state, or null when Wi-Fi is lost or the monitor has stopped. */
    fun publish(status: WifiStatus?) {
        _status.value = status
    }
}

/** A Wi-Fi snapshot: [level] is the signal strength from 0 (weakest) to 4 (strongest). */
data class WifiStatus(val level: Int)
