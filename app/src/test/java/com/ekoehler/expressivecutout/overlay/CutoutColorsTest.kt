package com.ekoehler.expressivecutout.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.AppColorFallback
import com.ekoehler.expressivecutout.data.ColorSpec
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import org.junit.Assert.assertEquals
import org.junit.Test

class CutoutColorsTest {

    @Test
    fun testSystemEventPrimaryColorDefault() {
        val wifiEvent = IslandEvent(
            id = 1L,
            icon = IslandIcon.Vector(Icons.Rounded.Info),
            label = "Wi-Fi Disconnected",
            accent = Color(SystemEventType.WIFI_DISCONNECTED.accent),
        )

        val primary = wifiEvent.resolvePrimaryColor()
        assertEquals(Color(SystemEventType.WIFI_DISCONNECTED.accent), primary)

        // When stroke is configured as AppIcon, it resolves to the event's primary accent
        val strokeColor = CutoutColor.AppIcon(AppColorFallback.ADAPTIVE)
        val resolvedStroke = strokeColor.resolveColor(appColor = primary)
        assertEquals(Color(SystemEventType.WIFI_DISCONNECTED.accent), resolvedStroke)

        // When background is configured as AppIcon, it also resolves to the event's primary accent
        val bgSpec = ColorSpec.AppIcon(AppColorFallback.ADAPTIVE)
        val resolvedBg = bgSpec.resolveColor(appColor = primary)
        assertEquals(Color(SystemEventType.WIFI_DISCONNECTED.accent), resolvedBg)
    }

    @Test
    fun testSystemEventPrimaryColorWithColorOverride() {
        val overrideColor = CutoutColor.Solid(0xFFFF0000)
        val chargingEvent = IslandEvent(
            id = 2L,
            icon = IslandIcon.Vector(Icons.Rounded.Info),
            label = "Charging",
            accent = Color(SystemEventType.CHARGING_STARTED.accent),
            colorOverride = overrideColor,
        )

        val primary = chargingEvent.resolvePrimaryColor()
        assertEquals(Color(0xFFFF0000), primary)

        val strokeColor = CutoutColor.AppIcon(AppColorFallback.ADAPTIVE)
        val resolvedStroke = strokeColor.resolveColor(appColor = primary)
        assertEquals(Color(0xFFFF0000), resolvedStroke)
    }

    @Test
    fun testSystemEventPrimaryColorWithDynamicTheme() {
        val mockDynamicPrimary = Color(0xFF123456)
        val mockDynamicSecondary = Color(0xFF654321)

        val dynamicResolver: (DynamicRole) -> Color = { role ->
            when (role) {
                DynamicRole.PRIMARY -> mockDynamicPrimary
                DynamicRole.SECONDARY -> mockDynamicSecondary
                DynamicRole.TERTIARY -> Color(0xFF999999)
            }
        }

        val dynamicEvent = IslandEvent(
            id = 3L,
            icon = IslandIcon.Vector(Icons.Rounded.Info),
            label = "Wi-Fi Connected",
            accent = Color(SystemEventType.WIFI_CONNECTED.accent),
            useThemeColor = true,
            themeColorRole = DynamicRole.PRIMARY,
        )

        val primary = dynamicEvent.resolvePrimaryColor(dynamicResolver)
        assertEquals(mockDynamicPrimary, primary)

        val strokeColor = CutoutColor.AppIcon(AppColorFallback.ADAPTIVE)
        val resolvedStroke = strokeColor.resolveColor(appColor = primary, dynamicResolver = dynamicResolver)
        assertEquals(mockDynamicPrimary, resolvedStroke)
    }

    @Test
    fun testNotificationWithAppColor() {
        val appColor = Color(0xFF00FF00)
        val notificationEvent = IslandEvent(
            id = 4L,
            icon = IslandIcon.Vector(Icons.Rounded.Info),
            label = "WhatsApp",
            accent = Color(0xFF38BDF8),
            appColor = appColor,
        )

        val primary = notificationEvent.resolvePrimaryColor()
        assertEquals(appColor, primary)

        val strokeColor = CutoutColor.AppIcon(AppColorFallback.ADAPTIVE)
        val resolvedStroke = strokeColor.resolveColor(appColor = primary)
        assertEquals(appColor, resolvedStroke)
    }

    @Test
    fun testEmptyIslandFallbackStrategies() {
        val mockDynamic = Color(0xFF112233)
        val dynamicResolver: (DynamicRole) -> Color = { mockDynamic }

        // When no event is active, appColor is null
        val appColor: Color? = null

        val adaptiveColor = CutoutColor.AppIcon(AppColorFallback.ADAPTIVE)
        assertEquals(mockDynamic, adaptiveColor.resolveColor(appColor = appColor, dynamicResolver = dynamicResolver))

        val dynamicFallback = CutoutColor.AppIcon(AppColorFallback.DYNAMIC_THEME)
        assertEquals(mockDynamic, dynamicFallback.resolveColor(appColor = appColor, dynamicResolver = dynamicResolver))

        val oledFallback = CutoutColor.AppIcon(AppColorFallback.OLED_BLACK)
        assertEquals(Color(0xFF000000L), oledFallback.resolveColor(appColor = appColor, dynamicResolver = dynamicResolver))
    }
}
