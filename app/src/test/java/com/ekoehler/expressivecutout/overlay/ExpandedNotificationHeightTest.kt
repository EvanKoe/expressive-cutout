package com.ekoehler.expressivecutout.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedNotificationHeightTest {

    @Test
    fun testExpandedActionsExtraDpPreservesHeightForMultilineNotification() {
        val baseExpandedHeightDp = 108
        val topMarginDp = 48
        val buttonHeightDp = 36
        val textRowHeightDp = 68
        val itemSpacingDp = 12
        val bottomPaddingDp = 18

        // Full column height measured by onGloballyPositioned (including top and bottom padding):
        val measuredTotalColumnHeightDp = topMarginDp + textRowHeightDp + itemSpacingDp + buttonHeightDp + bottomPaddingDp // 182 dp

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = topMarginDp,
            measuredContentHeightDp = measuredTotalColumnHeightDp,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
        )

        // The calculated height must match the measured column height without double-adding padding
        assertTrue(
            "Expected height >= $measuredTotalColumnHeightDp to preserve action button height, but got $calculatedHeight",
            calculatedHeight >= measuredTotalColumnHeightDp
        )
        assertEquals(measuredTotalColumnHeightDp, calculatedHeight)
    }

    @Test
    fun testProgressNotificationPreservesMeasuredHeight() {
        val baseExpandedHeightDp = 108
        val topMarginDp = 48
        val bottomPaddingDp = 18
        val innerRowWithProgressHeightDp = 58

        val totalMeasuredHeightDp = topMarginDp + innerRowWithProgressHeightDp + bottomPaddingDp // 124 dp

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = topMarginDp,
            measuredContentHeightDp = totalMeasuredHeightDp,
            buttonHeightDp = 36,
            hasActions = false,
        )

        assertEquals(totalMeasuredHeightDp, calculatedHeight)
    }

    @Test
    fun testTopMarginIncreasesBaseHeight() {
        val baseExpandedHeightDp = 108
        val topMarginDp = 78 // 30 dp above default 48
        val buttonHeightDp = 36

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = topMarginDp,
            measuredContentHeightDp = 0,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
        )

        val expectedBaseHeight = baseExpandedHeightDp + expandedActionsExtraDp(buttonHeightDp) + 30 // 108 + 48 + 30 = 186
        assertEquals(expectedBaseHeight, calculatedHeight)
    }

    @Test
    fun testFallbackToBaseHeightBeforeMeasurement() {
        val baseExpandedHeightDp = 108
        val buttonHeightDp = 36
        val expectedBaseHeight = baseExpandedHeightDp + expandedActionsExtraDp(buttonHeightDp) // 108 + 48 = 156

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 48,
            measuredContentHeightDp = 0,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
        )

        assertEquals(expectedBaseHeight, calculatedHeight)
    }

    @Test
    fun testRespectsBaseHeightWhenContentIsSmaller() {
        val baseExpandedHeightDp = 108

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 48,
            measuredContentHeightDp = 20,
            buttonHeightDp = 36,
            hasActions = false,
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
            measuredContentHeightDp = 500,
            buttonHeightDp = 36,
            hasActions = true,
            maxHeightLimitDp = maxLimitDp,
        )

        assertEquals(maxLimitDp, calculatedHeight)
    }
}
