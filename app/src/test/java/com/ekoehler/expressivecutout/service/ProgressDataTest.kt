package com.ekoehler.expressivecutout.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ProgressData] state computation and completion detection.
 */
class ProgressDataTest {

    @Test
    fun testProgressCompletionDetection() {
        val complete = ProgressData(max = 100, current = 100, isIndeterminate = false)
        assertTrue("Progress at 100/100 should be complete", complete.isComplete)

        val overflow = ProgressData(max = 100, current = 105, isIndeterminate = false)
        assertTrue("Progress exceeding max should be complete", overflow.isComplete)

        val inProgress = ProgressData(max = 100, current = 50, isIndeterminate = false)
        assertFalse("Progress at 50/100 should not be complete", inProgress.isComplete)

        val zeroProgress = ProgressData(max = 100, current = 0, isIndeterminate = false)
        assertFalse("Progress at 0/100 should not be complete", zeroProgress.isComplete)
    }

    @Test
    fun testIndeterminateProgressNeverComplete() {
        val indeterminate = ProgressData(max = 100, current = 100, isIndeterminate = true)
        assertFalse("Indeterminate progress should never report complete", indeterminate.isComplete)
    }

    @Test
    fun testZeroOrNegativeMaxNeverComplete() {
        val zeroMax = ProgressData(max = 0, current = 0, isIndeterminate = false)
        assertFalse("Progress with 0 max should not report complete", zeroMax.isComplete)

        val negativeMax = ProgressData(max = -1, current = 0, isIndeterminate = false)
        assertFalse("Progress with negative max should not report complete", negativeMax.isComplete)
    }
}
