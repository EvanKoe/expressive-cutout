package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The device flashlight's on/off state, as last observed by [CenterShortcutExecutor]. It is synced
 * when the center opens and updated on every torch toggle, so the center can render the torch as a
 * live toggle (lit when on) without any resident camera listener.
 */
object TorchStateBus {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    fun update(on: Boolean) {
        _on.value = on
    }
}
