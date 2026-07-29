package com.ekoehler.expressivecutout.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import android.app.RemoteInput as PlatformRemoteInput

/**
 * Posts a real system notification so the user can confirm notifications work and see how
 * one looks. It also nudges the island directly: the listener deliberately ignores the
 * app's own posts, so we emit the preview signal here to guarantee the island reacts.
 */
object TestNotifier {

    private const val CHANNEL_ID = "test"
    const val NOTIFICATION_ID = 4711

    /** The notification auto-dismisses after this long so the test never lingers. */
    private const val TIMEOUT_MS = 15_000L

    /** True once a notification can actually be posted (Android 13+ gates this at runtime). */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // Guarded by canPost() below; the lint check can't see through the runtime helper.
    @SuppressLint("MissingPermission")
    fun send(context: Context) {
        ensureChannel(context)

        val replyIntent = broadcast(context, requestCode = 1, TestReplyReceiver.ACTION_REPLY)
        val markReadIntent = broadcast(context, requestCode = 2, TestReplyReceiver.ACTION_MARK_READ)
        val replyHint = context.getString(R.string.test_notification_reply_hint)

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_island,
            context.getString(R.string.test_notification_action_reply),
            replyIntent,
        ).addRemoteInput(
            RemoteInput.Builder(TestReplyReceiver.KEY_REPLY).setLabel(replyHint).build(),
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .addAction(replyAction)
            .addAction(
                R.drawable.ic_stat_island,
                context.getString(R.string.test_notification_action_mark_read),
                markReadIntent,
            )
            .build()

        if (canPost(context)) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        // Show it on the island immediately, regardless of the listener's self-filter, wiring the
        // same buttons so the user can try inline reply straight from the island.
        IslandEventBus.emit(
            CutoutSignal.Notification(
                packageName = context.packageName,
                title = context.getString(R.string.test_notification_title),
                text = context.getString(R.string.test_notification_text),
                actions = listOf(
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_notification_action_reply),
                        intent = replyIntent,
                        reply = CutoutSignal.Notification.ReplyInput(
                            resultKey = TestReplyReceiver.KEY_REPLY,
                            remoteInputs = listOf(
                                PlatformRemoteInput.Builder(TestReplyReceiver.KEY_REPLY)
                                    .setLabel(replyHint)
                                    .build(),
                            ),
                            hint = replyHint,
                        ),
                    ),
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_notification_action_mark_read),
                        intent = markReadIntent,
                    ),
                ),
                // The same glyph the posted notification carries, so the preview goes through the
                // real "icon from the notification" path rather than the launcher-icon fallback.
                smallIcon = Icon.createWithResource(context, R.drawable.ic_stat_island),
            ),
        )
    }

    /** A mutable broadcast [PendingIntent] to [TestReplyReceiver]; mutability lets reply text fill in. */
    private fun broadcast(context: Context, requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, TestReplyReceiver::class.java).setAction(action)
        // FLAG_MUTABLE only exists on API 31+; below that, intents are mutable by default.
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
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
