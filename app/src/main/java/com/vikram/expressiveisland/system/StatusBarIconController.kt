package com.vikram.expressiveisland.system

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.vikram.expressiveisland.data.StatusBarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private const val TAG = "StatusBarIcons"
private const val DISABLE_NONE = 0x00000000
private const val DISABLE_NOTIFICATION_ICONS = 0x00020000
private const val DISABLE_NOTIFICATION_ALERTS = 0x00040000
private const val DISABLE_SYSTEM_INFO = 0x00100000
private const val DISABLE_CLOCK = 0x00800000

/**
 * Hides selected system status-bar elements through Shizuku. The binder token is retained for the
 * process lifetime so Android restores the system UI automatically when this process dies.
 */
object StatusBarIconController {

    private val token = Binder()

    @Volatile
    private var service: Any? = null

    fun start(context: Context, scope: CoroutineScope) {
        val preferences = StatusBarPreferences(context)
        val packageName = context.packageName
        scope.launch {
            combine(
                preferences.hideNotificationIcons,
                preferences.hideSystemInfo,
                preferences.hideClock,
                preferences.silenceAlerts,
                ShizukuState.status,
            ) { hideIcons, hideSystemInfo, hideClock, silenceAlerts, status ->
                Wish(hideIcons, hideSystemInfo, hideClock, silenceAlerts, status)
            }.collect { wish ->
                if (wish.status != ShizukuStatus.READY) {
                    service = null
                    return@collect
                }
                apply(
                    wish.hideIcons,
                    wish.hideSystemInfo,
                    wish.hideClock,
                    wish.silenceAlerts,
                    packageName,
                )
            }
        }
    }

    fun apply(
        hideIcons: Boolean,
        hideSystemInfo: Boolean,
        hideClock: Boolean,
        silenceAlerts: Boolean,
        packageName: String,
    ): Boolean = runCatching {
        var flags = DISABLE_NONE
        if (hideIcons) flags = flags or DISABLE_NOTIFICATION_ICONS
        if (hideSystemInfo) flags = flags or DISABLE_SYSTEM_INFO
        if (hideClock) flags = flags or DISABLE_CLOCK
        if (silenceAlerts) flags = flags or DISABLE_NOTIFICATION_ALERTS
        val statusBar = service ?: buildService().also { service = it }
        statusBar.disable(flags, packageName)
        true
    }.getOrElse { error ->
        Log.w(
            TAG,
            "Could not apply status-bar flags " +
                "(icons=$hideIcons, systemInfo=$hideSystemInfo, clock=$hideClock, alerts=$silenceAlerts)",
            error,
        )
        service = null
        false
    }

    private data class Wish(
        val hideIcons: Boolean,
        val hideSystemInfo: Boolean,
        val hideClock: Boolean,
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

        // Some Android builds expose only disableForUser. The current user's hash is accepted by
        // the framework implementation without relying on the hidden UserHandle.myUserId() API.
        javaClass.getMethod(
            "disableForUser",
            Int::class.javaPrimitiveType,
            IBinder::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
        ).invoke(this, flags, token, packageName, android.os.Process.myUserHandle().hashCode())
    }
}
