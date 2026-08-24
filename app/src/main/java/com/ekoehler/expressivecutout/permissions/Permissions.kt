package com.ekoehler.expressivecutout.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.service.CutoutAccessibilityService

/**
 * Thin, side-effect-free helpers for querying and requesting the three grants the app
 * needs. Keeping the "is it on?" checks and the "open the right screen" intents together
 * lets the UI stay declarative and re-check state on every resume.
 */
object Permissions {

    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /**
     * Whether the accessibility service is enabled, read from the system's own colon-separated list
     * rather than from the service itself so the answer is right even before it binds.
     */
    fun isAccessibilityGranted(context: Context): Boolean {
        val expected = ComponentName(context, CutoutAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /**
     * Whether the app is exempt from battery optimisation, which it needs for the island to keep
     * reacting after a long idle.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService<PowerManager>() ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openNotificationAccessSettings(context: Context) =
        context.startActivitySafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openAccessibilitySettings(context: Context) =
        context.startActivitySafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /**
     * Opens the direct "ignore battery optimisation" prompt for this app. Falls back to
     * the general list if the OEM blocks the targeted request.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        val targeted = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        if (!context.startActivitySafely(targeted)) {
            context.startActivitySafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    /**
     * Opens the Shizuku app so the user can start it, or its store listing when it isn't installed.
     * Shizuku has to be restarted by hand after every reboot, so this is a link the Status bar
     * screen surfaces often rather than a one-time grant.
     */
    fun openShizuku(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launch != null && context.startActivitySafely(launch)) return
        context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_STORE_URL)))
    }

    private fun Context.startActivitySafely(intent: Intent): Boolean = runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val SHIZUKU_STORE_URL =
        "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"
}
