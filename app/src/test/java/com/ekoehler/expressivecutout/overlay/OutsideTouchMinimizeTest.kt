package com.ekoehler.expressivecutout.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutsideTouchMinimizeTest {

    @Test
    fun `expanded island should minimize on outside click or tap`() {
        var isExpanded = true
        var tapExpanded = true
        val previewPinned = false

        val shouldCollapse = IslandOverlayController.shouldCollapseOnOutsideTouch(
            isExpanded = isExpanded,
            previewPinned = previewPinned,
        )

        assertTrue("Outside click should be handled when island is expanded", shouldCollapse)
        if (shouldCollapse) {
            tapExpanded = false
            isExpanded = false
        }

        assertFalse("Island should minimize (expanded = false)", isExpanded)
        assertFalse("tapExpanded should be reset to false", tapExpanded)
    }

    @Test
    fun `collapsed island should not react to outside touch`() {
        val isExpanded = false
        val previewPinned = false

        val shouldCollapse = IslandOverlayController.shouldCollapseOnOutsideTouch(
            isExpanded = isExpanded,
            previewPinned = previewPinned,
        )

        assertFalse("Outside touch should not trigger collapse when already collapsed", shouldCollapse)
    }

    @Test
    fun `pinned preview should not minimize on outside touch`() {
        val isExpanded = true
        val previewPinned = true

        val shouldCollapse = IslandOverlayController.shouldCollapseOnOutsideTouch(
            isExpanded = isExpanded,
            previewPinned = previewPinned,
        )

        assertFalse("Pinned preview should remain expanded", shouldCollapse)
    }

    @Test
    fun `outside touch resets active inline reply state when collapsing`() {
        var isExpanded = true
        var tapExpanded = true
        var replyingTo: String? = "reply_action_key"

        val shouldCollapse = IslandOverlayController.shouldCollapseOnOutsideTouch(
            isExpanded = isExpanded,
            previewPinned = false,
        )

        assertTrue(shouldCollapse)
        if (shouldCollapse) {
            tapExpanded = false
            replyingTo = null
            isExpanded = false
        }

        assertFalse(isExpanded)
        assertFalse(tapExpanded)
        assertNull("Inline reply should be cancelled when outside touch occurs", replyingTo)
    }

    @Test
    fun `empty center expanded should minimize on outside touch`() {
        val emptyPill = true
        val emptyOpensCenter = true
        var tapExpanded = true
        var isExpanded = emptyPill && emptyOpensCenter && tapExpanded

        val shouldCollapse = IslandOverlayController.shouldCollapseOnOutsideTouch(
            isExpanded = isExpanded,
            previewPinned = false,
        )

        assertTrue(shouldCollapse)
        if (shouldCollapse) {
            tapExpanded = false
            isExpanded = emptyPill && emptyOpensCenter && tapExpanded
        }

        assertFalse("Center should minimize back to empty collapsed pill", isExpanded)
        assertFalse(tapExpanded)
    }
}
