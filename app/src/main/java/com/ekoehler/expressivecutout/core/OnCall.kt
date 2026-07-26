package com.ekoehler.expressivecutout.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live "on call" state, kept up to date by the notification listener (which reads the dialer's
 * ongoing-call notification) and read by the overlay so the phone tile can show the contact photo
 * and a duration that keeps ticking while the call lasts. Deliberately separate from the transient
 * [CutoutSignal] flow — mirroring [NowPlayingBus] for the music tile — because a signal makes the
 * island *appear* while this holds the *current* call state that keeps changing while it is shown.
 */
object OnCallBus {

    private val _state = MutableStateFlow<OnCall?>(null)
    val state: StateFlow<OnCall?> = _state.asStateFlow()

    fun update(state: OnCall?) {
        _state.value = state
    }
}

/**
 * A snapshot of the phone call currently surfaced on the cutout. [callerLabel] is the contact name
 * (or the number when unknown). [photo] is the contact's picture, or null to fall back to a default
 * avatar. [startTimeMs] is the call's connect time (epoch millis) once it is [ongoing], used to tick
 * the duration; it is null while the call is still ringing or the dialer exposes no timer.
 */
data class OnCall(
    val callerLabel: String,
    val photo: ImageBitmap?,
    val startTimeMs: Long?,
    val ongoing: Boolean,
)
