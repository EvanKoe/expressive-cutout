package com.ekoehler.expressivecutout.system

import android.app.AppOpsManager
import android.content.Context
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.Immutable
import com.ekoehler.expressivecutout.core.PermissionDotPreviewBus
import com.ekoehler.expressivecutout.data.PermissionDotKinds
import com.ekoehler.expressivecutout.data.PermissionDotPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/** Log tag for the reflection below, which reports failures rather than throwing. */
private const val TAG = "PermissionUsage"

/** Which sensitive resource an app op belongs to, as the island reports it. */
private enum class UsageKind { MICROPHONE, CAMERA, LOCATION }

/**
 * The app ops watched, and what each one means. Location covers both the one-shot fixes and the two
 * "monitor" ops a resident subscriber holds (navigation, fitness tracking) — the same set the
 * system's own location indicator reacts to. The monitor ops are spelled out because their
 * `OPSTR_` constants are hidden; an op name this Android version doesn't know is dropped when the
 * codes are resolved.
 */
private val WATCHED_OPS: Map<String, UsageKind> = mapOf(
    AppOpsManager.OPSTR_RECORD_AUDIO to UsageKind.MICROPHONE,
    AppOpsManager.OPSTR_CAMERA to UsageKind.CAMERA,
    AppOpsManager.OPSTR_FINE_LOCATION to UsageKind.LOCATION,
    AppOpsManager.OPSTR_COARSE_LOCATION to UsageKind.LOCATION,
    "android:monitor_location" to UsageKind.LOCATION,
    "android:monitor_high_power_location" to UsageKind.LOCATION,
)

/**
 * Which sensitive resources some app is using right now. All false is both the resting state and
 * what the monitor falls back to whenever it can't read the truth, so the island never shows a dot
 * it isn't sure about.
 */
@Immutable
data class PermissionUsage(
    val microphone: Boolean = false,
    val camera: Boolean = false,
    val location: Boolean = false,
) {
    /** Whether anything at all is in use, i.e. whether there is a dot to draw. */
    val any: Boolean get() = microphone || camera || location

    /** How many dots the island will draw, used to reserve room for them on the pill. */
    val count: Int get() = listOf(microphone, camera, location).count { it }
}

/**
 * Reports live microphone, camera and location use, as a [StateFlow] the island collects.
 *
 * Android deliberately gives an ordinary app no way to see this: since Android 10 the audio
 * recording callbacks are filtered down to the app's own recordings, and the green dot in the status
 * bar is drawn by SystemUI with no API behind it. The honest source is `IAppOpsService`, which needs
 * `android.permission.GET_APP_OPS_STATS` — a privileged permission we can never hold, but shell
 * does, which is what Shizuku lends us. Reflection rather than a stub AIDL for the same reason as
 * [StatusBarIconController]: transaction IDs are positional, so reflecting on the framework's own
 * class is what keeps this working across releases.
 *
 * There is no callback route to use instead. `startWatchingActive` reports other apps only to a
 * caller holding a signature permission no shell grant can supply, and the interface it calls back
 * on changed shape in Android 12. Polling is the version-stable option, and it only runs while the
 * user has the feature switched on.
 */
object PermissionUsageMonitor {

    private val _usage = MutableStateFlow(PermissionUsage())
    val usage: StateFlow<PermissionUsage> = _usage.asStateFlow()

    @Volatile
    private var service: Any? = null

    /** The numeric codes for [WATCHED_OPS], resolved once per process from their names. */
    @Volatile
    private var watchedCodes: IntArray? = null

