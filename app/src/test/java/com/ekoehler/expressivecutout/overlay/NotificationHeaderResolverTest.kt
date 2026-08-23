package com.ekoehler.expressivecutout.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationHeaderResolverTest {

    @Test
    fun testFormatRelativeTimeNow() {
        val now = 1_000_000L
        assertEquals("Now", NotificationHeaderResolver.formatRelativeTime(now, now))
        assertEquals("Now", NotificationHeaderResolver.formatRelativeTime(now - 2_000L, now))
        assertEquals("Now", NotificationHeaderResolver.formatRelativeTime(now - 4_999L, now))
        assertEquals("Now", NotificationHeaderResolver.formatRelativeTime(now + 10_000L, now))
    }

    @Test
    fun testFormatRelativeTimeSeconds() {
        val now = 1_000_000L
        assertEquals("5s ago", NotificationHeaderResolver.formatRelativeTime(now - 5_000L, now))
        assertEquals("30s ago", NotificationHeaderResolver.formatRelativeTime(now - 30_000L, now))
        assertEquals("59s ago", NotificationHeaderResolver.formatRelativeTime(now - 59_000L, now))
    }

    @Test
    fun testFormatRelativeTimeMinutes() {
        val now = 1_000_000L
        assertEquals("1m ago", NotificationHeaderResolver.formatRelativeTime(now - 60_000L, now))
        assertEquals("2m ago", NotificationHeaderResolver.formatRelativeTime(now - 120_000L, now))
        assertEquals("59m ago", NotificationHeaderResolver.formatRelativeTime(now - 59 * 60_000L, now))
    }

    @Test
    fun testFormatRelativeTimeHours() {
        val now = 100_000_000L
        assertEquals("1h ago", NotificationHeaderResolver.formatRelativeTime(now - 3600_000L, now))
        assertEquals("2h ago", NotificationHeaderResolver.formatRelativeTime(now - 2 * 3600_000L, now))
        assertEquals("23h ago", NotificationHeaderResolver.formatRelativeTime(now - 23 * 3600_000L, now))
    }

    @Test
    fun testFormatRelativeTimeDays() {
        val now = 100_000_000L
        assertEquals("1d ago", NotificationHeaderResolver.formatRelativeTime(now - 24 * 3600_000L, now))
        assertEquals("5d ago", NotificationHeaderResolver.formatRelativeTime(now - 5 * 24 * 3600_000L, now))
    }

    @Test
    fun testFormatNotificationHeaderBoth() {
        val header = NotificationHeaderResolver.formatHeader(
            appName = "Expressive Cutout",
            relativeTime = "Now",
            showAppName = true,
            showTimestamp = true,
        )
        assertEquals("Expressive Cutout • Now", header)
    }

    @Test
    fun testFormatNotificationHeaderAppNameOnly() {
        val header = NotificationHeaderResolver.formatHeader(
            appName = "Expressive Cutout",
            relativeTime = "Now",
            showAppName = true,
            showTimestamp = false,
        )
        assertEquals("Expressive Cutout", header)
    }

    @Test
    fun testFormatNotificationHeaderTimestampOnly() {
        val header = NotificationHeaderResolver.formatHeader(
            appName = "Expressive Cutout",
            relativeTime = "Now",
            showAppName = false,
            showTimestamp = true,
        )
        assertEquals("Now", header)
    }

    @Test
    fun testFormatNotificationHeaderNone() {
        val header = NotificationHeaderResolver.formatHeader(
            appName = "Expressive Cutout",
            relativeTime = "Now",
            showAppName = false,
            showTimestamp = false,
        )
        assertNull(header)
    }

    @Test
    fun testFormatNotificationHeaderNullInputs() {
        assertNull(
            NotificationHeaderResolver.formatHeader(
                appName = null,
                relativeTime = null,
                showAppName = true,
                showTimestamp = true,
            )
        )
        assertEquals(
            "Expressive Cutout",
            NotificationHeaderResolver.formatHeader(
                appName = "Expressive Cutout",
                relativeTime = null,
                showAppName = true,
                showTimestamp = true,
            )
        )
        assertEquals(
            "Now",
            NotificationHeaderResolver.formatHeader(
                appName = null,
                relativeTime = "Now",
                showAppName = true,
                showTimestamp = true,
            )
        )
    }

    @Test
    fun testResolvePostTimeMs() {
        val customTime = 123456789L
        assertEquals(customTime, NotificationHeaderResolver.resolvePostTimeMs(customTime))

        val before = System.currentTimeMillis()
        val resolved = NotificationHeaderResolver.resolvePostTimeMs(0L)
        val after = System.currentTimeMillis()
        org.junit.Assert.assertTrue(resolved in before..after)
    }

    @Test
    fun testResolveHeaderConvenience() {
        val now = 1_000_000L
        val header = NotificationHeaderResolver.resolveHeader(
            appName = "TestApp",
            postTimeMs = now - 5_000L,
            showAppName = true,
            showTimestamp = true,
            nowMs = now,
        )
        assertEquals("TestApp • 5s ago", header)
    }

    @Test
    fun testResolveHeaderSystemEvent() {
        val now = 1_000_000L
        val header = NotificationHeaderResolver.resolveHeader(
            appName = "Expressive Cutout",
            postTimeMs = now,
            showAppName = true,
            showTimestamp = true,
            nowMs = now,
        )
        assertEquals("Expressive Cutout • Now", header)
    }

    @Test
    fun testResolveHeaderHiddenPreferences() {
        val now = 1_000_000L
        assertNull(
            NotificationHeaderResolver.resolveHeader(
                appName = "Expressive Cutout",
                postTimeMs = now,
                showAppName = false,
                showTimestamp = false,
                nowMs = now,
            )
        )
    }

    @Test
    fun testResolveHeaderPlacementCenterCutout() {
        // offsetXDp == 0 (Center cutout): App name on Left, Timestamp on Right
        val placement = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Slack",
            relativeTime = "2m ago",
            showAppName = true,
            showTimestamp = true,
            offsetXDp = 0,
        )
        assertEquals("Slack", placement.leftText)
        assertEquals("2m ago", placement.rightText)
    }

    @Test
    fun testResolveHeaderPlacementCenterCutoutPartial() {
        // App name only
        val appOnly = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Slack",
            relativeTime = "2m ago",
            showAppName = true,
            showTimestamp = false,
            offsetXDp = 0,
        )
        assertEquals("Slack", appOnly.leftText)
        assertNull(appOnly.rightText)

        // Timestamp only
        val timeOnly = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Slack",
            relativeTime = "2m ago",
            showAppName = false,
            showTimestamp = true,
            offsetXDp = 0,
        )
        assertNull(timeOnly.leftText)
        assertEquals("2m ago", timeOnly.rightText)
    }

    @Test
    fun testResolveHeaderPlacementLeftCutout() {
        // offsetXDp < 0 (Left cutout): App name and timestamp together on Right
        val placement = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "WhatsApp",
            relativeTime = "Now",
            showAppName = true,
            showTimestamp = true,
            offsetXDp = -24,
        )
        assertNull(placement.leftText)
        assertEquals("WhatsApp • Now", placement.rightText)
    }

    @Test
    fun testResolveHeaderPlacementRightCutout() {
        // offsetXDp > 0 (Right cutout): App name and timestamp together on Left
        val placement = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Gmail",
            relativeTime = "5s ago",
            showAppName = true,
            showTimestamp = true,
            offsetXDp = 30,
        )
        assertEquals("Gmail • 5s ago", placement.leftText)
        assertNull(placement.rightText)
    }

    @Test
    fun testResolveHeaderPlacementDisabled() {
        val placement = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Gmail",
            relativeTime = "5s ago",
            showAppName = false,
            showTimestamp = false,
            offsetXDp = 0,
        )
        assertNull(placement.leftText)
        assertNull(placement.rightText)
    }

    @Test
    fun testResolveHeaderPlacementWithPostTimeMs() {
        val now = 10_000_000L
        val center = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Slack",
            postTimeMs = now - 60_000L,
            showAppName = true,
            showTimestamp = true,
            offsetXDp = 0,
            nowMs = now,
        )
        assertEquals("Slack", center.leftText)
        assertEquals("1m ago", center.rightText)

        val leftCutout = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Slack",
            postTimeMs = now - 60_000L,
            showAppName = true,
            showTimestamp = true,
            offsetXDp = -10,
            nowMs = now,
        )
        assertNull(leftCutout.leftText)
        assertEquals("Slack • 1m ago", leftCutout.rightText)

        val rightCutout = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Slack",
            postTimeMs = now - 60_000L,
            showAppName = true,
            showTimestamp = true,
            offsetXDp = 10,
            nowMs = now,
        )
        assertEquals("Slack • 1m ago", rightCutout.leftText)
        assertNull(rightCutout.rightText)
    }

    @Test
    fun testResolveHeaderPlacementLeftCutoutPartials() {
        // App name only with left cutout
        val appOnly = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Messages",
            relativeTime = "30s ago",
            showAppName = true,
            showTimestamp = false,
            offsetXDp = -15,
        )
        assertNull(appOnly.leftText)
        assertEquals("Messages", appOnly.rightText)

        // Timestamp only with left cutout
        val timeOnly = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Messages",
            relativeTime = "30s ago",
            showAppName = false,
            showTimestamp = true,
            offsetXDp = -15,
        )
        assertNull(timeOnly.leftText)
        assertEquals("30s ago", timeOnly.rightText)
    }

    @Test
    fun testResolveHeaderPlacementRightCutoutPartials() {
        // App name only with right cutout
        val appOnly = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Messages",
            relativeTime = "30s ago",
            showAppName = true,
            showTimestamp = false,
            offsetXDp = 15,
        )
        assertEquals("Messages", appOnly.leftText)
        assertNull(appOnly.rightText)

        // Timestamp only with right cutout
        val timeOnly = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "Messages",
            relativeTime = "30s ago",
            showAppName = false,
            showTimestamp = true,
            offsetXDp = 15,
        )
        assertEquals("30s ago", timeOnly.leftText)
        assertNull(timeOnly.rightText)
    }

    @Test
    fun testResolveHeaderPlacementBlankAndNull() {
        val blankApp = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "  ",
            relativeTime = "Now",
            showAppName = true,
            showTimestamp = true,
            offsetXDp = 0,
        )
        assertNull(blankApp.leftText)
        assertEquals("Now", blankApp.rightText)

        val nullTime = NotificationHeaderResolver.resolveHeaderPlacement(
            appName = "App",
            relativeTime = null,
            showAppName = true,
            showTimestamp = true,
            offsetXDp = 0,
        )
        assertEquals("App", nullTime.leftText)
        assertNull(nullTime.rightText)
    }
}
