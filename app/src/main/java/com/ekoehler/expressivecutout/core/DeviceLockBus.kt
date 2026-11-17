package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the device is currently locked (keyguard showing), as seen by the event monitor.
 *
 * It exists so `StatusBarIconController` can follow the keyguard without reaching into
 * `SystemEventMonitor`: the monitor publishes the fact, the controller picks the matching rule.
 * While the accessibility service is down this is false, so the unlocked rule applies.
 */
object DeviceLockBus {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Publishes the current keyguard state. */
    fun update(locked: Boolean) {
        _locked.value = locked
    }
}
