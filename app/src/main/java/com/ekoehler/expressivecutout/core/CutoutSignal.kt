package com.ekoehler.expressivecutout.core

import android.app.PendingIntent
import android.app.RemoteInput
import android.graphics.drawable.Icon
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.VpnKeyOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.WifiTetheringOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.service.ProgressData

/**
 * A raw, source-agnostic trigger emitted onto the [IslandEventBus]. Producers (the
 * notification listener, the system-event monitor) stay free of any UI or icon logic;
 * resolving a signal into something displayable is the overlay's responsibility.
 */
sealed interface CutoutSignal {

    /** A notification was posted by another app. */
    data class Notification(
        val packageName: String,
        val title: String?,
        val text: String? = null,
        val appName: String? = null,
        val postTimeMs: Long = java.lang.System.currentTimeMillis(),
        /** The posting notification's stable key, used to dismiss it from the system. */
        val key: String? = null,
        /** The notification's tap action, fired when the user taps the expanded island. */
        val contentIntent: PendingIntent? = null,
        /** The notification's action buttons (e.g. "Archive", "Mark read"), if any. */
        val actions: List<Action> = emptyList(),
        /**
         * The notification's own large icon — full-colour art the posting app chose for this specific
         * notification (a sender's photo, a podcast cover). Null when it set none.
         */
        val largeIcon: Icon? = null,
        /**
         * The notification's own small icon: the monochrome glyph the status bar shows. Always set by
         * a valid notification, and the fallback when [largeIcon] is null.
         */
        val smallIcon: Icon? = null,
        val progressData: ProgressData? = null,
        val isSilent: Boolean = false,
    ) : CutoutSignal {

        /**
         * A single notification action button. When [reply] is non-null the action expects typed
         * text (a messaging "Reply"); the island shows an inline text field and sends the result
         * through [intent]. When null the action fires [intent] directly.
         */
        data class Action(
            val title: String,
            val intent: PendingIntent,
            val reply: ReplyInput? = null,
        )

        /**
         * The pieces needed to fulfil an inline reply: the [resultKey] the receiving app reads the
         * text under, every [remoteInputs] the action declared (all must be filled in), and an
         * optional [hint] to show in the field.
         */
        data class ReplyInput(
            val resultKey: String,
            val remoteInputs: List<RemoteInput>,
            val hint: String?,
        )
    }

    /** A device-level event occurred. */
    data class System(
        val payload: SystemEventPayload,
    ) : CutoutSignal {
        val type: SystemEventType get() = payload.type
        val batteryLevel: Int? get() = payload.collapsedBadgeText?.removeSuffix("%")?.toIntOrNull()

        /** Backward-compatible constructor for callers emitting (type, batteryLevel). */
        constructor(type: SystemEventType, batteryLevel: Int? = null) : this(
            SystemEventPayload(
                type = type,
                collapsedBadgeText = batteryLevel?.let { "$it%" },
            ),
        )
    }

    /** Media started playing on the device (surfaced from any app's media session). */
    data class Music(
        val packageName: String,
        val title: String?,
        val artist: String? = null,
        /** The media session's activity, opened when the user taps the expanded island. */
        val contentIntent: PendingIntent? = null,
    ) : CutoutSignal

    /**
     * A phone call is in progress (surfaced from the dialer's ongoing-call notification). [actions]
     * are the call's own buttons (Hang up, and Answer / Decline / Mute / … when the dialer exposes
     * them), reusing this file's [Notification.Action] so the overlay fires them like any
     * notification action (calls never carry a reply).
     */
    data class Call(
        val packageName: String,
        val callerLabel: String,
        /** The call notification's key, so a tap can open it and removal can end the tile. */
        val key: String? = null,
        /** The dialer's tap action, opened when the user taps the expanded island. */
        val contentIntent: PendingIntent? = null,
        val actions: List<Notification.Action> = emptyList(),
        /** True for a connected call, false while it is still ringing / incoming. */
        val ongoing: Boolean = true,
    ) : CutoutSignal

