package com.ekoehler.expressivecutout.overlay

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.ekoehler.expressivecutout.data.ColorSpec
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.CutoutFill
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.GradientDirection

/** Fallback accent used for dynamic colours before Android 12 (no Material You). */
private val DYNAMIC_FALLBACK = Color(0xFF60A5FA)

/**
 * Resolve a [CutoutColor] to a concrete [Color]. [Dynamic] reads the system Material You accent
 * (dark/light to match the phone) on Android 12+, and falls back to a fixed accent below that.
 * Works both inside and outside a MaterialTheme, so the overlay and the in-app preview agree.
 */
@Composable
fun CutoutColor.resolve(): Color = when (this) {
    is CutoutColor.Solid -> Color(argb)
    is CutoutColor.Dynamic -> dynamicRole(role)
}

/** Resolve a single [ColorSpec] to a concrete [Color], applying its opacity. */
@Composable
fun ColorSpec.resolve(): Color = when (this) {
    is ColorSpec.Fixed -> Color(argb)
    is ColorSpec.Dynamic -> dynamicRole(role).copy(alpha = alpha)
}

/**
 * Resolve a [CutoutFill] to the [Brush] painted behind the island. A [CutoutFill.Solid] is a flat
 * [SolidColor]; a [CutoutFill.Gradient] becomes a two-stop gradient in its chosen direction.
 */
@Composable
fun CutoutFill.resolveBrush(): Brush = when (this) {
    is CutoutFill.Solid -> SolidColor(color.resolve())
    is CutoutFill.Gradient -> {
        val colors = listOf(start.resolve(), end.resolve())
        when (direction) {
            GradientDirection.VERTICAL -> Brush.verticalGradient(colors)
            GradientDirection.HORIZONTAL -> Brush.horizontalGradient(colors)
            GradientDirection.DIAGONAL -> Brush.linearGradient(colors)
        }
    }
}

/**
 * A single representative [Color] for a [CutoutFill], used to decide whether text/icons over it
 * should be light or dark. A gradient reports the midpoint of its two stops.
 */
@Composable
fun CutoutFill.representativeColor(): Color = when (this) {
    is CutoutFill.Solid -> color.resolve()
    is CutoutFill.Gradient -> lerp(start.resolve(), end.resolve(), 0.5f)
}

/** The Material You [role] colour (dark/light to match the phone) on Android 12+, else a fallback. */
@Composable
private fun dynamicRole(role: DynamicRole): Color {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.forRole(role)
    } else {
        DYNAMIC_FALLBACK
    }
}

/**
 * The matching "on" colour for a [CutoutColor.Dynamic] [role], read from the *same* system Material
 * You scheme as [dynamicRole] (which [CutoutColor.resolve] fills the badge with). Using
 * `MaterialTheme.colorScheme.onForRole` instead would draw the app theme's on-colour, which needn't
 * match the system accent the badge is actually painted with — leaving e.g. white ink on a light
 * dynamic accent in dark mode when the app's own dynamic colour is off or its light/dark differs.
 */
@Composable
fun onDynamicRole(role: DynamicRole): Color {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.onForRole(role)
    } else {
        // The fallback fill is a mid blue; white ink reads legibly on it in both themes.
        Color.White
    }
}

/**
 * Picks one role out of the live Material You scheme, so a [DynamicRole] stored in preferences can
 * be resolved at draw time.
 */
internal fun ColorScheme.forRole(role: DynamicRole): Color = when (role) {
    DynamicRole.PRIMARY -> primary
    DynamicRole.SECONDARY -> secondary
    DynamicRole.TERTIARY -> tertiary
}

/** The matching "on" colour for [role], for legible ink on a [forRole] fill. */
internal fun ColorScheme.onForRole(role: DynamicRole): Color = when (role) {
    DynamicRole.PRIMARY -> onPrimary
    DynamicRole.SECONDARY -> onSecondary
    DynamicRole.TERTIARY -> onTertiary
}
