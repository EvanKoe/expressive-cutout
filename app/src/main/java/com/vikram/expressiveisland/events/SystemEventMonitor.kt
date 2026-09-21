package com.vikram.expressiveisland.events

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.core.ChargingBus
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.core.CutoutSignal
import com.vikram.expressiveisland.core.IslandEventBus
import com.vikram.expressiveisland.core.SystemEventType
import com.vikram.expressiveisland.core.WifiBus
import com.vikram.expressiveisland.core.WifiState
import kotlin.math.abs
import android.net.LinkProperties
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.wifi.WifiInfo
import android.provider.Settings
import android.util.Log
import com.vikram.expressiveisland.core.BatteryBus
import com.vikram.expressiveisland.core.BatteryState
import com.vikram.expressiveisland.core.HeadphonesBus
import com.vikram.expressiveisland.core.HeadphonesState
import kotlin.math.roundToInt

/**
 * Listens for device-level events and republishes them on [IslandEventBus].
 *
 * Charging has a live state of its own through [ChargingBus], just like music uses [NowPlayingBus].
 * The system event only tells the island that charging started/stopped; the bus carries the
 * continuously changing battery details used by the expanded charging UI.
 */
class SystemEventMonitor(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService<WifiManager>()

    private val audioManager =
        context.getSystemService<AudioManager>()

    private val batteryManager =
        context.getSystemService<BatteryManager>()

    private val connectivityManager =
        context.getSystemService<ConnectivityManager>()

    private val bluetoothManager =
        context.getSystemService<BluetoothManager>()

    private val vpnNetworks = mutableSetOf<Network>()
    private val wifiNetworks = mutableSetOf<Network>()
    private val bluetoothDevices = mutableSetOf<String>()
    private val headphoneDevices = mutableSetOf<Int>()

    @Volatile
    private var isLowBatteryState = false

    @Volatile
    private var wasBatteryFull = false

    @Volatile
    private var wasHotspotEnabled = false

    @Volatile
    private var lastRingerMode = AudioManager.RINGER_MODE_NORMAL

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                Intent.ACTION_POWER_CONNECTED -> {
                    isLowBatteryState = false
                    updateChargingState(intent)
                    emit(SystemEventType.CHARGING_STARTED)
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    wasBatteryFull = false
                    ChargingBus.update(null)
                    emit(SystemEventType.CHARGING_STOPPED)
                }

                Intent.ACTION_BATTERY_CHANGED -> {
                    updateChargingState(intent)
                }

                Intent.ACTION_BATTERY_LOW -> {
                    if (!isLowBatteryState) {
                        isLowBatteryState = true
                        emit(SystemEventType.BATTERY_LOW)
                    }
                }

                Intent.ACTION_BATTERY_OKAY -> {
                    isLowBatteryState = false
                }

                Intent.ACTION_SCREEN_OFF ->
                    emit(SystemEventType.DEVICE_LOCKED)

                Intent.ACTION_USER_PRESENT ->
                    emit(SystemEventType.DEVICE_UNLOCKED)

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    emit(SystemEventType.USB_MOUNTED)

                    if (isAdbConnection(intent)) {
                        emit(SystemEventType.ADB_CONNECTED)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    emit(SystemEventType.USB_UNMOUNTED)

                    if (isAdbConnection(intent)) {
                        emit(SystemEventType.ADB_DISCONNECTED)
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )

                    device?.address?.let { address ->
                        if (bluetoothDevices.add(address)) {
                            emit(SystemEventType.BLUETOOTH_CONNECTED)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )

                    device?.address?.let { address ->
                        if (bluetoothDevices.remove(address)) {
                            emit(SystemEventType.BLUETOOTH_DISCONNECTED)
                        }
                    }
                }

                "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                    val state = intent.getIntExtra(
                        "wifi_state",
                        -1,
                    )

                    when (state) {
                        13 -> {
                            if (!wasHotspotEnabled) {
                                wasHotspotEnabled = true
                                emit(SystemEventType.HOTSPOT_ENABLED)
                            }
                        }

                        11 -> {
                            if (wasHotspotEnabled) {
                                wasHotspotEnabled = false
                                emit(SystemEventType.HOTSPOT_DISABLED)
                            }
                        }
                    }
                }

                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val mode = intent.getIntExtra(
                        AudioManager.EXTRA_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL,
                    )

                    if (mode == lastRingerMode) {
                        return
                    }

                    lastRingerMode = mode

                    when (mode) {
                        AudioManager.RINGER_MODE_NORMAL -> {
                            emit(SystemEventType.RINGER_NORMAL)
                        }

                        AudioManager.RINGER_MODE_VIBRATE -> {
                            emit(SystemEventType.RINGER_VIBRATE)
                        }

                        AudioManager.RINGER_MODE_SILENT -> {
                            emit(SystemEventType.RINGER_SILENT)
                        }
                    }
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(
            addedDevices: Array<out AudioDeviceInfo>,
        ) {
            addedDevices
                .filter { it.isHeadphone }
                .forEach { device ->
                    if (headphoneDevices.add(device.id)) {
                        HeadphonesBus.update(
                            HeadphonesState(
                                name = device.productName?.toString(),
                                type = device.type,
                            )
                        )
                        emit(SystemEventType.HEADPHONES_CONNECTED)
                    }
                }
        }

        override fun onAudioDevicesRemoved(
            removedDevices: Array<out AudioDeviceInfo>,
        ) {
            removedDevices
                .filter { it.isHeadphone }
                .forEach { device -> headphoneDevices.remove(device.id) }
            if (headphoneDevices.isEmpty() && removedDevices.any { it.isHeadphone }) {
                HeadphonesBus.update(null)
                emit(SystemEventType.HEADPHONES_DISCONNECTED)
            }
        }
    }

    private val wifiNetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (wifiNetworks.add(network)) emit(SystemEventType.WIFI_CONNECTED)
                updateWifiState(network, connectivityManager?.getNetworkCapabilities(network))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateWifiState(network, capabilities)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                updateWifiState(network, connectivityManager?.getNetworkCapabilities(network))
            }

            override fun onLost(network: Network) {
                if (wifiNetworks.remove(network) && wifiNetworks.isEmpty()) {
                    WifiBus.update(null)
                    emit(SystemEventType.WIFI_DISCONNECTED)
                }
            }
        }

    private val vpnNetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager?.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(TRANSPORT_VPN) == true && vpnNetworks.add(network)) {
                    emit(SystemEventType.VPN_CONNECTED)
                }
            }

            override fun onLost(network: Network) {
                if (vpnNetworks.remove(network) && vpnNetworks.isEmpty()) {
                    emit(SystemEventType.VPN_DISCONNECTED)
                }
            }
        }

    private fun updateWifiState(
        network: Network,
        capabilities: NetworkCapabilities?,
    ) {
        Log.d(
            "WifiDebug",
            "========== updateWifiState START =========="
        )

        try {
            val connectivity = connectivityManager

            if (connectivity == null) {
                Log.e("WifiDebug", "FAIL: connectivityManager == null")
                return
            }

            Log.d(
                "WifiDebug",
                "capabilities=$capabilities"
            )

            if (capabilities == null) {
                Log.e(
                    "WifiDebug",
                    "FAIL: capabilities == null"
                )
                return
            }

            val isWifi =
                capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                )

            Log.d(
                "WifiDebug",
                "isWifi=$isWifi"
            )

            if (!isWifi) {
                Log.e(
                    "WifiDebug",
                    "FAIL: network is not WIFI"
                )
                return
            }

            val transportInfo =
                capabilities.transportInfo

            Log.d(
                "WifiDebug",
                "transportInfo=$transportInfo"
            )

            val wifiInfo =
                transportInfo as? WifiInfo

            Log.d(
                "WifiDebug",
                "wifiInfo=$wifiInfo"
            )

            if (wifiInfo == null) {
                Log.e(
                    "WifiDebug",
                    "FAIL: transportInfo is not WifiInfo"
                )
                return
            }

            Log.d(
                "WifiDebug",
                "SSID=${wifiInfo.ssid}"
            )

            Log.d(
                "WifiDebug",
                "RSSI=${wifiInfo.rssi}"
            )

            Log.d(
                "WifiDebug",
                "linkSpeed=${wifiInfo.linkSpeed}"
            )

            Log.d(
                "WifiDebug",
                "frequency=${wifiInfo.frequency}"
            )

            val linkProperties =
                connectivity.getLinkProperties(network)

            Log.d(
                "WifiDebug",
                "linkProperties=$linkProperties"
            )

            val ipAddress = linkProperties
                ?.linkAddresses
                ?.firstOrNull { linkAddress ->
                    linkAddress.address.hostAddress
                        ?.contains(":") == false
                }
                ?.address
                ?.hostAddress

            Log.d(
                "WifiDebug",
                "ipAddress=$ipAddress"
            )

            val signalLevel =
                WifiManager.calculateSignalLevel(
                    wifiInfo.rssi,
                    5,
                )

            val ssid = wifiInfo.ssid
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                ?.takeUnless {
                    it.isBlank() ||
                            it == "<unknown ssid>"
                }

            val state = WifiState(
                ssid = ssid,
                signalLevel = signalLevel,
                rssi = wifiInfo.rssi.takeIf {
                    it != -127
                },
                linkSpeedMbps =
                    wifiInfo.linkSpeed.takeIf {
                        it > 0
                    },
                frequencyMhz =
                    wifiInfo.frequency.takeIf {
                        it > 0
                    },
                ipAddress = ipAddress,
                isMetered =
                    capabilities
                        .hasCapability(
                            NetworkCapabilities
                                .NET_CAPABILITY_NOT_METERED
                        )
                        .not(),
            )

            Log.d(
                "WifiDebug",
                "CREATED STATE=$state"
            )

            WifiBus.update(state)

            Log.d(
                "WifiDebug",
                "WifiBus AFTER UPDATE=${WifiBus.state.value}"
            )

            Log.d(
                "WifiDebug",
                "========== updateWifiState SUCCESS =========="
            )

        } catch (e: SecurityException) {

            Log.e(
                "WifiDebug",
                "SECURITY EXCEPTION",
                e,
            )

            WifiBus.update(null)

        } catch (e: Exception) {

            Log.e(
                "WifiDebug",
                "GENERAL EXCEPTION",
                e,
            )

            WifiBus.update(null)
        }
    }

    private fun isAdbConnection(intent: Intent): Boolean {
        val device =
            intent.getParcelableExtra<android.hardware.usb.UsbDevice>(
                UsbManager.EXTRA_DEVICE
            ) ?: return false

        return device.interfaceCount > 0 &&
                (0 until device.interfaceCount).any { index ->
                    device.getInterface(index).interfaceClass == 255 &&
                            device.getInterface(index).interfaceSubclass == 66 &&
                            device.getInterface(index).interfaceProtocol == 1
                }
    }

    fun start() {

        lastRingerMode = audioManager?.ringerMode
            ?: AudioManager.RINGER_MODE_NORMAL

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            buildIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )

        audioManager?.registerAudioDeviceCallback(
            audioDeviceCallback,
            null,
        )

        connectivityManager?.registerNetworkCallback(
            wifiRequest(),
            wifiNetworkCallback,
        )
        connectivityManager?.registerNetworkCallback(
            vpnRequest(),
            vpnNetworkCallback,
        )

        // Populate charging state immediately if the monitor starts while
        // the device is already plugged in.
        context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )?.let { intent ->
            updateChargingState(intent)
        }
    }

    fun stop() {
        runCatching {
            context.unregisterReceiver(broadcastReceiver)
        }

        audioManager?.unregisterAudioDeviceCallback(
            audioDeviceCallback
        )

        connectivityManager?.unregisterNetworkCallback(wifiNetworkCallback)
        connectivityManager?.unregisterNetworkCallback(vpnNetworkCallback)

        vpnNetworks.clear()
        wifiNetworks.clear()
        bluetoothDevices.clear()
        headphoneDevices.clear()
        wasHotspotEnabled = false

        ChargingBus.update(null)
        WifiBus.update(null)
    }

    private fun updateChargingState(intent: Intent) {

        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )

        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

        if (!isCharging) {
            wasBatteryFull = false
            ChargingBus.update(null)
            return
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL && !wasBatteryFull) {
            wasBatteryFull = true
            emit(SystemEventType.CHARGING_COMPLETE)
        } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            wasBatteryFull = false
        }

        val level = intent.getIntExtra(
            BatteryManager.EXTRA_LEVEL,
            -1,
        )

        val scale = intent.getIntExtra(
            BatteryManager.EXTRA_SCALE,
            100,
        )

        val percentage = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).roundToInt()
        } else {
            -1
        }

        if (percentage >= 0) {
            BatteryBus.update(
                BatteryState(
                    percentage = percentage.coerceIn(0, 100),
                    isCharging = isCharging,
                )
            )
        }

        val batteryPercent =
            if (level >= 0 && scale > 0) {
                ((level * 100f) / scale)
                    .toInt()
                    .coerceIn(0, 100)
            } else {
                -1
            }

        val voltageMv =
            intent.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE,
                -1,
            ).takeIf { it > 0 }

        val temperatureTenthsC =
            intent.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                -1,
            ).takeIf { it > 0 }

        val currentUa =
            batteryManager
                ?.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CURRENT_NOW,
                )
                ?.toLong()
                ?.takeIf { it != 0L }

        val powerWatts =
            if (voltageMv != null && currentUa != null) {
                abs(
                    voltageMv.toFloat() *
                            currentUa.toFloat()
                ) / 1_000_000_000f
            } else {
                null
            }

        val timeToFullMs =
            batteryManager
                ?.computeChargeTimeRemaining()
                ?.takeIf { it > 0L }

        val pluggedType =
            intent.getIntExtra(
                BatteryManager.EXTRA_PLUGGED,
                0,
            )

        ChargingBus.update(
            ChargingState(
                batteryPercent = batteryPercent,
                isCharging = true,
                pluggedType = pluggedType,
                voltageMv = voltageMv,
                currentUa = currentUa,
                powerWatts = powerWatts,
                temperatureTenthsC = temperatureTenthsC,
                timeToFullMs = timeToFullMs,
            )
        )
    }

    private fun emit(type: SystemEventType) {
        IslandEventBus.emit(
            CutoutSignal.System(type)
        )
    }

    private fun buildIntentFilter() =
        IntentFilter().apply {

            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)

            // Live battery updates while charging.
            addAction(Intent.ACTION_BATTERY_CHANGED)

            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)

            addAction(Intent.ACTION_SCREEN_OFF)

            addAction(Intent.ACTION_USER_PRESENT)

            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")

            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }

    private fun wifiRequest() =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

    private fun vpnRequest() =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()

    private val AudioDeviceInfo.isHeadphone: Boolean
        get() = type in HEADPHONE_TYPES

    private companion object {

        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}