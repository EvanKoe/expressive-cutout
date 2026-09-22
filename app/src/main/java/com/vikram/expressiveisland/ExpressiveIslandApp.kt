package com.vikram.expressiveisland

import android.app.Application
import com.vikram.expressiveisland.system.PermissionUsageMonitor
import com.vikram.expressiveisland.system.ShizukuState
import com.vikram.expressiveisland.system.StatusBarIconController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** Application entry point for process-lifetime system integrations. */
class ExpressiveIslandApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        HiddenApiBypass.addHiddenApiExemptions("")
        ShizukuState.start(this)
        StatusBarIconController.start(this, appScope)
        PermissionUsageMonitor.start(this, appScope)
    }
}
