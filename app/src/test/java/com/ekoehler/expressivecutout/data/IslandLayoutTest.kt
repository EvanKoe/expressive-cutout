package com.ekoehler.expressivecutout.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandLayoutTest {

    @Test
    fun testDefaultTopMarginDp() {
        assertEquals(48, IslandDimensions.DEFAULT_TOP_MARGIN_DP)
        assertEquals(48, IslandLayout.DEFAULT_EXPANDED.topMarginDp)
    }

    @Test
    fun testTopMarginClamping() {
        val clampedMin = IslandDimensions.of(
            widthPercent = 90,
            heightDp = 108,
            offsetXDp = 0,
            offsetYDp = 6,
            cornerTopLeftDp = 30,
            cornerTopRightDp = 30,
            cornerBottomLeftDp = 30,
            cornerBottomRightDp = 30,
            topMarginDp = -10,
        )
        assertEquals(IslandDimensions.MIN_TOP_MARGIN_DP, clampedMin.topMarginDp)

        val clampedMax = IslandDimensions.of(
            widthPercent = 90,
            heightDp = 108,
            offsetXDp = 0,
            offsetYDp = 6,
            cornerTopLeftDp = 30,
            cornerTopRightDp = 30,
            cornerBottomLeftDp = 30,
            cornerBottomRightDp = 30,
            topMarginDp = 200,
        )
        assertEquals(IslandDimensions.MAX_TOP_MARGIN_DP, clampedMax.topMarginDp)
    }

    @Test
    fun testCustomTopMargin() {
        val custom = IslandDimensions.of(
            widthPercent = 90,
            heightDp = 108,
            offsetXDp = 0,
            offsetYDp = 6,
            cornerTopLeftDp = 30,
            cornerTopRightDp = 30,
            cornerBottomLeftDp = 30,
            cornerBottomRightDp = 30,
            topMarginDp = 24,
        )
        assertEquals(24, custom.topMarginDp)
    }

    @Test
    fun testAsCallCutoutCarriesTopMargin() {
        val dims = IslandDimensions.of(
            widthPercent = 90,
            heightDp = 108,
            offsetXDp = 0,
            offsetYDp = 6,
            cornerTopLeftDp = 30,
            cornerTopRightDp = 30,
            cornerBottomLeftDp = 30,
            cornerBottomRightDp = 30,
            topMarginDp = 28,
        )
        val callCutout = dims.asCallCutout()
        assertEquals(28, callCutout.topMarginDp)
    }
}
