package com.ekoehler.expressivecutout.overlay

import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.core.SystemEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusDotColorTest {

    @Test
    fun testStatusDotColorsMatchSemanticRoles() {
        val successColor = Color(0xFF4ADE80)
        val warningColor = Color(0xFFFACC15)
        val dangerColor = Color(0xFFF87171)

        // Success (green)
        assertEquals(successColor, IconResolver.statusDotColorFor(SystemEventType.WIFI_CONNECTED))
        assertEquals(successColor, IconResolver.statusDotColorFor(SystemEventType.HEADPHONES_CONNECTED))
        assertEquals(successColor, IconResolver.statusDotColorFor(SystemEventType.USB_MOUNTED))
        assertEquals(successColor, IconResolver.statusDotColorFor(SystemEventType.CHARGING_STARTED))

        // Warning / Lock (yellow)
        assertEquals(warningColor, IconResolver.statusDotColorFor(SystemEventType.BATTERY_LOW))
        assertEquals(warningColor, IconResolver.statusDotColorFor(SystemEventType.DEVICE_LOCKED))
        assertEquals(warningColor, IconResolver.statusDotColorFor(SystemEventType.DEVICE_UNLOCKED))

        // Danger / Disconnected / Unplugged (red)
        assertEquals(dangerColor, IconResolver.statusDotColorFor(SystemEventType.WIFI_DISCONNECTED))
        assertEquals(dangerColor, IconResolver.statusDotColorFor(SystemEventType.HEADPHONES_DISCONNECTED))
        assertEquals(dangerColor, IconResolver.statusDotColorFor(SystemEventType.USB_UNMOUNTED))
        assertEquals(dangerColor, IconResolver.statusDotColorFor(SystemEventType.CHARGING_STOPPED))
    }
}
