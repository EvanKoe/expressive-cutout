package com.ekoehler.expressivecutout.system

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/** The Shizuku app's package, needed to tell "not installed" apart from "not running". */
private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

/** Arbitrary code echoed back to [Shizuku.OnRequestPermissionResultListener]. */
private const val PERMISSION_REQUEST_CODE = 4919

/**
 * Every state the Shizuku bridge can be in, ordered from least to most usable. Only [READY] means
 * privileged calls will actually go through.
 */
enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY,
}

/**
 * Tracks whether Shizuku is reachable and permitted, as a [StateFlow] the UI can collect.
 *
 * Shizuku is a process running as shell that lends its privileges to apps that ask. It has to be
 * started by hand after every reboot (wireless debugging or ADB), so this state changes underneath
 * us at any time — hence the binder listeners rather than a one-off check at startup.
 */
object ShizukuState {

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var appContext: Context? = null

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult =
        Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    /**
     * Registers the binder listeners and seeds the initial state. Safe to call once, from the app.
     *
     * There is deliberately no `stop()`: the [Application] owns this lifetime, and Android never
     * calls a reliable teardown on it, so the listeners live until the process dies and are
     * released with it.
     */
    fun start(context: Context) {
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refresh()
    }

    /** Re-reads the live state. Call after returning from Shizuku, since it may have been started. */
    fun refresh() {
        _status.value = currentStatus()
    }

    /**
     * Works out the live Shizuku status, distinguishing not-installed from installed-but-stopped
     * and from an old version whose handshake this app no longer speaks.
     */
    private fun currentStatus(): ShizukuStatus {
        val context = appContext ?: return ShizukuStatus.NOT_INSTALLED
        if (!isInstalled(context)) return ShizukuStatus.NOT_INSTALLED
        // pingBinder is the only honest "is it running" check — the grant can read fine while the
        // process is gone, exactly like the accessibility service after an app update.
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return ShizukuStatus.NOT_RUNNING
        // Pre-v11 Shizuku used a different, now-removed permission handshake; treat it as needing
        // attention rather than silently failing every privileged call.
        if (Shizuku.isPreV11()) return ShizukuStatus.PERMISSION_REQUIRED
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return if (granted) ShizukuStatus.READY else ShizukuStatus.PERMISSION_REQUIRED
    }

    /** Asks Shizuku for access; the result arrives via the listener and updates [status]. */
    fun requestPermission() {
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
    }

    /** Whether the Shizuku app is installed, treating any lookup failure as absent. */
    private fun isInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    }.getOrDefault(false)
}
