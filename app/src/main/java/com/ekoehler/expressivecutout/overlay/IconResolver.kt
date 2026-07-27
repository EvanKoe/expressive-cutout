package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import java.util.concurrent.atomic.AtomicLong

/**
 * Turns a source-agnostic [CutoutSignal] into a renderable [IslandEvent], applying the
 * user's icon overrides and looking up app metadata. This is the one place that touches
 * the [PackageManager] and content resolver, so all the "impure" resolution lives here.
 */
class IconResolver(private val context: Context) {

    private val idGenerator = AtomicLong(0L)

    fun resolve(
        signal: CutoutSignal,
        customIcons: Map<SystemEventType, IconSource>,
        musicSettings: MusicTileSettings,
        phoneSettings: PhoneTileSettings,
    ): IslandEvent = when (signal) {
        is CutoutSignal.Notification -> resolveNotification(signal)
        is CutoutSignal.System -> resolveSystem(signal.type, customIcons)
        is CutoutSignal.Music -> resolveMusic(signal, musicSettings)
        is CutoutSignal.Call -> resolveCall(signal, phoneSettings)
    }

    private fun resolveNotification(signal: CutoutSignal.Notification): IslandEvent {
        val packageManager = context.packageManager
        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(signal.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(signal.packageName)

        val icon = runCatching {
            packageManager.getApplicationIcon(signal.packageName).toImageBitmap()
        }.onFailure { Log.w(TAG, "No icon for ${signal.packageName}", it) }
            .map { IslandIcon.Raster(it) as IslandIcon }
            .getOrDefault(IslandIcon.Vector(SystemEventType.DEVICE_UNLOCKED.defaultIcon))

        val title = signal.title?.takeIf { it.isNotBlank() }
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            // Expanded shows the notification's title and text; the icon conveys the app.
            label = title ?: appLabel,
            detail = signal.text?.takeIf { it.isNotBlank() },
            accent = NOTIFICATION_ACCENT,
            contentIntent = signal.contentIntent,
            notificationKey = signal.key,
            actions = signal.actions.map { action ->
                IslandAction(
                    label = action.title,
                    intent = action.intent,
                    reply = action.reply?.let {
                        IslandReply(it.resultKey, it.remoteInputs, it.hint)
                    },
                )
            },
        )
    }

    private fun resolveMusic(signal: CutoutSignal.Music, settings: MusicTileSettings): IslandEvent {
        val packageManager = context.packageManager
        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(signal.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(signal.packageName)

        // Fallback icon for the collapsed pill when there is no album art (or it's turned off):
        // the player's own launcher icon reads better than a generic note.
        val icon = runCatching {
            packageManager.getApplicationIcon(signal.packageName).toImageBitmap()
        }.onFailure { Log.w(TAG, "No icon for ${signal.packageName}", it) }
            .map { IslandIcon.Raster(it) as IslandIcon }
            .getOrDefault(IslandIcon.Vector(DynamicTile.MUSIC.defaultIcon))

        val title = signal.title?.takeIf { it.isNotBlank() }
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            // Track title on the primary line; artist (or the app) on the secondary line.
            label = title ?: context.getString(DynamicTile.MUSIC.labelRes),
            detail = signal.artist?.takeIf { it.isNotBlank() } ?: appLabel,
            accent = Color(DynamicTile.MUSIC.accent),
            contentIntent = signal.contentIntent,
            media = MediaTileOptions(
                showAlbumArt = settings.showAlbumArt,
                rotateAlbumArt = settings.rotateAlbumArt,
                showControls = settings.showControls,
                skipStyle = settings.skipButton,
                playPauseStyle = settings.playPauseButton,
            ),
        )
    }

    private fun resolveCall(signal: CutoutSignal.Call, settings: PhoneTileSettings): IslandEvent {
        // The live contact photo comes from OnCallBus; this icon is only the no-photo fallback, so a
        // person avatar reads as "a contact" (the Google-dialer default look) better than a handset.
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = IslandIcon.Vector(Icons.Rounded.Person),
            label = signal.callerLabel,
            accent = Color(DynamicTile.PHONE.accent),
            contentIntent = signal.contentIntent,
            // Deliberately no notificationKey: a swipe should hide the pill, never cancel the
            // dialer's own call notification (which wouldn't end the call and would just re-post).
            actions = signal.actions.map { action ->
                IslandAction(
                    label = action.title,
                    intent = action.intent,
                    destructive = isHangUpLabel(action.title),
                )
            },
            call = CallTileOptions(
                showPhoto = settings.showPhoto,
                showDuration = settings.showDuration,
                showActions = settings.showActions,
                hangUpColor = settings.hangUpColor,
                otherButtonColor = settings.otherButtonColor,
            ),
        )
    }

    /**
     * Best-effort match for a call's hang-up / end-call / decline button by its label. A call
     * notification carries no machine-readable flag marking which action ends the call, so we key
     * off the label; failing to match simply leaves that button on the shared "other" colour.
     */
    private fun isHangUpLabel(label: String): Boolean {
        val normalised = label.lowercase()
        return HANG_UP_KEYWORDS.any { normalised.contains(it) }
    }

    private fun resolveSystem(
        type: SystemEventType,
        customIcons: Map<SystemEventType, IconSource>,
    ): IslandEvent {
        val icon = customIcons[type]?.toRasterOrNull() ?: IslandIcon.Vector(type.defaultIcon)
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            label = context.getString(type.labelRes),
            accent = Color(type.accent),
        )
    }

    /** Loads the chosen source into a raster icon, or null to fall back to the default. */
    private fun IconSource.toRasterOrNull(): IslandIcon.Raster? = when (this) {
        is IconSource.Image ->
            Uri.parse(uri).loadImageBitmapOrNull(context)?.let(IslandIcon::Raster)

        is IconSource.App -> runCatching {
            context.packageManager.getApplicationIcon(packageName).toImageBitmap()
        }.onFailure { Log.w(TAG, "No icon for app $packageName", it) }
            .getOrNull()
            ?.let(IslandIcon::Raster)
    }

    private companion object {
        const val TAG = "IconResolver"
        val NOTIFICATION_ACCENT = Color(0xFF38BDF8)

        // Lower-cased substrings that mark a call's end/decline action. English-led (most dialers'
        // notifications localise to the device language, but English covers the common case); the
        // phrases avoid false hits like "send" that a bare "end" would catch.
        val HANG_UP_KEYWORDS = listOf("hang up", "hangup", "hang-up", "end call", "decline", "reject")
    }
}
