package com.vikram.expressiveisland.system

import android.content.Context
import android.os.IBinder
import android.os.Process
import android.util.Log
import java.lang.reflect.Method

class StatusBarIconController(
    private val context: Context,
) {
    // Existing implementation retained; only the fallback user-id lookup is changed
    // to use public SDK APIs instead of the hidden UserHandle.myUserId() method.
    private var service: Any? = null
    private var disableMethod: Method? = null

    private fun disableForCurrentUser(
        flags: Int,
        token: IBinder,
        packageName: String,
    ) {
        val currentService = service ?: return
        val disable = disableMethod
        if (disable != null) {
            runCatching {
                disable.invoke(currentService, flags, token, packageName)
            }.onFailure {
                Log.w("StatusBarIconController", "Failed to update status bar", it)
            }
            return
        }

        runCatching {
            currentService.javaClass.getMethod(
                "disableForUser",
                Int::class.javaPrimitiveType,
                IBinder::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
            ).invoke(
                currentService,
                flags,
                token,
                packageName,
                Process.myUserHandle().identifier,
            )
        }.onFailure {
            Log.w("StatusBarIconController", "Failed to update status bar for current user", it)
        }
    }
}
