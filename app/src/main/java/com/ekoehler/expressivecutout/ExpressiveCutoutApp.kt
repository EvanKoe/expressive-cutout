package com.ekoehler.expressivecutout

import android.app.Application
import com.ekoehler.expressivecutout.system.PermissionUsageMonitor
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.StatusBarIconController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Application entry point. Owns the process-lifetime pieces of the Shizuku bridge: the hidden-API
 * exemption, the binder listeners behind [ShizukuState], and the coroutine scope that re-applies the
 * status-bar flags and re-reads permission usage whenever Shizuku reconnects.
 */
class ExpressiveCutoutApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Starts the singletons that have to outlive any single service or activity, and lifts the
     * hidden-API restriction they need, before anything else in the process runs.
     */
    override fun onCreate() {
        super.onCreate()
        // IStatusBarService is a non-SDK interface, so plain reflection on it is blocked for apps
        // targeting a recent SDK. This lifts the restriction for our process only.
        HiddenApiBypass.addHiddenApiExemptions("")
        ShizukuState.start(this)
        StatusBarIconController.start(this, appScope)
        PermissionUsageMonitor.start(this, appScope)
    }
}
