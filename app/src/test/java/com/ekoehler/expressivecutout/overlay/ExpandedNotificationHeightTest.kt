package com.ekoehler.expressivecutout.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedNotificationHeightTest {

    @Test
    fun testExpandedActionsExtraDpPreservesHeightForMultilineNotificationWhenShowFullTextEnabled() {
        val baseExpandedHeightDp = 108
        val topMarginDp = 48
        val bottomPaddingDp = 22
        val buttonHeightDp = 36
        val textRowHeightDp = 68 // Header (16) + Title (20) + 2-line detail (32)
        val itemSpacingDp = 12

        // Inner content height measured inside the column (excluding top margin and bottom padding):
        val innerContentHeightDp = textRowHeightDp + itemSpacingDp + buttonHeightDp // 116 dp
        val totalRequiredHeightDp = topMarginDp + innerContentHeightDp + bottomPaddingDp // 186 dp

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = topMarginDp,
            bottomPaddingDp = bottomPaddingDp,
            measuredContentHeightDp = innerContentHeightDp,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
            showFullNotificationText = true,
        )

        // The calculated height must equal the total required height so the button is not squished
        assertTrue(
            "Expected height >= $totalRequiredHeightDp to preserve action button height, but got $calculatedHeight",
            calculatedHeight >= totalRequiredHeightDp
        )
        assertEquals(totalRequiredHeightDp, calculatedHeight)
    }

    @Test
    fun testPreservesBaseHeightWhenShowFullNotificationTextIsDisabled() {
        val baseExpandedHeightDp = 108
        val buttonHeightDp = 36
        val innerContentHeightDp = 200

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 48,
            bottomPaddingDp = 22,
            measuredContentHeightDp = innerContentHeightDp,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
            showFullNotificationText = false,
        )

        val expectedBaseHeight = baseExpandedHeightDp + expandedActionsExtraDp(buttonHeightDp) // 108 + 50 = 158
        assertEquals(expectedBaseHeight, calculatedHeight)
    }

    @Test
    fun testFallbackToBaseHeightBeforeMeasurement() {
        val baseExpandedHeightDp = 108
        val buttonHeightDp = 36
        val expectedBaseHeight = baseExpandedHeightDp + expandedActionsExtraDp(buttonHeightDp) // 108 + 50 = 158

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 48,
            bottomPaddingDp = 22,
            measuredContentHeightDp = 0,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
            showFullNotificationText = true,
        )

        assertEquals(expectedBaseHeight, calculatedHeight)
    }

    @Test
    fun testRespectsBaseHeightWhenContentIsSmaller() {
        val baseExpandedHeightDp = 108

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 48,
            bottomPaddingDp = 22,
            measuredContentHeightDp = 20,
            buttonHeightDp = 36,
            hasActions = false,
            showFullNotificationText = true,
        )

        assertEquals(baseExpandedHeightDp, calculatedHeight)
    }

    @Test
    fun testClampsToMaxHeightLimit() {
        val baseExpandedHeightDp = 108
        val maxLimitDp = 300

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 48,
            bottomPaddingDp = 22,
            measuredContentHeightDp = 500,
            buttonHeightDp = 36,
            hasActions = true,
            showFullNotificationText = true,
            maxHeightLimitDp = maxLimitDp,
        )

        assertEquals(maxLimitDp, calculatedHeight)
    }
}
