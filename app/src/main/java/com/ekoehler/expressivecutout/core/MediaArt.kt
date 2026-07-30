package com.ekoehler.expressivecutout.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The album cover lifted off a player's own media notification, used when the media session itself
 * carries no usable art. Players are free to publish their cover as a URI rather than a bitmap, and
 * Spotify points at its image CDN over https — nothing the session API or the content resolver can
 * rasterise. The same cover rides on the player's MediaStyle notification as its large icon, which
 * the notification listener already receives, so it needs no package-visibility declaration.
 *
 * Kept separate from [NowPlayingBus] because it is fed by a different source (the notification
 * listener, not the media monitor) and arrives on its own schedule — the notification may be posted
 * before or after the session publishes its metadata, so the two are combined where they are drawn.
 */
object MediaArtBus {

    private val _state = MutableStateFlow<MediaArt?>(null)
    val state: StateFlow<MediaArt?> = _state.asStateFlow()

    fun update(state: MediaArt?) {
        _state.value = state
    }
}

/** A cover and the package that posted it, so it is only ever drawn for that player's tile. */
data class MediaArt(
    val packageName: String,
    val art: ImageBitmap,
)
