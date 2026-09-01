package com.ekoehler.expressivecutout.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelativeTimeTest {

    @Test
    fun testFormatRelativeTimeNow() {
        val now = 1_000_000L
        assertEquals("Now", formatRelativeTime(now, now))
        assertEquals("Now", formatRelativeTime(now - 2_000L, now))
        assertEquals("Now", formatRelativeTime(now - 4_999L, now))
        // Future timestamps due to clock skew
        assertEquals("Now", formatRelativeTime(now + 10_000L, now))
    }

    @Test
    fun testFormatRelativeTimeSeconds() {
        val now = 1_000_000L
        assertEquals("5s ago", formatRelativeTime(now - 5_000L, now))
        assertEquals("30s ago", formatRelativeTime(now - 30_000L, now))
        assertEquals("59s ago", formatRelativeTime(now - 59_000L, now))
    }

    @Test
    fun testFormatRelativeTimeMinutes() {
        val now = 1_000_000L
        assertEquals("1m ago", formatRelativeTime(now - 60_000L, now))
        assertEquals("2m ago", formatRelativeTime(now - 120_000L, now))
        assertEquals("59m ago", formatRelativeTime(now - 59 * 60_000L, now))
    }

    @Test
    fun testFormatRelativeTimeHours() {
        val now = 100_000_000L
        assertEquals("1h ago", formatRelativeTime(now - 3600_000L, now))
        assertEquals("2h ago", formatRelativeTime(now - 2 * 3600_000L, now))
        assertEquals("23h ago", formatRelativeTime(now - 23 * 3600_000L, now))
    }

    @Test
    fun testFormatRelativeTimeDays() {
        val now = 100_000_000L
        assertEquals("1d ago", formatRelativeTime(now - 24 * 3600_000L, now))
        assertEquals("5d ago", formatRelativeTime(now - 5 * 24 * 3600_000L, now))
    }

    @Test
    fun testFormatNotificationHeader() {
        assertEquals("App • Now", formatNotificationHeader("App", "Now", showAppName = true, showTimestamp = true))
        assertEquals("App", formatNotificationHeader("App", "Now", showAppName = true, showTimestamp = false))
        assertEquals("Now", formatNotificationHeader("App", "Now", showAppName = false, showTimestamp = true))
        assertNull(formatNotificationHeader("App", "Now", showAppName = false, showTimestamp = false))
        assertNull(formatNotificationHeader(null, null, showAppName = true, showTimestamp = true))
        assertEquals("App", formatNotificationHeader("App", null, showAppName = true, showTimestamp = true))
        assertEquals("Now", formatNotificationHeader(null, "Now", showAppName = true, showTimestamp = true))
    }
}
