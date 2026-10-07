package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the cutout is currently visible, shared so status-bar behavior follows its appearance
 * without the status-bar controller reaching into the overlay.
 */
object CutoutVisibilityBus {
    /** The cutout's visibility and current normal-versus-expanded state. */
    data class State(val visible: Boolean = false, val expanded: Boolean = false)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Publishes whether the cutout is visible and whether it is expanded. */
    fun update(visible: Boolean, expanded: Boolean = false) {
        _state.value = State(visible, expanded && visible)
    }
}
