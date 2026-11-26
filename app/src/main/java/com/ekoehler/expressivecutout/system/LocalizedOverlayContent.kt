package com.ekoehler.expressivecutout.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.data.LanguagePreferences

/** Accessibility windows may retain the system locale even when the Activity uses a per-app locale. */
@Composable
internal fun LocalizedOverlayContent(content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    val preferences = remember(base) { LanguagePreferences(base) }
    val tag by preferences.language.collectAsStateWithLifecycle()
    val localized = remember(base, configuration, tag) { AppLocale.localizedContext(base, tag) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content,
    )
}
