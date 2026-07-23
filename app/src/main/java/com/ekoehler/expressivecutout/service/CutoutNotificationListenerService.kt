package com.ekoehler.expressivecutout.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus

/**
 * Mirrors freshly posted notifications onto the island. It keeps only the posting
 * package and title — never the message body — and filters out noise (its own posts,
 * group summaries, and ongoing/system-managed notifications).
 */
class CutoutNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (!notification.shouldSurface()) return

        val title = notification.notification.extras
            ?.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()

        IslandEventBus.emit(CutoutSignal.Notification(notification.packageName, title))
    }

    private fun StatusBarNotification.shouldSurface(): Boolean {
        if (packageName == this@CutoutNotificationListenerService.packageName) return false
        val flags = notification.flags
        val isSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
        val isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        return isClearable && !isSummary && !isOngoing
    }
}