    /**
     * Polls app-op state for as long as the user wants the dots and Shizuku can answer, and reports
     * nothing the rest of the time. Safe to call once, from the app.
     *
     * There is deliberately no `stop()`: the [android.app.Application] owns this lifetime and the
     * polling already stops on its own whenever either condition drops, so a public stop would only
     * be a way to strand the state at whatever it last read.
     */
    fun start(context: Context, scope: CoroutineScope) {
        val preferences = PermissionDotPreferences(context)
        val ownPackage = context.packageName
        scope.launch {
            combine(
                preferences.enabled,
                preferences.kinds,
                ShizukuState.status,
                PermissionDotPreviewBus.active,
            ) { enabled, kinds, status, preview ->
                when {
                    // The settings screen is open: report every watched resource as in use so the
                    // dots can be seen on the real cutout, whatever Shizuku is doing.
                    preview -> Reading(kinds, poll = false)
                    // Nothing to report: the feature is off, Shizuku can't answer, or the user has
                    // switched every resource off.
                    !enabled || !kinds.any || status != ShizukuStatus.READY -> null
                    else -> Reading(kinds, poll = true)
                }
            }
                .distinctUntilChanged()
                .collectLatest { reading ->
                    if (reading == null) {
                        // A dead Shizuku also invalidates the cached proxy; drop it so the next
                        // successful call rebuilds one over the fresh binder.
                        service = null
                        _usage.value = PermissionUsage()
                        return@collectLatest
                    }
                    val kinds = reading.kinds
                    if (!reading.poll) {
                        _usage.value = PermissionUsage(
                            microphone = kinds.microphone,
                            camera = kinds.camera,
                            location = kinds.location,
                        )
                        return@collectLatest
                    }
                    while (true) {
                        _usage.value = read(ownPackage).maskedBy(kinds)
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }
    }

    /** What the monitor should report right now: which resources to watch, and whether to read app ops for them. */
    private data class Reading(val kinds: PermissionDotKinds, val poll: Boolean)

    /** Drops the resources the user isn't watching, so a switched-off one never lights a dot. */
    private fun PermissionUsage.maskedBy(kinds: PermissionDotKinds) = PermissionUsage(
        microphone = microphone && kinds.microphone,
        camera = camera && kinds.camera,
        location = location && kinds.location,
    )

    /**
     * Reads one snapshot of who is using what, ignoring our own package so the island never reports
     * itself. Returns an empty [PermissionUsage] when the call didn't go through, which on a
     * [ShizukuStatus.READY] bridge means the OS rejected or moved the hidden API.
     */
    private fun read(ownPackage: String): PermissionUsage = runCatching {
        val codes = watchedCodes ?: resolveWatchedCodes().also { watchedCodes = it }
        if (codes.isEmpty()) return PermissionUsage()
        val appOps = service ?: buildService().also { service = it }

        var microphone = false
        var camera = false
        var location = false
        for (packageOps in appOps.packagesForOps(codes)) {
            if (packageOps.call("getPackageName") == ownPackage) continue
            val entries = packageOps.call("getOps") as? List<*> ?: continue
            for (entry in entries.filterNotNull()) {
                if (entry.call("isRunning") != true) continue
                when (WATCHED_OPS[entry.call("getOpStr")]) {
                    UsageKind.MICROPHONE -> microphone = true
                    UsageKind.CAMERA -> camera = true
                    UsageKind.LOCATION -> location = true
                    null -> Unit
                }
            }
        }
        PermissionUsage(microphone = microphone, camera = camera, location = location)
    }.getOrElse { error ->
        Log.w(TAG, "Could not read app-op usage", error)
        service = null
        PermissionUsage()
    }

    /**
     * Turns [WATCHED_OPS]' names into the numeric codes `IAppOpsService` speaks, dropping any name
     * this Android version doesn't know rather than failing the whole lookup.
     */
    private fun resolveWatchedCodes(): IntArray {
        val strOpToOp = Class.forName("android.app.AppOpsManager")
            .getMethod("strOpToOp", String::class.java)
        return WATCHED_OPS.keys
            .mapNotNull { name -> runCatching { strOpToOp.invoke(null, name) as Int }.getOrNull() }
            .toIntArray()
    }

    /**
     * Reflects `IAppOpsService` out from behind a Shizuku binder wrapper, so the query runs with
     * shell's identity and passes the `GET_APP_OPS_STATS` check. Hidden and non-SDK, which is why
     * the app lifts the hidden-API restriction at startup.
     */
    private fun buildService(): Any {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.APP_OPS_SERVICE))
        return Class.forName("com.android.internal.app.IAppOpsService\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
            ?: error("IAppOpsService.asInterface returned null")
    }

    /**
     * Invokes `getPackagesForOps(int[])`, falling back to the device-aware overload Android 15 added
     * beside it. An unknown shape yields no packages rather than throwing, so a future change to the
     * interface shows up as absent dots instead of a crash.
     *
     * The elements are `AppOpsManager.PackageOps`, which is hidden — hence the untyped list, read
     * back through [call].
     */
    private fun Any.packagesForOps(ops: IntArray): List<Any> {
        val overloads = javaClass.methods.filter { it.name == "getPackagesForOps" }
        val byOps = overloads.firstOrNull {
            it.parameterTypes.contentEquals(arrayOf(IntArray::class.java))
        }
        val result = when {
            byOps != null -> byOps.invoke(this, ops)
            else -> overloads.firstOrNull {
                it.parameterTypes.contentEquals(arrayOf(IntArray::class.java, String::class.java))
            }?.invoke(this, ops, DEFAULT_DEVICE_ID)
        }
        return (result as? List<*>)?.filterNotNull().orEmpty()
    }

    /**
     * Reads a no-argument getter off one of the hidden app-op records. Their classes aren't in the
     * SDK, so the whole chain from `PackageOps` down to each `OpEntry` is walked by name.
     */
    private fun Any.call(name: String): Any? = javaClass.getMethod(name).invoke(this)

    /** How often the app-op snapshot is re-read while the feature is on. */
    private const val POLL_INTERVAL_MS = 1_000L

    /**
     * `VirtualDeviceManager.PERSISTENT_DEVICE_ID_DEFAULT` — the physical device rather than a
     * virtual one. Spelled out because the constant is newer than our compile SDK.
     */
    private const val DEFAULT_DEVICE_ID = "default"
}
