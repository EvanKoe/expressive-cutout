package com.ekoehler.expressivecutout.overlay

import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.core.SystemEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that same-family system state changes replace visible slots instead of duplicating them. */
class SystemEventSlotTransitionTest {

    @Test
    fun `wifi state change replaces primary without creating satellite`() {
        val connected = event(1L, SystemEventType.WIFI_CONNECTED)
        val disconnected = event(2L, SystemEventType.WIFI_DISCONNECTED)

        val result = replaceSystemEventInSlots(
            slots = SystemEventSlots(
                primary = connected,
                primaryType = SystemEventType.WIFI_CONNECTED,
            ),
            incomingType = SystemEventType.WIFI_DISCONNECTED,
            incoming = disconnected,
        )

        assertTrue(result.handled)
        assertEquals(disconnected, result.slots.primary)
        assertNull(result.slots.satellite)
    }

    @Test
    fun `wifi state change replaces satellite while preserving unrelated primary`() {
        val notification = event(1L, null)
        val connected = event(2L, SystemEventType.WIFI_CONNECTED)
        val disconnected = event(3L, SystemEventType.WIFI_DISCONNECTED)

        val result = replaceSystemEventInSlots(
            slots = SystemEventSlots(
                primary = notification,
                satellite = connected,
                satelliteType = SystemEventType.WIFI_CONNECTED,
            ),
            incomingType = SystemEventType.WIFI_DISCONNECTED,
            incoming = disconnected,
        )

        assertTrue(result.handled)
        assertEquals(notification, result.slots.primary)
        assertEquals(disconnected, result.slots.satellite)
    }

    @Test
    fun `wifi state change preserves unrelated satellite`() {
        val connected = event(1L, SystemEventType.WIFI_CONNECTED)
        val usb = event(2L, SystemEventType.USB_MOUNTED)
        val disconnected = event(3L, SystemEventType.WIFI_DISCONNECTED)

        val result = replaceSystemEventInSlots(
            slots = SystemEventSlots(
                primary = connected,
                primaryType = SystemEventType.WIFI_CONNECTED,
                satellite = usb,
                satelliteType = SystemEventType.USB_MOUNTED,
            ),
            incomingType = SystemEventType.WIFI_DISCONNECTED,
            incoming = disconnected,
        )

        assertTrue(result.handled)
        assertEquals(disconnected, result.slots.primary)
        assertEquals(usb, result.slots.satellite)
    }

    @Test
    fun `unrelated system state does not claim replacement slot`() {
        val connected = event(1L, SystemEventType.WIFI_CONNECTED)
        val usb = event(2L, SystemEventType.USB_MOUNTED)

        val result = replaceSystemEventInSlots(
            slots = SystemEventSlots(
                primary = connected,
                primaryType = SystemEventType.WIFI_CONNECTED,
            ),
            incomingType = SystemEventType.USB_MOUNTED,
            incoming = usb,
        )

        assertFalse(result.handled)
        assertEquals(connected, result.slots.primary)
        assertNull(result.slots.satellite)
    }

    private fun event(id: Long, type: SystemEventType?): IslandEvent =
        IslandEvent(
            id = id,
            icon = type?.let { IslandIcon.Vector(it.defaultIcon) }
                ?: IslandIcon.Vector(SystemEventType.WIFI_CONNECTED.defaultIcon),
            label = type?.name ?: "Notification",
            accent = Color.White,
        )
}
