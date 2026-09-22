package com.vikram.expressiveisland.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Process-wide hot signal stream shared by framework services and the overlay controller.
 *
 * Signals are intentionally non-blocking for Android framework callbacks. A small replay window
 * also means the overlay can recover the most recent signals when its collector is recreated during
 * an overlay/service lifecycle transition instead of starting with an empty stream.
 *
 * This bus is still an ephemeral event channel, not durable notification storage: notification
 * lifecycle ownership remains with CutoutNotificationListenerService and the Android notification
 * framework. Do not put unbounded payloads on this stream.
 */
object IslandEventBus {

    private const val REPLAY_COUNT = 8
    private const val BUFFER_CAPACITY = 64

    private val mutableSignals = MutableSharedFlow<CutoutSignal>(
        replay = REPLAY_COUNT,
        extraBufferCapacity = BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val signals: SharedFlow<CutoutSignal> = mutableSignals

    /**
     * Emit without blocking the framework callback thread. Returns false only when the stream could
     * not accept the signal; callers must not perform retries from notification/system callbacks.
     */
    fun emit(signal: CutoutSignal): Boolean = mutableSignals.tryEmit(signal)
}
