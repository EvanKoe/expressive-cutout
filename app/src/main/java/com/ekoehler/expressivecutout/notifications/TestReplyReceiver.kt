package com.ekoehler.expressivecutout.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.ekoehler.expressivecutout.R

/**
 * Backs the buttons on the in-app test notification so tapping them actually does something the
 * user can see. "Reply" packs typed text through a [RemoteInput]; the plain action just confirms.
 * Both dismiss the notification, mirroring how a real messaging app finishes an inline reply.
 */
class TestReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = when (intent.action) {
            ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_REPLY)
                    ?.toString()
                    .orEmpty()
                context.getString(R.string.test_notification_reply_sent, text)
            }

            else -> context.getString(R.string.test_notification_marked_read)
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        NotificationManagerCompat.from(context).cancel(TestNotifier.NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_REPLY = "com.ekoehler.expressivecutout.action.TEST_REPLY"
        const val ACTION_MARK_READ = "com.ekoehler.expressivecutout.action.TEST_MARK_READ"
        const val KEY_REPLY = "key_test_reply"
    }
}
