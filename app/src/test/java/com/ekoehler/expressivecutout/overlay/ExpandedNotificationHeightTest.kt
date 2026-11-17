package com.ekoehler.expressivecutout.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feedback loop for the multiline expanded island: action chips stay fully visible and the island
 * hugs wrap-content height (camera band + inner column + bottom padding).
 */
class ExpandedNotificationHeightTest {

    @Test
    fun `multiline notification with actions fits chips without extra empty height`() {
        val collapsed = 34
        val expanded = 108
        val button = 44
        val bottom = EXPANDED_NOTIFICATION_BOTTOM_PADDING_DP
        val spacing = expandedActionsExtraDp(button) - button
        val textRow = 80
        val inner = textRow + spacing + button

        val island = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = expanded,
            topMarginDp = collapsed,
            measuredInnerHeightDp = inner,
            buttonHeightDp = button,
            hasActions = true,
            bottomPaddingDp = bottom,
        )
        val wrapContent = collapsed + inner + bottom
        val chipsBottom = collapsed + inner

        assertTrue(
            "Action chips clipped: island=${island}dp, chips end at ${chipsBottom}dp",
            island >= chipsBottom,
        )
        assertEquals(wrapContent, island)
    }

    @Test
    fun `inner column measurement is padded by the camera band and bottom inset`() {
        val collapsed = 34
        val expanded = 108
        val button = 44
        val spacing = expandedActionsExtraDp(button) - button
        val inner = 80 + spacing + button

        val island = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = expanded,
            topMarginDp = collapsed,
            measuredInnerHeightDp = inner,
            buttonHeightDp = button,
            hasActions = true,
        )

        assertEquals(collapsed + inner + EXPANDED_NOTIFICATION_BOTTOM_PADDING_DP, island)
    }

    @Test
    fun `expanded notification preserves measured multiline height with action buttons`() {
        val baseExpandedHeightDp = 110
        val topMarginDp = 48
        val buttonHeightDp = 44
        val textRowHeightDp = 80
        val itemSpacingDp = 12
        val inner = textRowHeightDp + itemSpacingDp + buttonHeightDp

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = topMarginDp,
            measuredInnerHeightDp = inner,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
        )

        assertEquals(topMarginDp + inner + EXPANDED_NOTIFICATION_BOTTOM_PADDING_DP, calculatedHeight)
    }

    @Test
    fun `fallback to base height plus action button allowance when unmeasured`() {
        val baseExpandedHeightDp = 110
        val buttonHeightDp = 44
        val expectedBaseHeight = baseExpandedHeightDp + expandedActionsExtraDp(buttonHeightDp)

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 36,
            measuredInnerHeightDp = 0,
            buttonHeightDp = buttonHeightDp,
            hasActions = true,
        )

        assertEquals(expectedBaseHeight, calculatedHeight)
    }

    @Test
    fun `content smaller than base height clamps to base height`() {
        val baseExpandedHeightDp = 110

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 36,
            measuredInnerHeightDp = 20,
            buttonHeightDp = 44,
            hasActions = false,
        )

        assertEquals(baseExpandedHeightDp, calculatedHeight)
    }

    @Test
    fun `clamping to maximum height limit`() {
        val baseExpandedHeightDp = 110
        val maxLimitDp = 320

        val calculatedHeight = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = baseExpandedHeightDp,
            topMarginDp = 36,
            measuredInnerHeightDp = 600,
            buttonHeightDp = 44,
            hasActions = true,
            maxHeightLimitDp = maxLimitDp,
        )

        assertEquals(maxLimitDp, calculatedHeight)
    }

    @Test
    fun `estimate notification content height precomputes multiline height with padding`() {
        val topMarginDp = 36
        val buttonHeightDp = 44

        val estimatedHeight = estimateNotificationContentHeightDp(
            topMarginDp = topMarginDp,
            titleLines = 1,
            detailLines = 4,
            secondaryLinesCount = 0,
            hasHeader = false,
            hasActions = true,
            buttonHeightDp = buttonHeightDp,
            hasProgress = false,
        )

        assertTrue(
            "Estimated height ($estimatedHeight) should accommodate multiline content and action row",
            estimatedHeight >= topMarginDp + 86 + 12 + buttonHeightDp + EXPANDED_NOTIFICATION_BOTTOM_PADDING_DP,
        )
    }

    @Test
    fun `unbounded inner measure settles to wrap-content even if the first frame is short`() {
        val collapsed = 34
        val expanded = 108
        val button = 44
        val spacing = expandedActionsExtraDp(button) - button
        val inner = 80 + spacing + button
        val wrapContent = collapsed + inner + EXPANDED_NOTIFICATION_BOTTOM_PADDING_DP

        val afterMeasure = calculateExpandedNotificationHeightDp(
            baseExpandedHeightDp = expanded,
            topMarginDp = collapsed,
            measuredInnerHeightDp = inner,
            buttonHeightDp = button,
            hasActions = true,
        )

        assertEquals(wrapContent, afterMeasure)
    }
}
