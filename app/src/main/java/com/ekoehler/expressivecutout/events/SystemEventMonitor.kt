package com.ekoehler.expressivecutout.events

import android.app.KeyguardManager
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.SystemEventPayload
import com.ekoehler.expressivecutout.core.SystemEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Listens for device-level events and republishes each as a rich [CutoutSignal.System]
 * on the [IslandEventBus]. All registration is dynamic so it lives and dies with the
 * hosting service; nothing here reads or retains any user content.
 */
class SystemEventMonitor(
    private val context: Context,
    private val keyguardManager: KeyguardManager? = context.getSystemService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()
    private val audioManager = context.getSystemService<AudioManager>()

    @Volatile
    private var isLowBatteryState = false

    @Volatile
    private var isDeviceCurrentlyLocked = false

    @Volatile
    private var isAdbConnected = false

    @Volatile
    private var lastRingerMode = -1

    private var lockPollingJob: Job? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isLowBatteryState = false
                    val level = getBatteryLevel(context)
                    val plug = getBatteryPlugType(context)
                    val subtitle = if (plug != null) "$level% • $plug" else "$level% charged"
                    emit(
                        SystemEventPayload(
                            type = SystemEventType.CHARGING_STARTED,
                            title = context.getString(R.string.event_charging_started),
                            subtitle = subtitle,
                            collapsedBadgeText = "$level%",
                            actionIntentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
                        ),
                    )
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    val level = getBatteryLevel(context)
                    emit(
                        SystemEventPayload(
                            type = SystemEventType.CHARGING_STOPPED,
                            title = context.getString(R.string.event_charging_stopped),
                            subtitle = "$level% remaining",
                            collapsedBadgeText = "$level%",
                            actionIntentAction = Intent.ACTION_POWER_USAGE_SUMMARY,
                        ),
                    )
                }
                Intent.ACTION_BATTERY_LOW -> {
                    if (!isLowBatteryState) {
                        isLowBatteryState = true
                        val level = getBatteryLevel(context)
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.BATTERY_LOW,
                                title = context.getString(R.string.event_battery_low),
                                subtitle = "$level% • Connect charger",
                                collapsedBadgeText = "$level%",
                                actionIntentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
                            ),
                        )
                    }
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    isLowBatteryState = false
                }
                Intent.ACTION_SCREEN_OFF -> {
                    onScreenOff()
                }
                Intent.ACTION_SCREEN_ON -> {
                    onScreenOn()
                }
                Intent.ACTION_USER_PRESENT -> {
                    onUserPresent()
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = getUsbDevice(intent)
                    val name = device?.productName?.takeIf { it.isNotBlank() } ?: "Accessory connected"
                    emit(
                        SystemEventPayload(
                            type = SystemEventType.USB_MOUNTED,
                            title = context.getString(R.string.event_usb_mounted),
                            subtitle = name,
                            actionIntentAction = Settings.ACTION_SETTINGS,
                        ),
                    )
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    emit(
                        SystemEventPayload(
                            type = SystemEventType.USB_UNMOUNTED,
                            title = context.getString(R.string.event_usb_unmounted),
                            subtitle = "Device disconnected",
                            actionIntentAction = Settings.ACTION_SETTINGS,
                        ),
                    )
                }
                ACTION_USB_STATE -> {
                    val connected = intent.getBooleanExtra(EXTRA_CONNECTED, false)
                    val adb = intent.getBooleanExtra(EXTRA_ADB, false)
                    if (connected && adb && !isAdbConnected) {
                        isAdbConnected = true
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.ADB_CONNECTED,
                                title = context.getString(R.string.event_adb_connected),
                                subtitle = "ADB session active",
                                actionIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                            ),
                        )
                    } else if ((!connected || !adb) && isAdbConnected) {
                        isAdbConnected = false
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.ADB_DISCONNECTED,
                                title = context.getString(R.string.event_adb_disconnected),
                                subtitle = "ADB session closed",
                                actionIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                            ),
                        )
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = getBluetoothDevice(intent)
                    // Audio headsets/earbuds are handled separately with rich metadata by AudioDeviceCallback
                    val isAudio = runCatching {
                        device?.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
                    }.getOrDefault(false)
                    if (!isAudio) {
                        val name = runCatching { device?.name }.getOrNull()?.takeIf { it.isNotBlank() }
                            ?: "Bluetooth accessory"
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.BLUETOOTH_CONNECTED,
                                title = context.getString(R.string.event_bluetooth_connected),
                                subtitle = name,
                                actionIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
                            ),
                        )
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = getBluetoothDevice(intent)
                    val isAudio = runCatching {
                        device?.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
                    }.getOrDefault(false)
                    if (!isAudio) {
                        val name = runCatching { device?.name }.getOrNull()?.takeIf { it.isNotBlank() }
                            ?: "Device disconnected"
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.BLUETOOTH_DISCONNECTED,
                                title = context.getString(R.string.event_bluetooth_disconnected),
                                subtitle = name,
                                actionIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
                            ),
                        )
                    }
                }
                ACTION_WIFI_AP_STATE_CHANGED -> {
                    val state = intent.getIntExtra(EXTRA_WIFI_AP_STATE, 0)
                    if (state == WIFI_AP_STATE_ENABLED) {
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.HOTSPOT_ENABLED,
                                title = context.getString(R.string.event_hotspot_enabled),
                                subtitle = "Tethering active",
                                actionIntentAction = Settings.ACTION_WIRELESS_SETTINGS,
                            ),
                        )
                    } else if (state == WIFI_AP_STATE_DISABLED) {
                        emit(
                            SystemEventPayload(
                                type = SystemEventType.HOTSPOT_DISABLED,
                                title = context.getString(R.string.event_hotspot_disabled),
                                subtitle = "Tethering turned off",
                                actionIntentAction = Settings.ACTION_WIRELESS_SETTINGS,
                            ),
                        )
                    }
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val mode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, audioManager?.ringerMode ?: -1)
                    if (mode != lastRingerMode) {
                        lastRingerMode = mode
                        when (mode) {
                            AudioManager.RINGER_MODE_NORMAL -> emit(
                                SystemEventPayload(
                                    type = SystemEventType.RINGER_NORMAL,
                                    title = context.getString(R.string.event_ringer_normal),
                                    subtitle = "Ring & alerts active",
                                    actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
                                ),
                            )
                            AudioManager.RINGER_MODE_VIBRATE -> emit(
                                SystemEventPayload(
                                    type = SystemEventType.RINGER_VIBRATE,
                                    title = context.getString(R.string.event_ringer_vibrate),
                                    subtitle = "Calls and alerts will vibrate",
                                    actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
                                ),
                            )
                            AudioManager.RINGER_MODE_SILENT -> emit(
                                SystemEventPayload(
                                    type = SystemEventType.RINGER_SILENT,
                                    title = context.getString(R.string.event_ringer_silent),
                                    subtitle = "Calls and alerts muted",
                                    actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
                                ),
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val headphone = addedDevices.firstOrNull { it.isHeadphone }
            if (headphone != null) {
                val name = headphone.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Audio device"
                emit(
                    SystemEventPayload(
                        type = SystemEventType.HEADPHONES_CONNECTED,
                        title = context.getString(R.string.event_headphones_connected),
                        subtitle = name,
                        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
                    ),
                )
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any { it.isHeadphone }) {
                emit(
                    SystemEventPayload(
                        type = SystemEventType.HEADPHONES_DISCONNECTED,
                        title = context.getString(R.string.event_headphones_disconnected),
                        subtitle = "Audio routed to speaker",
                        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
                    ),
                )
            }
        }
    }

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val ssid = getWifiSsid(context)
            val subtitle = ssid ?: "Connected"
            emit(
                SystemEventPayload(
                    type = SystemEventType.WIFI_CONNECTED,
                    title = context.getString(R.string.event_wifi_connected),
                    subtitle = subtitle,
                    actionIntentAction = Settings.ACTION_WIFI_SETTINGS,
                ),
            )
        }

        override fun onLost(network: Network) {
            emit(
                SystemEventPayload(
                    type = SystemEventType.WIFI_DISCONNECTED,
                    title = context.getString(R.string.event_wifi_disconnected),
                    subtitle = "Disconnected",
                    actionIntentAction = Settings.ACTION_WIFI_SETTINGS,
                ),
            )
        }
    }

    private val vpnCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            emit(
                SystemEventPayload(
                    type = SystemEventType.VPN_CONNECTED,
                    title = context.getString(R.string.event_vpn_connected),
                    subtitle = "Secure tunnel active",
                    actionIntentAction = Settings.ACTION_VPN_SETTINGS,
                ),
            )
        }

        override fun onLost(network: Network) {
            emit(
                SystemEventPayload(
                    type = SystemEventType.VPN_DISCONNECTED,
                    title = context.getString(R.string.event_vpn_disconnected),
                    subtitle = "Disconnected",
                    actionIntentAction = Settings.ACTION_VPN_SETTINGS,
                ),
            )
        }
    }

    /**
     * Registers every broadcast, audio-device and network callback the system events are built
     * from. Paired with [stop].
     */
    fun start() {
        // These are all protected system broadcasts, so the receiver is exported.
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            buildIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )
        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
        connectivityManager?.registerNetworkCallback(wifiRequest(), wifiCallback)
        connectivityManager?.registerNetworkCallback(vpnRequest(), vpnCallback)

        if (keyguardManager?.isDeviceLocked == true) {
            isDeviceCurrentlyLocked = true
            startLockPolling()
        } else {
            isDeviceCurrentlyLocked = false
        }
    }

    /**
     * Undoes everything [start] registered, each unregister guarded so one already-gone callback
     * can't strand the rest.
     */
    fun stop() {
        stopLockPolling()
        scope.cancel()
        runCatching { context.unregisterReceiver(broadcastReceiver) }
        audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        connectivityManager?.unregisterNetworkCallback(wifiCallback)
        connectivityManager?.unregisterNetworkCallback(vpnCallback)
    }

    private fun onScreenOff() {
        stopLockPolling()
        isDeviceCurrentlyLocked = true
        emit(
            SystemEventPayload(
                type = SystemEventType.DEVICE_LOCKED,
                title = context.getString(R.string.event_device_locked),
                subtitle = "Device secured",
                actionIntentAction = Settings.ACTION_SECURITY_SETTINGS,
            ),
        )
    }

    private fun onScreenOn() {
        val locked = keyguardManager?.isDeviceLocked == true
        if (locked) {
            isDeviceCurrentlyLocked = true
            startLockPolling()
        } else if (isDeviceCurrentlyLocked) {
            // Screen turned on and the device is already unlocked (e.g. fingerprint on power button or no lock).
            isDeviceCurrentlyLocked = false
            stopLockPolling()
            emitUnlocked()
        }
    }

    private fun onUserPresent() {
        stopLockPolling()
        if (isDeviceCurrentlyLocked) {
            isDeviceCurrentlyLocked = false
            emitUnlocked()
        }
    }

    private fun startLockPolling() {
        lockPollingJob?.cancel()
        lockPollingJob = scope.launch {
            while (isActive) {
                delay(LOCK_POLL_INTERVAL_MS)
                val locked = keyguardManager?.isDeviceLocked == true
                if (!locked) {
                    if (isDeviceCurrentlyLocked) {
                        isDeviceCurrentlyLocked = false
                        emitUnlocked()
                    }
                    break
                }
            }
        }
    }

    private fun stopLockPolling() {
        lockPollingJob?.cancel()
        lockPollingJob = null
    }

    private fun emitUnlocked() {
        emit(
            SystemEventPayload(
                type = SystemEventType.DEVICE_UNLOCKED,
                title = context.getString(R.string.event_device_unlocked),
                subtitle = "Device unlocked",
                actionIntentAction = Settings.ACTION_SECURITY_SETTINGS,
            ),
        )
    }

    private fun emit(payload: SystemEventPayload) =
        IslandEventBus.emit(CutoutSignal.System(payload))

    /** Reads the current battery capacity (0..100) using [BatteryManager]. */
    private fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService<BatteryManager>()
        val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (capacity != null && capacity in 0..100) {
            return capacity
        }
        val batteryStatus = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) {
            return (level * 100 / scale)
        }
        return 100
    }

    /** Resolves the charging source label (Fast charging / Wireless / USB) from the sticky battery broadcast. */
    private fun getBatteryPlugType(context: Context): String? = runCatching {
        val status = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        when (status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast charging"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless charging"
            else -> null
        }
    }.getOrNull()

    /** Safely extracts the active Wi-Fi network's SSID when available and permitted. */
    private fun getWifiSsid(context: Context): String? = runCatching {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = wifiManager?.connectionInfo
        val ssid = info?.ssid?.removeSurrounding("\"")
        if (ssid == null || ssid == "<unknown ssid>" || ssid.isBlank()) null else ssid
    }.getOrNull()

    /** Safely extracts the attached [UsbDevice] from [intent] across Android SDK versions. */
    @Suppress("DEPRECATION")
    private fun getUsbDevice(intent: Intent): UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else {
        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }

    /** Safely extracts the connected [BluetoothDevice] from [intent] across Android SDK versions. */
    @Suppress("DEPRECATION")
    private fun getBluetoothDevice(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

    /** Builds the [NetworkRequest] matching active VPN connections. */
    private fun vpnRequest() = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        .build()

    /**
     * The set of system broadcasts the pill reacts to, kept in one place so [start] and the
     * manifest can't drift apart.
     */
    private fun buildIntentFilter() = IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        addAction(ACTION_USB_STATE)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        addAction(ACTION_WIFI_AP_STATE_CHANGED)
        addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
    }

    /** Builds the [NetworkRequest] matching active Wi-Fi connections. */
    private fun wifiRequest() = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private val AudioDeviceInfo.isHeadphone: Boolean
        get() = type in HEADPHONE_TYPES

    private companion object {
        const val LOCK_POLL_INTERVAL_MS = 150L
        const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
        const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_ADB = "adb"
        const val EXTRA_WIFI_AP_STATE = "wifi_state"
        const val WIFI_AP_STATE_DISABLED = 11
        const val WIFI_AP_STATE_ENABLED = 13

        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}

