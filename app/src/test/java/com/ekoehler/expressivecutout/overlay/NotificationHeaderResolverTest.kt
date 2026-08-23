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
}
