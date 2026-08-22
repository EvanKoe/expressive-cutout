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
import com.ekoehler.expressivecutout.service.ProgressData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.RemoteInput as PlatformRemoteInput

/**
 * Posts a real system notification so the user can confirm notifications work and see how
 * one looks. It also nudges the island directly: the listener deliberately ignores the
 * app's own posts, so we emit the preview signal here to guarantee the island reacts.
 */
object TestNotifier {

    private const val CHANNEL_ID = "test"
    const val NOTIFICATION_ID = 4711
    const val PROGRESS_NOTIFICATION_ID = 4712
    const val PLAIN_NOTIFICATION_ID = 4713

    /** The notification auto-dismisses after this long so the test never lingers. */
    private const val TIMEOUT_MS = 15_000L

    private const val PROGRESS_MAX = 100
    private const val PROGRESS_STEP = 5
    private const val PROGRESS_SWEEP_MS = 5_000L
    private const val PROGRESS_KEY = "test-progress"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    /** True once a notification can actually be posted (Android 13+ gates this at runtime). */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Posts the sample notification used to preview the island without waiting for a real one,
     * complete with reply and mark-read actions.
     */
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
                appName = context.getString(R.string.app_name),
                postTimeMs = System.currentTimeMillis(),
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

    /**
     * Posts a test notification carrying no actions at all, and mirrors it onto the island. The
     * counterpart to [send]: the expanded cutout then renders only the header row, which is the
     * layout where the title has the least room and can ride up under the camera hole. Its text is
     * deliberately long enough to wrap onto a second line, the case that pushes the header highest.
     */
    @SuppressLint("MissingPermission")
    fun sendPlain(context: Context) {
        ensureChannel(context)

        val title = context.getString(R.string.test_plain_notification_title)
        val text = context.getString(R.string.test_plain_notification_text)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .build()

        if (canPost(context)) {
            NotificationManagerCompat.from(context).notify(PLAIN_NOTIFICATION_ID, notification)
        }

        IslandEventBus.emit(
            CutoutSignal.Notification(
                packageName = context.packageName,
                title = title,
                text = text,
                smallIcon = Icon.createWithResource(context, R.drawable.ic_stat_island),
            ),
        )
    }

    /**
     * Posts a real progress notification and mirrors it onto the island, sweeping the bar from 0 to
     * [PROGRESS_MAX] over [PROGRESS_SWEEP_MS] so the progress pipeline (extras -> [ProgressData] ->
     * the cutout) can be watched filling up without waiting on a real download. Each step re-posts
     * the system notification and re-emits the island signal under a stable [PROGRESS_KEY], which the
     * overlay updates in place — the same channel a real app re-posting its download uses. Re-tapping
     * cancels any sweep already running.
     */
    @SuppressLint("MissingPermission")
    fun sendProgress(context: Context) {
        ensureChannel(context)
        val appContext = context.applicationContext
        val title = appContext.getString(R.string.test_progress_notification_title)
        val text = appContext.getString(R.string.test_progress_notification_text)

        progressJob?.cancel()
        progressJob = scope.launch {
            var current = 0
            while (true) {
                val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_island)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOnlyAlertOnce(true)
                    .setProgress(PROGRESS_MAX, current, false)
                    .setTimeoutAfter(TIMEOUT_MS)
                    .build()

                if (canPost(appContext)) {
                    NotificationManagerCompat.from(appContext).notify(PROGRESS_NOTIFICATION_ID, notification)
                }

                IslandEventBus.emit(
                    CutoutSignal.Notification(
                        packageName = appContext.packageName,
                        title = title,
                        text = text,
                        appName = appContext.getString(R.string.app_name),
                        postTimeMs = System.currentTimeMillis(),
                        key = PROGRESS_KEY,
                        smallIcon = Icon.createWithResource(appContext, R.drawable.ic_stat_island),
                        progressData = ProgressData(
                            max = PROGRESS_MAX,
                            current = current,
                            isIndeterminate = false,
                            title = title,
                        ),
                    ),
                )

                if (current >= PROGRESS_MAX) break
                delay(PROGRESS_SWEEP_MS * PROGRESS_STEP / PROGRESS_MAX)
                current = (current + PROGRESS_STEP).coerceAtMost(PROGRESS_MAX)
            }
        }
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

    /**
     * Creates the notification channel on first use, tolerating an existing one so a reinstall
     * doesn't duplicate it.
     */
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
