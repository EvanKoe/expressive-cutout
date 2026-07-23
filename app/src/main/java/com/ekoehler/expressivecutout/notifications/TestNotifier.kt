package com.ekoehler.expressivecutout.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus

/**
 * Posts a real system notification so the user can confirm notifications work and see how
 * one looks. It also nudges the island directly: the listener deliberately ignores the
 * app's own posts, so we emit the preview signal here to guarantee the island reacts.
 */
object TestNotifier {

    private const val CHANNEL_ID = "test"
    private const val NOTIFICATION_ID = 4711

    /** True once a notification can actually be posted (Android 13+ gates this at runtime). */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // Guarded by canPost() below; the lint check can't see through the runtime helper.
    @SuppressLint("MissingPermission")
    fun send(context: Context) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (canPost(context)) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        // Show it on the island immediately, regardless of the listener's self-filter.
        IslandEventBus.emit(
            CutoutSignal.Notification(
                packageName = context.packageName,
                title = context.getString(R.string.test_notification_title),
            ),
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }
}
