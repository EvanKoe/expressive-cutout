package com.ekoehler.expressivecutout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying [SystemEventPayload] domain logic and [CutoutSignal.System] backward compatibility.
 */
class SystemEventPayloadTest {

    @Test
    fun testLegacySystemSignalConstructor() {
        val signal = CutoutSignal.System(SystemEventType.CHARGING_STARTED, 85)
        assertEquals(SystemEventType.CHARGING_STARTED, signal.type)
        assertEquals(85, signal.batteryLevel)
        assertEquals("85%", signal.payload.collapsedBadgeText)
        assertEquals(SystemEventType.CHARGING_STARTED, signal.payload.type)
    }

    @Test
    fun testLegacySystemSignalWithoutBattery() {
        val signal = CutoutSignal.System(SystemEventType.WIFI_CONNECTED)
        assertEquals(SystemEventType.WIFI_CONNECTED, signal.type)
        assertNull(signal.batteryLevel)
        assertNull(signal.payload.collapsedBadgeText)
    }

    @Test
    fun testRichSystemPayloadSignal() {
        val payload = SystemEventPayload(
            type = SystemEventType.HOTSPOT_ENABLED,
            title = "Hotspot active",
            subtitle = "Tethering active",
            collapsedBadgeText = "3 devs",
            secondaryLines = listOf("3 devices connected", "1.2 GB used"),
            actionIntentAction = "android.settings.WIRELESS_SETTINGS",
        )
        val signal = CutoutSignal.System(payload)

        assertEquals(SystemEventType.HOTSPOT_ENABLED, signal.type)
        assertEquals(payload, signal.payload)
        assertEquals("Hotspot active", signal.payload.title)
        assertEquals("Tethering active", signal.payload.subtitle)
        assertEquals("3 devs", signal.payload.collapsedBadgeText)
        assertEquals(2, signal.payload.secondaryLines.size)
        assertEquals("android.settings.WIRELESS_SETTINGS", signal.payload.actionIntentAction)
    }
}
