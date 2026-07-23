package com.ekoehler.expressivecutout.core

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekoehler.expressivecutout.R

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
    ) : CutoutSignal

    /** A device-level event occurred. */
    data class System(val type: SystemEventType) : CutoutSignal
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
    CHARGING_STOPPED(Icons.Rounded.PowerOff, R.string.event_charging_stopped, 0xFF94A3B8),
    BATTERY_LOW(Icons.Rounded.BatteryAlert, R.string.event_battery_low, 0xFFF87171),
    WIFI_CONNECTED(Icons.Rounded.Wifi, R.string.event_wifi_connected, 0xFF60A5FA),
    WIFI_DISCONNECTED(Icons.Rounded.WifiOff, R.string.event_wifi_disconnected, 0xFF94A3B8),
    HEADPHONES_CONNECTED(Icons.Rounded.Headphones, R.string.event_headphones_connected, 0xFFA78BFA),
    HEADPHONES_DISCONNECTED(Icons.Rounded.HeadsetOff, R.string.event_headphones_disconnected, 0xFF94A3B8),
    USB_MOUNTED(Icons.Rounded.Usb, R.string.event_usb_mounted, 0xFF38BDF8),
    USB_UNMOUNTED(Icons.Rounded.Usb, R.string.event_usb_unmounted, 0xFF94A3B8),
    DEVICE_UNLOCKED(Icons.Rounded.LockOpen, R.string.event_device_unlocked, 0xFFFACC15),
}
