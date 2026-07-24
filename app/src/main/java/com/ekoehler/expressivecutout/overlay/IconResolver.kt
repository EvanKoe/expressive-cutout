package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.IconSource
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
    ): IslandEvent = when (signal) {
        is CutoutSignal.Notification -> resolveNotification(signal)
        is CutoutSignal.System -> resolveSystem(signal, customIcons)
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

    private fun resolveSystem(
        signal: CutoutSignal.System,
        customIcons: Map<SystemEventType, IconSource>,
    ): IslandEvent {
        val type = signal.type
        val icon = customIcons[type]?.toRasterOrNull() ?: IslandIcon.Vector(type.defaultIcon)
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            label = context.getString(type.labelRes),
            // Only Wi‑Fi carries a secondary line today, and only when the network name is known.
            // The collapsed pill shows just the icon, so this text appears only when expanded.
            detail = systemDetail(type, signal.detail),
            accent = Color(type.accent),
        )
    }

    /** Builds the expanded island's secondary line for system events that supply extra data. */
    private fun systemDetail(type: SystemEventType, detail: String?): String? {
        val value = detail?.takeIf { it.isNotBlank() } ?: return null
        return when (type) {
            SystemEventType.WIFI_CONNECTED ->
                context.getString(R.string.event_wifi_connected_detail, value)

            else -> null
        }
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
    }
}
