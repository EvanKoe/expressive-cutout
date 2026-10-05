package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the island is currently drawn wider than its normal collapsed cutout — expanded, or
 * showing one of the fuller call layouts.
 *
 * It exists so the status-bar flags can follow the pill without `StatusBarIconController` reaching
 * into the overlay: the overlay publishes the fact, the controller decides what to do with it.
 * Nothing is published while the island is down, so the flags fall back to the user's own wishes.
 */
object CutoutWidthBus {
    private val _widened = MutableStateFlow(false)
    val widened: StateFlow<Boolean> = _widened.asStateFlow()

    fun update(widened: Boolean) {
        _widened.value = widened
    }
}
