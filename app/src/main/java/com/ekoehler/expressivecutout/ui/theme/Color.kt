package com.ekoehler.expressivecutout.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand seed palette used when dynamic colour is unavailable (Android 11 and below). */
val SeedPrimary = Color(0xFF3B82F6)
val SeedSecondary = Color(0xFF7C3AED)
val SeedTertiary = Color(0xFF14B8A6)

/**
 * The light scheme's page and card colours, used when Material You dynamic colour is unavailable or
 * turned off.
 */
val LightBackground = Color(0xFFFAFAFC)
val LightSurface = Color(0xFFFFFFFF)

/**
 * The dark scheme's page and card colours, deliberately not pure black so the island's own
 * near-black still reads as separate.
 */
val DarkBackground = Color(0xFF101216)
val DarkSurface = Color(0xFF191C22)
