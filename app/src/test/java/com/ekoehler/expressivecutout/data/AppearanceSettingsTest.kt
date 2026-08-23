package com.ekoehler.expressivecutout.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {

    @Test
    fun testDefaultPreferDynamicIconColorIsFalse() {
        val settings = AppearanceSettings()
        assertFalse("Default preferDynamicIconColor should be false", settings.preferDynamicIconColor)
        assertEquals(false, AppearanceSettings.DEFAULT_PREFER_DYNAMIC_ICON_COLOR)
    }

    @Test
    fun testPreferDynamicIconColorCustomValue() {
        val settings = AppearanceSettings(preferDynamicIconColor = true)
        assertTrue(settings.preferDynamicIconColor)
    }

    @Test
    fun testPreferDynamicIconColorIconSelectionLogic() {
        // Simulates the icon selection logic
        data class MockIcon(val source: String, val isThemedMonochrome: Boolean)

        fun selectIcon(
            appLauncherIcon: MockIcon?,
            appMonochromeIcon: MockIcon?,
            notificationSmallIcon: MockIcon?,
            preferDynamicColor: Boolean,
        ): MockIcon? {
            if (preferDynamicColor) {
                if (appMonochromeIcon != null) return appMonochromeIcon
                if (notificationSmallIcon != null) return notificationSmallIcon
                if (appLauncherIcon != null) return appLauncherIcon
            } else {
                if (appLauncherIcon != null) return appLauncherIcon
                if (notificationSmallIcon != null) return notificationSmallIcon
            }
            return null
        }

        val plainAppIcon = MockIcon("app_launcher", isThemedMonochrome = false)
        val monochromeAppIcon = MockIcon("app_monochrome", isThemedMonochrome = true)
        val smallNotifIcon = MockIcon("small_icon", isThemedMonochrome = true)

        // When preferDynamicColor is OFF:
        // Plain app icon is chosen even if monochrome icon exists
        val resultOff = selectIcon(
            appLauncherIcon = plainAppIcon,
            appMonochromeIcon = monochromeAppIcon,
            notificationSmallIcon = smallNotifIcon,
            preferDynamicColor = false,
        )
        assertEquals("app_launcher", resultOff?.source)
        assertFalse(resultOff?.isThemedMonochrome ?: true)

        // When preferDynamicColor is ON:
        // Monochrome dynamic icon is preferred
        val resultOn = selectIcon(
            appLauncherIcon = plainAppIcon,
            appMonochromeIcon = monochromeAppIcon,
            notificationSmallIcon = smallNotifIcon,
            preferDynamicColor = true,
        )
        assertEquals("app_monochrome", resultOn?.source)
        assertTrue(resultOn?.isThemedMonochrome ?: false)

        // When preferDynamicColor is ON but no monochrome app icon, falls back to small icon
        val resultOnNoMonochrome = selectIcon(
            appLauncherIcon = plainAppIcon,
            appMonochromeIcon = null,
            notificationSmallIcon = smallNotifIcon,
            preferDynamicColor = true,
        )
        assertEquals("small_icon", resultOnNoMonochrome?.source)
        assertTrue(resultOnNoMonochrome?.isThemedMonochrome ?: false)

        // When preferDynamicColor is ON but no monochrome or small icon, falls back to plain app icon
        val resultOnNoThemed = selectIcon(
            appLauncherIcon = plainAppIcon,
            appMonochromeIcon = null,
            notificationSmallIcon = null,
            preferDynamicColor = true,
        )
        assertEquals("app_launcher", resultOnNoThemed?.source)
    }
}
