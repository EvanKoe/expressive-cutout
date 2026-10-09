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

    /**
     * Whether the collapsed pill is drawn a little wider on its trailing edge to seat the permission
     * dots beside a tile that already writes there. Separate from [widened] because it is a few dp
     * on the right only: it reaches the system icons, which sit at that end of the status bar, but
     * not the clock or the notification icons at the other end.
     */
    private val _grownForDots = MutableStateFlow(false)
    val grownForDots: StateFlow<Boolean> = _grownForDots.asStateFlow()

    fun update(widened: Boolean, grownForDots: Boolean) {
        _widened.value = widened
        _grownForDots.value = grownForDots
    }
}
