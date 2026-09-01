package com.ekoehler.expressivecutout.overlay

import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The expanded music tile sizes itself from its own content, so the user's expanded-height knob
 * neither strands its controls above dead space nor squeezes them out of the card.
 */
class ExpandedMediaHeightTest {

    @Test
    fun `media height matches the expanded card the tile was laid out against`() {
        assertEquals(
            IslandLayout.DEFAULT_EXPANDED.heightDp,
            mediaExpandedBaseHeightDp(IslandDimensions.DEFAULT_TOP_MARGIN_DP),
        )
    }

    @Test
    fun `media height follows the top margin so the camera band never eats the artwork row`() {
        val default = mediaExpandedBaseHeightDp(IslandDimensions.DEFAULT_TOP_MARGIN_DP)
        val pushedDown = mediaExpandedBaseHeightDp(IslandDimensions.DEFAULT_TOP_MARGIN_DP + 20)

        assertEquals(default + 20, pushedDown)
    }
}
