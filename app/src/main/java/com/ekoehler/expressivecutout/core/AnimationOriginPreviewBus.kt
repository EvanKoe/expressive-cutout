package com.ekoehler.expressivecutout.core

import com.ekoehler.expressivecutout.data.AnimationOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Carries a temporary animation-origin marker position from the settings screen to the accessibility
 * overlay, or null when the marker should be hidden.
 */
object AnimationOriginPreviewBus {

    private val mutableOrigin = MutableStateFlow<AnimationOrigin?>(null)
    val origin: StateFlow<AnimationOrigin?> = mutableOrigin

    /** Shows the marker at [origin], or hides it when null. */
    fun setOrigin(origin: AnimationOrigin?) {
        mutableOrigin.value = origin
    }
}
