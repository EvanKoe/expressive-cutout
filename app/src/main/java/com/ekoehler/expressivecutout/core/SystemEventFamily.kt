package com.ekoehler.expressivecutout.core

import androidx.annotation.StringRes
import com.ekoehler.expressivecutout.R

/**
 * Groups system events that describe different states of the same device integration.
 */
enum class SystemEventFamily(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    val members: List<SystemEventType>,
) {
    CHARGING(R.string.event_group_charging, R.string.event_group_charging_desc, listOf(
        SystemEventType.CHARGING_STARTED,
        SystemEventType.CHARGING_STOPPED,
        SystemEventType.CHARGING_COMPLETE,
        SystemEventType.BATTERY_LOW,
    )),
    WIFI(R.string.event_group_wifi, R.string.event_group_wifi_desc, listOf(
        SystemEventType.WIFI_CONNECTED,
        SystemEventType.WIFI_DISCONNECTED,
    )),
    HEADPHONES(R.string.event_group_headphones, R.string.event_group_headphones_desc, listOf(
        SystemEventType.HEADPHONES_CONNECTED,
        SystemEventType.HEADPHONES_DISCONNECTED,
    )),
    USB(R.string.event_group_usb, R.string.event_group_usb_desc, listOf(
        SystemEventType.USB_MOUNTED,
        SystemEventType.USB_UNMOUNTED,
    )),
    LOCK(R.string.event_group_lock, R.string.event_group_lock_desc, listOf(
        SystemEventType.DEVICE_LOCKED,
        SystemEventType.DEVICE_UNLOCKED,
    )),
    VPN(R.string.event_group_vpn, R.string.event_group_vpn_desc, listOf(
        SystemEventType.VPN_CONNECTED,
        SystemEventType.VPN_DISCONNECTED,
    )),
    ADB(R.string.event_group_adb, R.string.event_group_adb_desc, listOf(
        SystemEventType.ADB_CONNECTED,
        SystemEventType.ADB_DISCONNECTED,
    )),
    WIRELESS_DEBUGGING(R.string.event_group_wireless_debugging, R.string.event_group_wireless_debugging_desc, listOf(
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED,
        SystemEventType.WIRELESS_DEBUGGING_DISCONNECTED,
    )),
    BLUETOOTH(R.string.event_group_bluetooth, R.string.event_group_bluetooth_desc, listOf(
        SystemEventType.BLUETOOTH_CONNECTED,
        SystemEventType.BLUETOOTH_DISCONNECTED,
    )),
    HOTSPOT(R.string.event_group_hotspot, R.string.event_group_hotspot_desc, listOf(
        SystemEventType.HOTSPOT_ENABLED,
        SystemEventType.HOTSPOT_DISABLED,
    )),
    RINGER(R.string.event_group_ringer, R.string.event_group_ringer_desc, listOf(
        SystemEventType.RINGER_NORMAL,
        SystemEventType.RINGER_VIBRATE,
        SystemEventType.RINGER_SILENT,
    )),
}

/**
 * Returns the state family represented by this system event.
 */
val SystemEventType.stateFamily: SystemEventFamily
    get() = SystemEventFamily.entries.first { this in it.members }
