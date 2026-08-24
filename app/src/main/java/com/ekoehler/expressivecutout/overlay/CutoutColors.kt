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
import com.ekoehler.expressivecutout.data.AppColorFallback
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
 * [AppIcon] resolves to [appColor] or the configured fallback strategy if no app color is present.
 */
@Composable
fun CutoutColor.resolve(appColor: Color? = null, adaptiveColor: Color? = null): Color = when (this) {
    is CutoutColor.Solid -> Color(argb)
    is CutoutColor.Dynamic -> dynamicRole(role)
    is CutoutColor.AppIcon -> {
        appColor ?: when (fallback) {
            AppColorFallback.DYNAMIC_THEME -> dynamicRole(DynamicRole.PRIMARY)
            AppColorFallback.OLED_BLACK -> Color(0xFF000000L)
            AppColorFallback.ADAPTIVE -> adaptiveColor ?: dynamicRole(DynamicRole.PRIMARY)
        }
    }
}

/** Resolve a single [ColorSpec] to a concrete [Color], applying its opacity. */
@Composable
fun ColorSpec.resolve(appColor: Color? = null, adaptiveColor: Color? = null): Color = when (this) {
    is ColorSpec.Fixed -> Color(argb)
    is ColorSpec.Dynamic -> dynamicRole(role).copy(alpha = alpha)
    is ColorSpec.AppIcon -> {
        val base = appColor ?: when (fallback) {
            AppColorFallback.DYNAMIC_THEME -> dynamicRole(DynamicRole.PRIMARY)
            AppColorFallback.OLED_BLACK -> Color(0xFF000000L)
            AppColorFallback.ADAPTIVE -> adaptiveColor ?: dynamicRole(DynamicRole.PRIMARY)
        }
        base.copy(alpha = alpha)
    }
}

/** Non-composable variant of [CutoutColor.resolve] allowing a custom dynamic color resolver for testing. */
fun CutoutColor.resolveColor(
    appColor: Color? = null,
    dynamicResolver: (DynamicRole) -> Color = { DYNAMIC_FALLBACK },
    adaptiveColor: Color? = null,
): Color = when (this) {
    is CutoutColor.Solid -> Color(argb)
    is CutoutColor.Dynamic -> dynamicResolver(role)
    is CutoutColor.AppIcon -> {
        appColor ?: when (fallback) {
            AppColorFallback.DYNAMIC_THEME -> dynamicResolver(DynamicRole.PRIMARY)
            AppColorFallback.OLED_BLACK -> Color(0xFF000000L)
            AppColorFallback.ADAPTIVE -> adaptiveColor ?: dynamicResolver(DynamicRole.PRIMARY)
        }
    }
}

/** Non-composable variant of [ColorSpec.resolve] allowing a custom dynamic color resolver for testing. */
fun ColorSpec.resolveColor(
    appColor: Color? = null,
    dynamicResolver: (DynamicRole) -> Color = { DYNAMIC_FALLBACK },
    adaptiveColor: Color? = null,
): Color = when (this) {
    is ColorSpec.Fixed -> Color(argb)
    is ColorSpec.Dynamic -> dynamicResolver(role).copy(alpha = alpha)
    is ColorSpec.AppIcon -> {
        val base = appColor ?: when (fallback) {
            AppColorFallback.DYNAMIC_THEME -> dynamicResolver(DynamicRole.PRIMARY)
            AppColorFallback.OLED_BLACK -> Color(0xFF000000L)
            AppColorFallback.ADAPTIVE -> adaptiveColor ?: dynamicResolver(DynamicRole.PRIMARY)
        }
        base.copy(alpha = alpha)
    }
}

/**
 * Resolve a [CutoutFill] to the [Brush] painted behind the island. A [CutoutFill.Solid] is a flat
 * [SolidColor]; a [CutoutFill.Gradient] becomes a two-stop gradient in its chosen direction.
 */
@Composable
fun CutoutFill.resolveBrush(appColor: Color? = null, adaptiveColor: Color? = null): Brush = when (this) {
    is CutoutFill.Solid -> SolidColor(color.resolve(appColor, adaptiveColor))
    is CutoutFill.Gradient -> {
        val startColor = start.resolve(appColor, adaptiveColor).let { it.copy(alpha = it.alpha * opacity) }
        val endColor = end.resolve(appColor, adaptiveColor).let { it.copy(alpha = it.alpha * opacity) }
        val colors = listOf(startColor, endColor)
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
fun CutoutFill.representativeColor(appColor: Color? = null, adaptiveColor: Color? = null): Color = when (this) {
    is CutoutFill.Solid -> color.resolve(appColor, adaptiveColor)
    is CutoutFill.Gradient -> lerp(
        start.resolve(appColor, adaptiveColor).let { it.copy(alpha = it.alpha * opacity) },
        end.resolve(appColor, adaptiveColor).let { it.copy(alpha = it.alpha * opacity) },
        0.5f,
    )
}

/**
 * Resolves the solid base background color for a [CutoutFill] to sit on top of.
 * Defaults to OLED Black (#000000), or the configured fallback if Solid AppIcon is chosen.
 */
@Composable
fun CutoutFill.resolveBaseColor(appColor: Color? = null, adaptiveColor: Color? = null): Color = when (this) {
    is CutoutFill.Solid -> when (val spec = color) {
        is ColorSpec.AppIcon -> when (spec.fallback) {
            AppColorFallback.DYNAMIC_THEME -> dynamicRole(DynamicRole.PRIMARY)
            AppColorFallback.OLED_BLACK -> Color(0xFF000000L)
            AppColorFallback.ADAPTIVE -> adaptiveColor ?: dynamicRole(DynamicRole.PRIMARY)
        }
        else -> Color(0xFF000000L)
    }
    is CutoutFill.Gradient -> Color(0xFF000000L)
}

/**
 * Resolves the primary accent/branding color for an [IslandEvent].
 * Order of precedence:
 * 1. User-configured per-event [IslandEvent.colorOverride]
 * 2. System Material You dynamic color when [IslandEvent.useThemeColor] is enabled
 * 3. Extracted [IslandEvent.appColor] for apps/notifications
 * 4. Built-in [IslandEvent.accent] default color
 */
fun IslandEvent.resolvePrimaryColor(dynamicResolver: (DynamicRole) -> Color = { DYNAMIC_FALLBACK }): Color = when {
    colorOverride != null -> colorOverride.resolveColor(appColor, dynamicResolver)
    useThemeColor -> dynamicResolver(themeColorRole)
    appColor != null -> appColor
    else -> accent
}

@Composable
fun IslandEvent.primaryColor(): Color = when {
    colorOverride != null -> colorOverride.resolve()
    useThemeColor -> dynamicRole(themeColorRole)
    appColor != null -> appColor
    else -> accent
}

/** The Material You [role] colour (dark/light to match the phone) on Android 12+, else a fallback. */
@Composable
internal fun dynamicRole(role: DynamicRole): Color {
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
