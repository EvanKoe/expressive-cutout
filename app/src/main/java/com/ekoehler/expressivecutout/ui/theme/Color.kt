package com.ekoehler.expressivecutout.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand seed palette used when dynamic colour is unavailable (Android 11 and below). */
val SEED_PRIMARY = Color(0xFF3B82F6)
val SEED_SECONDARY = Color(0xFF7C3AED)
val SEED_TERTIARY = Color(0xFF14B8A6)

/**
 * The light scheme's page and card colours, used when Material You dynamic colour is unavailable or
 * turned off.
 */
val LIGHT_BACKGROUND = Color(0xFFFAFAFC)
val LIGHT_SURFACE = Color(0xFFFFFFFF)

/**
 * The dark scheme's page and card colours, deliberately not pure black so the island's own
 * near-black still reads as separate.
 */
val DARK_BACKGROUND = Color(0xFF101216)
val DARK_SURFACE = Color(0xFF191C22)
