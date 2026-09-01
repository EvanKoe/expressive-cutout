package com.ekoehler.expressivecutout.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the buttons on the in-app test notification so tapping them actually does something the
 * user can see. "Reply" packs typed text through a [RemoteInput]; the plain action just confirms.
 * Both dismiss the notification, mirroring how a real messaging app finishes an inline reply.
 */
class TestReplyReceiver : BroadcastReceiver() {

    /**
     * Handles the test notification's reply and mark-read actions, turning the typed text into the
     * confirmation the island shows.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val message = when (intent.action) {
            ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_REPLY)
                    ?.toString()
                    .orEmpty()
                appContext.getString(R.string.test_notification_reply_sent, text)
            }

            ACTION_ARCHIVE -> appContext.getString(R.string.test_notification_archived)

            else -> appContext.getString(R.string.test_notification_marked_read)
        }

        // Keep the receiver alive past onReceive so the "Toast on action" preference can be read off
        // the main thread; goAsync() gives us a short window, which finish() closes when we're done.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toastOnAction = BehaviourPreferences(appContext).settings.first().toastOnAction
                withContext(Dispatchers.Main) {
                    if (toastOnAction) {
                        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
                    }
                    NotificationManagerCompat.from(appContext).cancel(TestNotifier.NOTIFICATION_ID)
                    NotificationManagerCompat.from(appContext).cancel(TestNotifier.MULTILINE_NOTIFICATION_ID)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.ekoehler.expressivecutout.action.TEST_REPLY"
        const val ACTION_MARK_READ = "com.ekoehler.expressivecutout.action.TEST_MARK_READ"
        const val ACTION_ARCHIVE = "com.ekoehler.expressivecutout.action.TEST_ARCHIVE"
        const val KEY_REPLY = "key_test_reply"
    }
}
