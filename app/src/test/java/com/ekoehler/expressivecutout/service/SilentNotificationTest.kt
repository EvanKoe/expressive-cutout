package com.ekoehler.expressivecutout.service

import android.app.NotificationManager
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.data.BehaviourSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilentNotificationTest {

    @Test
    fun testBehaviourSettingsDefaultIgnoreSilentNotifications() {
        val settings = BehaviourSettings()
        assertFalse("Default ignoreSilentNotifications should be false", settings.ignoreSilentNotifications)
    }

    @Test
    fun testNotificationClassifierIdentifiesSilentNotifications() {
        // Importance LOW, MIN, NONE should be classified as silent
        assertTrue(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_LOW, isAmbient = false))
        assertTrue(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_MIN, isAmbient = false))
        assertTrue(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_NONE, isAmbient = false))
        assertTrue(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_HIGH, isAmbient = true))

        // Importance DEFAULT, HIGH, MAX should NOT be silent
        assertFalse(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_DEFAULT, isAmbient = false))
        assertFalse(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_HIGH, isAmbient = false))
        assertFalse(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_MAX, isAmbient = false))
    }

    @Test
    fun testCutoutSignalNotificationCarriesIsSilent() {
        val silentNotification = CutoutSignal.Notification(
            packageName = "com.example.chat",
            title = "Test",
            text = "Message",
            isSilent = true,
        )
        assertTrue(silentNotification.isSilent)

        val defaultNotification = CutoutSignal.Notification(
            packageName = "com.example.chat",
            title = "Test",
            text = "Message",
        )
        assertFalse(defaultNotification.isSilent)
    }

    @Test
    fun testNotificationClassifierFallbackPriority() {
        // When importance is unspecified or null, priority is used as fallback
        assertTrue(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_UNSPECIFIED, priority = -1))
        assertTrue(NotificationClassifier.isSilent(importance = null, priority = -2))
        assertFalse(NotificationClassifier.isSilent(importance = NotificationManager.IMPORTANCE_UNSPECIFIED, priority = 0))
        assertFalse(NotificationClassifier.isSilent(importance = null, priority = 1))
    }

    @Test
    fun testSilentNotificationFilteringLogic() {
        val silentSignal = CutoutSignal.Notification(
            packageName = "com.example.app",
            title = "Silent Alert",
            isSilent = true,
        )
        val alertingSignal = CutoutSignal.Notification(
            packageName = "com.example.app",
            title = "Loud Alert",
            isSilent = false,
        )

        fun shouldShowBubble(signal: CutoutSignal.Notification, ignoreSilent: Boolean): Boolean {
            if (ignoreSilent && signal.isSilent) return false
            return true
        }

        // When toggle is OFF: both silent and alerting notifications show bubbles
        assertTrue(shouldShowBubble(alertingSignal, ignoreSilent = false))
        assertTrue(shouldShowBubble(silentSignal, ignoreSilent = false))

        // When toggle is ON: alerting shows bubble, silent is suppressed
        assertTrue(shouldShowBubble(alertingSignal, ignoreSilent = true))
        assertFalse(shouldShowBubble(silentSignal, ignoreSilent = true))
    }
}
