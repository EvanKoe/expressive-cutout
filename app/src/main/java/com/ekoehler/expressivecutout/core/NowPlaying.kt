package com.ekoehler.expressivecutout.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live "now playing" state, kept up to date by the media monitor and read by the overlay so
 * the music tile can show album art, reflect play/pause, and drive the transport controls. This is
 * deliberately separate from the transient [CutoutSignal] flow: a signal makes the island *appear*,
 * while this holds the *current* media state that keeps changing while it is shown.
 */
object NowPlayingBus {

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    fun update(state: NowPlaying?) {
        _state.value = state
    }
}

/** A snapshot of the media session currently surfaced on the cutout, plus a handle to control it. */
data class NowPlaying(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val albumArt: ImageBitmap?,
    val isPlaying: Boolean,
    val transport: MediaTransport,
)

/** The transport actions the music tile exposes. Backed by the active media session's controls. */
interface MediaTransport {
    fun previous()
    fun playPause()
    fun next()
}
