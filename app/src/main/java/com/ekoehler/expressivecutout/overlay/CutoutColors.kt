package com.ekoehler.expressivecutout.overlay

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ekoehler.expressivecutout.data.CutoutColor

/** Fallback accent used for [CutoutColor.Dynamic] before Android 12 (no Material You). */
private val DynamicFallback = Color(0xFF60A5FA)

/**
 * Resolve a [CutoutColor] to a concrete [Color]. [Dynamic] reads the system Material You accent
 * (dark/light to match the phone) on Android 12+, and falls back to a fixed accent below that.
 * Works both inside and outside a MaterialTheme, so the overlay and the in-app preview agree.
 */
@Composable
fun CutoutColor.resolve(): Color = when (this) {
    is CutoutColor.Solid -> Color(argb)
    CutoutColor.Dynamic -> dynamicAccent()
}

@Composable
private fun dynamicAccent(): Color {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else {
        DynamicFallback
    }
}
