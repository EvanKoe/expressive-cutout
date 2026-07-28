package com.ekoehler.expressivecutout.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Backs the hang-up button on the in-app test call ([TestCaller]): tapping it ends the fake call by
 * clearing [com.ekoehler.expressivecutout.core.OnCallBus], so the phone tile dismisses just as it
 * would when a real dialer removes its ongoing-call notification.
 */
class TestCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_END) TestCaller.end()
    }

    companion object {
        const val ACTION_END = "com.ekoehler.expressivecutout.action.TEST_END_CALL"
    }
}
