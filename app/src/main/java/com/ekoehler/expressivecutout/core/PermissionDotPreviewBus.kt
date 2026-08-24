package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Signals that the permission-dot settings screen is open and wants every enabled dot shown on the
 * real cutout, so placement and colours can be judged there rather than in a mock-up.
 *
 * While [active], `PermissionUsageMonitor` reports each watched resource as in use instead of
 * polling app ops — which is also what makes the preview work with Shizuku down — and the overlay
 * draws the dots even if the feature's own switch is off.
 */
object PermissionDotPreviewBus {

    private val mutableActive = MutableStateFlow(false)
    val active: StateFlow<Boolean> = mutableActive

    fun setActive(value: Boolean) {
        mutableActive.value = value
    }
}
