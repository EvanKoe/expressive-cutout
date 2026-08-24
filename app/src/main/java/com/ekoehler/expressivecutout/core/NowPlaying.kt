package com.ekoehler.expressivecutout.core

import androidx.compose.runtime.Stable
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
    /** Where playback has got to, or null for a session that publishes no position. */
    val progress: MediaProgress? = null,
)

/**
 * Playback position as a self-advancing anchor rather than a live value. A media session only
 * republishes its state when something *changes* — it does not tick — so [positionMs] is only true
 * as of [anchorUptimeMs], and a reader extrapolates from there at [speed]. This is what lets the
 * tile animate a moving bar without the monitor pushing an update several times a second.
 */
data class MediaProgress(
    /** Position within the track as of [anchorUptimeMs]. */
    val positionMs: Long,
    /** Track length, or null when the session publishes none — a live stream, or a player that
     *  simply omits it. The tile falls back to an indeterminate bar. */
    val durationMs: Long?,
    /** Rate the position advances at, 0 while paused. */
    val speed: Float,
    /** [android.os.SystemClock.elapsedRealtime] when [positionMs] was sampled. */
    val anchorUptimeMs: Long,
) {
    /** The position right now, extrapolated from the anchor and clamped to the track length. */
    fun positionAt(nowUptimeMs: Long): Long {
        val elapsed = ((nowUptimeMs - anchorUptimeMs).coerceAtLeast(0L) * speed).toLong()
        val position = positionMs + elapsed
        return durationMs?.let { position.coerceIn(0L, it) } ?: position.coerceAtLeast(0L)
    }

    /** How far through the track, 0f..1f, or null while the length is unknown. */
    fun fractionAt(nowUptimeMs: Long): Float? {
        val total = durationMs?.takeIf { it > 0L } ?: return null
        return (positionAt(nowUptimeMs).toFloat() / total).coerceIn(0f, 1f)
    }
}

/** The transport actions the music tile exposes. Backed by the active media session's controls. */
@Stable
interface MediaTransport {
    fun previous()
    fun playPause()
    fun next()
}
