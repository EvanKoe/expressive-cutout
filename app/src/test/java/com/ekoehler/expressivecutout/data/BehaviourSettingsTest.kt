package com.ekoehler.expressivecutout.data

import com.ekoehler.expressivecutout.core.CutoutSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BehaviourSettingsTest {

    @Test
    fun testDefaultNotificationsAutoExpandIsFalse() {
        val settings = BehaviourSettings()
        assertFalse("Default notificationsAutoExpand should be false", settings.notificationsAutoExpand)
        assertEquals(false, BehaviourSettings.DEFAULT_NOTIFICATIONS_AUTO_EXPAND)
    }

    @Test
    fun testNotificationsAutoExpandCustomValue() {
        val settings = BehaviourSettings(notificationsAutoExpand = true)
        assertTrue(settings.notificationsAutoExpand)
    }

    @Test
    fun testNotificationAutoExpandResolutionLogic() {
        fun resolveAutoExpand(
            signal: CutoutSignal,
            notificationsAutoExpand: Boolean,
            normalOnly: Boolean,
            isNoExpandLandscape: Boolean,
        ): Boolean {
            val rawAutoExpand = when (signal) {
                is CutoutSignal.Notification -> notificationsAutoExpand
                is CutoutSignal.Music -> false
                is CutoutSignal.Assistant -> false
                is CutoutSignal.Call -> false
                is CutoutSignal.Timer -> false
                is CutoutSignal.System -> false
            }
            return if (isNoExpandLandscape || normalOnly) false else rawAutoExpand
        }

        val signal = CutoutSignal.Notification(
            packageName = "com.example.app",
            title = "Title",
            text = "Text",
        )

        // When auto-expand is disabled in settings:
        assertFalse(
            resolveAutoExpand(
                signal = signal,
                notificationsAutoExpand = false,
                normalOnly = false,
                isNoExpandLandscape = false,
            )
        )

        // When auto-expand is enabled in settings:
        assertTrue(
            resolveAutoExpand(
                signal = signal,
                notificationsAutoExpand = true,
                normalOnly = false,
                isNoExpandLandscape = false,
            )
        )

        // When app is in normalOnly list, auto-expand is suppressed:
        assertFalse(
            resolveAutoExpand(
                signal = signal,
                notificationsAutoExpand = true,
                normalOnly = true,
                isNoExpandLandscape = false,
            )
        )

        // When in no-expand landscape, auto-expand is suppressed:
        assertFalse(
            resolveAutoExpand(
                signal = signal,
                notificationsAutoExpand = true,
                normalOnly = false,
                isNoExpandLandscape = true,
            )
        )
    }
}