    /**
     * A countdown timer is running (surfaced from the clock app's ongoing count-down notification).
     * The live remaining time is held on [RunningTimerBus]; [actions] are the notification's own
     * buttons (typically "+ 1:00" and Reset / Stop), reusing [Notification.Action] so the overlay
     * fires them like any notification action (timers never carry a reply).
     */
    data class Timer(
        val packageName: String,
        /** The timer's name when the clock app supplies one, else null. */
        val label: String? = null,
        /** The notification's key, so a tap can open it and removal can clear the tile. */
        val key: String? = null,
        /** The clock app's tap action, opened when the user taps the expanded island. */
        val contentIntent: PendingIntent? = null,
        val actions: List<Notification.Action> = emptyList(),
    ) : CutoutSignal

    /** Voice assistant activity or response (surfaced from voice assistant media session / app). */
    data class Assistant(
        val packageName: String,
        val title: String? = null,
        val text: String? = null,
        val contentIntent: PendingIntent? = null,
        val active: Boolean = true,
    ) : CutoutSignal
}

/**
 * The closed set of system events the island reacts to. Each carries a default icon and
 * a human-readable label; the default icon may be overridden per-type by the user.
 */
enum class SystemEventType(
    val defaultIcon: ImageVector,
    @param:StringRes val labelRes: Int,
    val accent: Long,
) {
    CHARGING_STARTED(Icons.Rounded.BatteryChargingFull, R.string.event_charging_started, 0xFF4ADE80),
    CHARGING_STOPPED(Icons.Rounded.PowerOff, R.string.event_charging_stopped, 0xFFF87171),
    BATTERY_LOW(Icons.Rounded.BatteryAlert, R.string.event_battery_low, 0xFFF87171),
    WIFI_CONNECTED(Icons.Rounded.Wifi, R.string.event_wifi_connected, 0xFF4ADE80),
    WIFI_DISCONNECTED(Icons.Rounded.WifiOff, R.string.event_wifi_disconnected, 0xFFF87171),
    HEADPHONES_CONNECTED(Icons.Rounded.Headphones, R.string.event_headphones_connected, 0xFFA78BFA),
    HEADPHONES_DISCONNECTED(Icons.Rounded.HeadsetOff, R.string.event_headphones_disconnected, 0xFFF87171),
    USB_MOUNTED(Icons.Rounded.Usb, R.string.event_usb_mounted, 0xFF38BDF8),
    USB_UNMOUNTED(Icons.Rounded.Usb, R.string.event_usb_unmounted, 0xFFF87171),
    DEVICE_LOCKED(Icons.Rounded.Lock, R.string.event_device_locked, 0xFFFACC15),
    DEVICE_UNLOCKED(Icons.Rounded.LockOpen, R.string.event_device_unlocked, 0xFFFACC15),
    VPN_CONNECTED(Icons.Rounded.VpnKey, R.string.event_vpn_connected, 0xFF38BDF8),
    VPN_DISCONNECTED(Icons.Rounded.VpnKeyOff, R.string.event_vpn_disconnected, 0xFFF87171),
    ADB_CONNECTED(Icons.Rounded.Adb, R.string.event_adb_connected, 0xFF4ADE80),
    ADB_DISCONNECTED(Icons.Rounded.Adb, R.string.event_adb_disconnected, 0xFFF87171),
    BLUETOOTH_CONNECTED(Icons.Rounded.BluetoothConnected, R.string.event_bluetooth_connected, 0xFF38BDF8),
    BLUETOOTH_DISCONNECTED(Icons.Rounded.BluetoothDisabled, R.string.event_bluetooth_disconnected, 0xFFF87171),
    HOTSPOT_ENABLED(Icons.Rounded.WifiTethering, R.string.event_hotspot_enabled, 0xFF4ADE80),
    HOTSPOT_DISABLED(Icons.Rounded.WifiTetheringOff, R.string.event_hotspot_disabled, 0xFFF87171),
    RINGER_NORMAL(Icons.Rounded.VolumeUp, R.string.event_ringer_normal, 0xFF4ADE80),
    RINGER_VIBRATE(Icons.Rounded.Vibration, R.string.event_ringer_vibrate, 0xFFFACC15),
    RINGER_SILENT(Icons.Rounded.VolumeOff, R.string.event_ringer_silent, 0xFFF87171),
}
