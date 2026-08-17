package com.ekoehler.expressivecutout.system

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private const val TAG = "StatusBarIcons"

/** Hidden `StatusBarManager` disable flags. Only the ones we use are named here. */
private const val DISABLE_NONE = 0x00000000
private const val DISABLE_NOTIFICATION_ICONS = 0x00020000
private const val DISABLE_NOTIFICATION_ALERTS = 0x00040000
private const val DISABLE_SYSTEM_INFO = 0x00100000

/**
 * Hides the system status bar's notification icons through Shizuku, so the island is the only thing
 * reporting notifications.
 *
 * The call itself is `IStatusBarService.disable`, which needs `android.permission.STATUS_BAR` — a
 * privileged permission we can never hold, but shell does, which is what Shizuku lends us.
 *
 * Two things drive the design:
 *
 * - **Reflection, not a stub AIDL.** AIDL transaction IDs are positional, so a hand-written
 *   `IStatusBarService.aidl` would have to match the running OS's method order exactly and would
 *   break on the next Android release. Reflecting on the framework's own class always matches.
 * - **[token] must outlive the call.** `StatusBarManagerService` keeps the disable flags in a record
 *   keyed by this binder and drops them when it dies, so the token is held for the process lifetime.
 *   The upshot is a free safety net: if our process is killed the icons come back on their own, and
 *   [start] re-applies them next launch.
 */
object StatusBarIconController {

    private val token = Binder()

    @Volatile
    private var service: Any? = null

    /**
     * Keeps the system status bar in sync with the saved wish, re-applying whenever Shizuku becomes
     * reachable again — after a reboot, or after the user starts Shizuku for the first time.
     */
    fun start(context: Context, scope: CoroutineScope) {
        val preferences = StatusBarPreferences(context)
        val packageName = context.packageName
        scope.launch {
            combine(
                preferences.hideNotificationIcons,
                preferences.hideSystemInfo,
                preferences.silenceAlerts,
                ShizukuState.status,
            ) { hideIcons, hideSystemInfo, silenceAlerts, status ->
                Wish(hideIcons, hideSystemInfo, silenceAlerts, status)
            }
                .collect { wish ->
                    if (wish.status != ShizukuStatus.READY) {
                        // A dead Shizuku also invalidates the cached proxy; drop it so the next
                        // successful call rebuilds one over the fresh binder.
                        service = null
                        return@collect
                    }
                    apply(wish.hideIcons, wish.hideSystemInfo, wish.silenceAlerts, packageName)
                }
        }
    }

    /**
     * Applies or clears the status-bar disable flags in one call — the flags are a single bitmask
     * held against [token], so every wish must be pushed together or a later call would clear the
     * earlier ones. Returns false when the call didn't go through, which on a [ShizukuStatus.READY]
     * bridge means the OS rejected or moved the hidden API.
     */
    fun apply(
        hideIcons: Boolean,
        hideSystemInfo: Boolean,
        silenceAlerts: Boolean,
        packageName: String,
    ): Boolean = runCatching {
        var flags = DISABLE_NONE
        if (hideIcons) flags = flags or DISABLE_NOTIFICATION_ICONS
        if (hideSystemInfo) flags = flags or DISABLE_SYSTEM_INFO
        if (silenceAlerts) flags = flags or DISABLE_NOTIFICATION_ALERTS
        val statusBar = service ?: buildService().also { service = it }
        statusBar.disable(flags, packageName)
        true
    }.getOrElse { error ->
        Log.w(
            TAG,
            "Could not apply status-bar flags " +
                "(icons=$hideIcons, systemInfo=$hideSystemInfo, alerts=$silenceAlerts)",
            error,
        )
        service = null
        false
    }

    /** The full set of status-bar wishes plus the bridge state, combined for [start]. */
    private data class Wish(
        val hideIcons: Boolean,
        val hideSystemInfo: Boolean,
        val silenceAlerts: Boolean,
        val status: ShizukuStatus,
    )

    private fun buildService(): Any {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("statusbar"))
        return Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
            ?: error("IStatusBarService.asInterface returned null")
    }

    /**
     * Invokes `disable(int, IBinder, String)`, falling back to the per-user overload on builds that
     * only expose that one.
     */
    private fun Any.disable(flags: Int, packageName: String) {
        val disable = runCatching {
            javaClass.getMethod(
                "disable",
                Int::class.javaPrimitiveType,
                IBinder::class.java,
                String::class.java,
            )
        }.getOrNull()
        if (disable != null) {
            disable.invoke(this, flags, token, packageName)
            return
        }
        javaClass.getMethod(
            "disableForUser",
            Int::class.javaPrimitiveType,
            IBinder::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
        ).invoke(this, flags, token, packageName, android.os.Process.myUserHandle().hashCode())
    }
}
