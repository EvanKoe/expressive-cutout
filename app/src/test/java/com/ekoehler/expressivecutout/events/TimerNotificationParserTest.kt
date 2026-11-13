package com.ekoehler.expressivecutout.events

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [TimerNotificationParser.mayTakeTile], the guard that stops a paused timer from stealing
 * the tile from the running one when a clock re-posts all its timers.
 */
class TimerNotificationParserTest {

    @Test
    fun pausedTimerMayTakeEmptyTile() {
        assertTrue(TimerNotificationParser.mayTakeTile(isPaused = true, key = "a", currentKey = null))
    }

    @Test
    fun pausedTimerMayRefreshItsOwnTile() {
        assertTrue(TimerNotificationParser.mayTakeTile(isPaused = true, key = "a", currentKey = "a"))
    }

    @Test
    fun pausedTimerMayNotStealTileFromAnotherTimer() {
        assertFalse(TimerNotificationParser.mayTakeTile(isPaused = true, key = "b", currentKey = "a"))
    }

    @Test
    fun runningTimerMayTakeTileFromAnotherTimer() {
        assertTrue(TimerNotificationParser.mayTakeTile(isPaused = false, key = "b", currentKey = "a"))
    }
}
