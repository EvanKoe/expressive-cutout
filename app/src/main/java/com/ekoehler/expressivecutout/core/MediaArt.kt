package com.ekoehler.expressivecutout.core

import android.app.PendingIntent
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

/**
 * A cover and/or like action lifted off the media notification of [packageName], so they are only
 * ever drawn for that player's tile. [art] is null when the notification carries no large icon;
 * [like] is null when no like/save action was recognised.
 */
data class MediaArt(
    val packageName: String,
    val art: ImageBitmap?,
    val like: MediaLike? = null,
)

/**
 * The player's like/save notification action: its [label] (the action title), whether the track is
 * already [liked] (the action then reads "Remove from…", "Liked"…), and the [intent] to fire when
 * the user taps it.
 */
data class MediaLike(
    val label: String,
    val liked: Boolean,
    val intent: PendingIntent,
)
