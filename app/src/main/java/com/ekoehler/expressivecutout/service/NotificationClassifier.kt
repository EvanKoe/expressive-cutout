package com.ekoehler.expressivecutout.service

import android.app.Notification
import android.app.NotificationManager

object NotificationClassifier {
    /**
     * Determines if a notification is classified as silent (low/min/none importance or ambient).
     */
    fun isSilent(
        importance: Int?,
        isAmbient: Boolean = false,
        priority: Int = Notification.PRIORITY_DEFAULT,
    ): Boolean {
        if (isAmbient) return true
        if (importance != null && importance != NotificationManager.IMPORTANCE_UNSPECIFIED) {
            return importance < NotificationManager.IMPORTANCE_DEFAULT
        }
        return priority < Notification.PRIORITY_DEFAULT
    }
}
