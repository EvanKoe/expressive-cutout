package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Process-wide, hot channel that decouples signal producers (which live in their own
 * framework services) from the single consumer that owns the overlay. Buffered and
 * drop-oldest so a burst of events can never block or crash a producer.
 */
object IslandEventBus {

    private val mutableSignals = MutableSharedFlow<CutoutSignal>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val signals: SharedFlow<CutoutSignal> = mutableSignals

    fun emit(signal: CutoutSignal) {
        mutableSignals.tryEmit(signal)
    }
}
