package com.ekoehler.expressivecutout.events

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.MediaTransport
import com.ekoehler.expressivecutout.core.NowPlaying
import com.ekoehler.expressivecutout.core.NowPlayingBus
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.toArtImageBitmap
import com.ekoehler.expressivecutout.service.CutoutNotificationListenerService

/**
 * Watches the device's active media sessions and drives the music tile. It keeps [NowPlayingBus]
 * in sync with the current session (title, artist, album art, play/pause state and a transport
 * handle) and republishes a [CutoutSignal.Music] whenever playback starts or the track changes, so
 * the island pops up. Access to media sessions is granted by the app's already-required
 * notification-listener binding — no extra permission is needed. Like [SystemEventMonitor], all
 * registration is dynamic and lives and dies with the hosting service.
 */
class MediaPlaybackMonitor(private val context: Context) {

    private val sessionManager = context.getSystemService<MediaSessionManager>()
    private val listenerComponent = ComponentName(context, CutoutNotificationListenerService::class.java)

    // Controllers we're currently watching, paired with the callback registered on each.
    private val watched = mutableMapOf<MediaController, MediaController.Callback>()

    // The track last surfaced as a "show" signal, so we don't re-pop on every state tick.
    private var lastShownKey: String? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            rebind(controllers.orEmpty())
        }

    fun start() {
        val manager = sessionManager ?: return
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            rebind(manager.getActiveSessions(listenerComponent))
        }.onFailure { Log.w(TAG, "Media session access unavailable", it) }
    }

    fun stop() {
        sessionManager?.let { runCatching { it.removeOnActiveSessionsChangedListener(sessionsListener) } }
        watched.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        watched.clear()
        lastShownKey = null
        NowPlayingBus.update(null)
    }

    /** Attach callbacks to newly active sessions and detach ones that have gone away. */
    private fun rebind(controllers: List<MediaController>) {
        val current = controllers.toSet()
        watched.keys.filter { it !in current }.toList().forEach(::detach)

        controllers.filter { it !in watched }.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = sync()
                override fun onMetadataChanged(metadata: MediaMetadata?) = sync()
                override fun onSessionDestroyed() = detach(controller)
            }
            watched[controller] = callback
            controller.registerCallback(callback)
        }
        sync()
    }

    private fun detach(controller: MediaController) {
        watched.remove(controller)?.let { controller.unregisterCallback(it) }
        sync()
    }

    /**
     * Recompute the surfaced session: prefer one that's actually playing, else any active one.
     * Publishes its live state to [NowPlayingBus] and pops the island when a new track starts.
     */
    private fun sync() {
        val primary = watched.keys.firstOrNull { it.isPlaying } ?: watched.keys.firstOrNull()
        if (primary == null) {
            NowPlayingBus.update(null)
            lastShownKey = null
            return
        }

        val playing = primary.isPlaying
        val metadata = primary.metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
        val artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.toString()
        val albumArt = metadata?.albumArt()

        NowPlayingBus.update(
            NowPlaying(
                packageName = primary.packageName,
                title = title,
                artist = artist,
                albumArt = albumArt,
                isPlaying = playing,
                transport = ControllerTransport(primary),
            ),
        )

        // Pop the island when a fresh track begins playing; reset when paused so a resume re-pops.
        if (!playing) {
            lastShownKey = null
            return
        }
        val key = "${primary.packageName}|$title|$artist"
        if (key != lastShownKey) {
            lastShownKey = key
            IslandEventBus.emit(
                CutoutSignal.Music(
                    packageName = primary.packageName,
                    title = title,
                    artist = artist,
                    contentIntent = primary.sessionActivity,
                ),
            )
        }
    }

    private val MediaController.isPlaying: Boolean
        get() = playbackState?.state == PlaybackState.STATE_PLAYING

    /**
     * The cover the session itself carries: a bitmap if the player published one, else a URI we can
     * read locally. A player pointing at a remote CDN (Spotify) yields null here and the tile falls
     * back to the cover lifted off its media notification — see
     * [com.ekoehler.expressivecutout.core.MediaArtBus].
     */
    private fun MediaMetadata.albumArt(): ImageBitmap? = (
        getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        )?.toArtImageBitmap()
        ?: artUri()?.loadImageBitmapOrNull(context)

    /** The art URI a player publishes in place of a bitmap, if it gave one at all. */
    private fun MediaMetadata.artUri(): Uri? = listOf(
        MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
        MediaMetadata.METADATA_KEY_ART_URI,
        MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
    ).firstNotNullOfOrNull { key -> getString(key)?.takeIf { it.isNotBlank() } }
        ?.let { runCatching { it.toUri() }.getOrNull() }

    /** Bridges the tile's transport buttons to the active session's controls. */
    private class ControllerTransport(private val controller: MediaController) : MediaTransport {
        override fun previous() {
            runCatching { controller.transportControls.skipToPrevious() }
        }

        override fun playPause() {
            runCatching {
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
        }

        override fun next() {
            runCatching { controller.transportControls.skipToNext() }
        }
    }

    private companion object {
        const val TAG = "MediaPlaybackMonitor"
    }
}
