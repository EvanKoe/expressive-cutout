package com.ekoehler.expressivecutout.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App theme. Resolves the user's [AppTheme] choice to light/dark, then prefers Material You
 * dynamic colour on Android 12+ and falls back to a fixed brand scheme elsewhere.
 */
@Composable
fun ExpressiveCutoutTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> darkColorScheme(
            primary = SeedPrimary,
            secondary = SeedSecondary,
            tertiary = SeedTertiary,
            background = DarkBackground,
            surface = DarkSurface,
        )

        else -> lightColorScheme(
            primary = SeedPrimary,
            secondary = SeedSecondary,
            tertiary = SeedTertiary,
            background = LightBackground,
            surface = LightSurface,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
