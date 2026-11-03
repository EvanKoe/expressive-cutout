package com.ekoehler.expressivecutout.system

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.ekoehler.expressivecutout.core.CutoutKind
import com.ekoehler.expressivecutout.core.CutoutWidthBus
import com.ekoehler.expressivecutout.core.DeviceLockBus
import com.ekoehler.expressivecutout.data.LockscreenRule
import com.ekoehler.expressivecutout.data.StatusBarHideMode
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/** Log tag for the reflection below, which reports failures rather than throwing. */
private const val TAG = "StatusBarIcons"

/** Hidden `StatusBarManager` disable flags. Only the ones we use are named here. */
private const val DISABLE_NONE = 0x00000000
private const val DISABLE_NOTIFICATION_ICONS = 0x00020000
private const val DISABLE_NOTIFICATION_ALERTS = 0x00040000
private const val DISABLE_SYSTEM_INFO = 0x00100000
private const val DISABLE_CLOCK = 0x00800000

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
     * reachable again — after a reboot, or after the user starts Shizuku for the first time. A
     * setting left on [StatusBarHideMode.AUTO] takes its wish from the island's own width, read
     * from [CutoutWidthBus] and the per-setting cutout triggers, so the bar empties and refills as the cutout grows and shrinks.
     * While the keyguard is up ([DeviceLockBus]) a setting may swap to its own [LockscreenRule]
     * instead.
     *
     * There is deliberately no `stop()`, and adding one would be a mistake: releasing [token] is
     * what restores the system icons, so a public stop would be a way to silently undo the user's
     * setting. The process dying is the only thing that should clear these flags, which is the
     * safety net described above.
     */
    fun start(context: Context, scope: CoroutineScope) {
        val preferences = StatusBarPreferences(context)
        val packageName = context.packageName
        val modeChoices = combine(
            preferences.notificationIconsMode,
            preferences.systemInfoMode,
            preferences.clockMode,
        ) { icons, systemInfo, clock ->
            Triple(icons, systemInfo, clock)
        }
        val triggers = combine(
            preferences.notificationIconsAutoTriggers,
            preferences.systemInfoAutoTriggers,
            preferences.clockAutoTriggers,
        ) { icons, systemInfo, clock ->
            Triple(icons, systemInfo, clock)
        }
        val lockRules = combine(
            preferences.notificationIconsLockscreen,
            preferences.systemInfoLockscreen,
            preferences.clockLockscreen,
        ) { icons, systemInfo, clock ->
            Triple(icons, systemInfo, clock)
        }
        val modes = combine(
            modeChoices,
            triggers,
            lockRules,
            DeviceLockBus.locked,
        ) { (icons, systemInfo, clock), (iconT, systemT, clockT), (iconL, systemL, clockL), locked ->
            Modes(icons, systemInfo, clock, iconT, systemT, clockT, iconL, systemL, clockL, locked)
        }
        scope.launch {
            combine(
                modes,
                preferences.silenceAlerts,
                ShizukuState.status,
                CutoutWidthBus.kind,
                combine(CutoutWidthBus.grownForDots, CutoutWidthBus.grownForStatusDot) { trailing, leading ->
                    trailing to leading
                },
            ) { chosen, silenceAlerts, status, kind, (grownForDots, grownForStatusDot) ->
                Wish(chosen.resolve(kind, grownForDots, grownForStatusDot), silenceAlerts, status)
            }
                .collect { wish ->
                    if (wish.status != ShizukuStatus.READY) {
                        // A dead Shizuku also invalidates the cached proxy; drop it so the next
                        // successful call rebuilds one over the fresh binder.
                        service = null
                        return@collect
                    }
                    apply(
                        wish.hiding.hideIcons,
                        wish.hiding.hideSystemInfo,
                        wish.hiding.hideClock,
                        wish.silenceAlerts,
                        packageName,
                    )
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

    /**
     * The three hiding settings as the user chose them, each with the set of [CutoutKind]s that
     * switch it on while it is left on [StatusBarHideMode.AUTO], plus each one's [LockscreenRule]
     * and whether the keyguard is up right now. Combined for [start].
     */
    private data class Modes(
        val notificationIcons: StatusBarHideMode,
        val systemInfo: StatusBarHideMode,
        val clock: StatusBarHideMode,
        val iconTriggers: Set<CutoutKind>,
        val systemInfoTriggers: Set<CutoutKind>,
        val clockTriggers: Set<CutoutKind>,
        val iconLockscreen: LockscreenRule,
        val systemInfoLockscreen: LockscreenRule,
        val clockLockscreen: LockscreenRule,
        val locked: Boolean,
    ) {
        /**
         * What each setting works out to right now, given the [kind] of cutout drawn (null while the
         * island is down). While [locked], a setting whose [LockscreenRule] is split off is judged
         * by that rule's mode and triggers instead of the base ones.
         *
         * [grownForDots] counts for the system icons alone: the pill grows on its trailing edge to
         * seat the permission dots, which is the end of the bar those icons live at, and nowhere
         * near the clock or the notification icons opposite them. [grownForStatusDot] is the mirror:
         * the pill grows on its leading edge to seat the status dot, so it counts for the
         * notification icons alone. Both only apply to a setting left on AUTO.
         */
        fun resolve(kind: CutoutKind?, grownForDots: Boolean, grownForStatusDot: Boolean): Hiding {
            val (iconsMode, iconsTriggers) = effective(notificationIcons, iconTriggers, iconLockscreen)
            val (systemMode, systemTriggers) = effective(systemInfo, systemInfoTriggers, systemInfoLockscreen)
            val (clockMode, clockTriggersNow) = effective(clock, clockTriggers, clockLockscreen)
            return Hiding(
                hideIcons = iconsMode.hides(kind, iconsTriggers) ||
                    (iconsMode == StatusBarHideMode.AUTO && grownForStatusDot),
                hideSystemInfo = systemMode.hides(kind, systemTriggers) ||
                    (systemMode == StatusBarHideMode.AUTO && grownForDots),
                hideClock = clockMode.hides(kind, clockTriggersNow),
            )
        }

        /**
         * The mode and triggers a setting runs on right now: its lock-screen [rule] while the
         * keyguard is up and the rule is split off, the base choice otherwise.
         */
        private fun effective(
            base: StatusBarHideMode,
            baseTriggers: Set<CutoutKind>,
            rule: LockscreenRule,
        ): Pair<StatusBarHideMode, Set<CutoutKind>> =
            if (locked && rule.separate) rule.mode to rule.triggers else base to baseTriggers
    }

    /** What the three settings work out to for the status bar as it stands. */
    private data class Hiding(
        val hideIcons: Boolean,
        val hideSystemInfo: Boolean,
        val hideClock: Boolean,
    )

    /** The resolved status-bar wishes plus the bridge state, combined for [start]. */
    private data class Wish(
        val hiding: Hiding,
        val silenceAlerts: Boolean,
        val status: ShizukuStatus,
    )

    /**
     * Reflects `IStatusBarService` out from behind a Shizuku binder wrapper. Hidden and non-SDK,
     * which is why the app lifts the hidden-API restriction at startup.
     */
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
