package com.ekoehler.expressivecutout.events

import android.app.KeyguardManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
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
 * Listens for the device-level events the island reacts to and republishes each as a
 * [CutoutSignal] on the [IslandEventBus]. All registration is dynamic so it lives and
 * dies with the hosting service; nothing here reads or retains any user content.
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

    private var lockPollingJob: Job? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isLowBatteryState = false
                    emit(SystemEventType.CHARGING_STARTED)
                }
                Intent.ACTION_POWER_DISCONNECTED -> emit(SystemEventType.CHARGING_STOPPED)
                Intent.ACTION_BATTERY_LOW -> {
                    if (!isLowBatteryState) {
                        isLowBatteryState = true
                        emit(SystemEventType.BATTERY_LOW)
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
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> emit(SystemEventType.USB_MOUNTED)
                UsbManager.ACTION_USB_DEVICE_DETACHED -> emit(SystemEventType.USB_UNMOUNTED)
                else -> {}
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.any { it.isHeadphone }) emit(SystemEventType.HEADPHONES_CONNECTED)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any { it.isHeadphone }) emit(SystemEventType.HEADPHONES_DISCONNECTED)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = emit(SystemEventType.WIFI_CONNECTED)
        override fun onLost(network: Network) = emit(SystemEventType.WIFI_DISCONNECTED)
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
        connectivityManager?.registerNetworkCallback(wifiRequest(), networkCallback)

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
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    private fun onScreenOff() {
        stopLockPolling()
        isDeviceCurrentlyLocked = true
        emit(SystemEventType.DEVICE_LOCKED)
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
            emit(SystemEventType.DEVICE_UNLOCKED)
        }
    }

    private fun onUserPresent() {
        stopLockPolling()
        if (isDeviceCurrentlyLocked) {
            isDeviceCurrentlyLocked = false
            emit(SystemEventType.DEVICE_UNLOCKED)
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
                        emit(SystemEventType.DEVICE_UNLOCKED)
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

    private fun emit(type: SystemEventType) = IslandEventBus.emit(CutoutSignal.System(type))

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
    }

    private fun wifiRequest() = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private val AudioDeviceInfo.isHeadphone: Boolean
        get() = type in HEADPHONE_TYPES

    private companion object {
        const val LOCK_POLL_INTERVAL_MS = 150L

        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}
