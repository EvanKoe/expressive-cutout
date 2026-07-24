package com.ekoehler.expressivecutout.events

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.service.CutoutNotificationListenerService

/**
 * Watches the device's active media sessions and republishes "now playing" as a
 * [CutoutSignal.Music] on the [IslandEventBus] whenever a session starts (or changes track
 * while) playing. Access to media sessions is granted by the app's already-required
 * notification-listener binding — no extra permission is needed — so we pass that component to
 * [MediaSessionManager]. Like [SystemEventMonitor], all registration is dynamic and lives and
 * dies with the hosting service.
 */
class MediaPlaybackMonitor(private val context: Context) {

    private val sessionManager = context.getSystemService<MediaSessionManager>()
    private val listenerComponent = ComponentName(context, CutoutNotificationListenerService::class.java)

    // Controllers we're currently watching, paired with the callback registered on each.
    private val watched = mutableMapOf<MediaController, MediaController.Callback>()

    // The controller last announced as playing, so we don't re-emit on every state tick.
    private var announced: MediaController? = null

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
        announced = null
    }

    /** Attach callbacks to newly active sessions and detach ones that have gone away. */
    private fun rebind(controllers: List<MediaController>) {
        val current = controllers.toSet()
        watched.keys.filter { it !in current }.toList().forEach(::detach)

        controllers.filter { it !in watched }.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) =
                    handleState(controller, state)

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    // A new track on the already-playing session: refresh the tile.
                    if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        announced = controller
                        emit(controller)
                    }
                }

                override fun onSessionDestroyed() = detach(controller)
            }
            watched[controller] = callback
            controller.registerCallback(callback)
            // Surface anything already playing the moment we start watching it.
            handleState(controller, controller.playbackState)
        }
    }

    private fun detach(controller: MediaController) {
        watched.remove(controller)?.let { controller.unregisterCallback(it) }
        if (announced == controller) announced = null
    }

    private fun handleState(controller: MediaController, state: PlaybackState?) {
        if (state?.state == PlaybackState.STATE_PLAYING) {
            if (announced != controller) {
                announced = controller
                emit(controller)
            }
        } else if (announced == controller) {
            announced = null
        }
    }

    private fun emit(controller: MediaController) {
        val metadata = controller.metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
        val artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.toString()
        IslandEventBus.emit(
            CutoutSignal.Music(
                packageName = controller.packageName,
                title = title,
                artist = artist,
                contentIntent = controller.sessionActivity,
            ),
        )
    }

    private companion object {
        const val TAG = "MediaPlaybackMonitor"
    }
}
