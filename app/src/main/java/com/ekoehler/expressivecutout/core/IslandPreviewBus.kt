package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Signals that the app is on the settings screen and wants the real overlay pinned open,
 * so size/position changes can be seen live on the actual cutout. The overlay controller
 * (in the accessibility service) observes this and keeps a persistent preview island shown.
 */
object IslandPreviewBus {

    private val mutableActive = MutableStateFlow(false)
    val active: StateFlow<Boolean> = mutableActive

    fun setActive(value: Boolean) {
        mutableActive.value = value
    }
}
