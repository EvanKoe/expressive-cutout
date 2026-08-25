package com.ekoehler.expressivecutout.events

import android.os.BatteryManager
import com.ekoehler.expressivecutout.core.SystemEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying logic for battery charging completion and 80% charging optimization caps.
 */
class ChargingCompleteLogicTest {

    /**
     * Pure evaluator mirroring [SystemEventMonitor]'s charging complete decision logic.
     */
    private fun isChargingComplete(
        status: Int,
        level: Int,
        cap: Int,
        isPlugged: Boolean,
    ): Boolean {
        if (!isPlugged) return false
        val isStatusFull = status == BatteryManager.BATTERY_STATUS_FULL
        val isCapReached = (cap < 100 && level >= cap) || (cap == 100 && level >= 100)
        val isChargingStoppedAtCap = isCapReached && (
            status == BatteryManager.BATTERY_STATUS_NOT_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                status == BatteryManager.BATTERY_STATUS_CHARGING
        )
        return isStatusFull || isChargingStoppedAtCap
    }

    /** Resolves the display title for charging completion based on whether the battery is capped. */
    private fun resolveChargingCompleteTitle(cap: Int, level: Int): String {
        val isCapped = cap < 100 && level <= cap + 1
        return if (isCapped) "Charge limit reached" else "Fully charged"
    }

    @Test
    fun testStandardFullChargingDetection() {
        // When plugged in and status is full
        assertTrue(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_FULL,
                level = 100,
                cap = 100,
                isPlugged = true,
            ),
        )

        // When plugged in at 100% while still in charging status
        assertTrue(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_CHARGING,
                level = 100,
                cap = 100,
                isPlugged = true,
            ),
        )

        // When plugged in but only at 90%
        assertFalse(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_CHARGING,
                level = 90,
                cap = 100,
                isPlugged = true,
            ),
        )

        // When unplugged even at 100%
        assertFalse(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_FULL,
                level = 100,
                cap = 100,
                isPlugged = false,
            ),
        )
    }

    @Test
    fun testPixelChargingOptimization80Cap() {
        // When Pixel 80% cap is enabled and battery reaches 80% while plugged
        assertTrue(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                level = 80,
                cap = 80,
                isPlugged = true,
            ),
        )

        // When Pixel 80% cap is enabled and status reports full at 80%
        assertTrue(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_FULL,
                level = 80,
                cap = 80,
                isPlugged = true,
            ),
        )

        // When Pixel 80% cap is enabled but battery is only at 75%
        assertFalse(
            isChargingComplete(
                status = BatteryManager.BATTERY_STATUS_CHARGING,
                level = 75,
                cap = 80,
                isPlugged = true,
            ),
        )

        assertEquals("Charge limit reached", resolveChargingCompleteTitle(cap = 80, level = 80))
        assertEquals("Fully charged", resolveChargingCompleteTitle(cap = 100, level = 100))
    }

    @Test
    fun testChargingCompleteEventTypeProperties() {
        val type = SystemEventType.CHARGING_COMPLETE
        assertEquals(0xFF4ADE80, type.accent)
        assertEquals(com.ekoehler.expressivecutout.R.string.event_charging_complete, type.labelRes)
    }
}
