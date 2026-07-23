package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/** Persists the selected [AppTheme], defaulting to [AppTheme.SYSTEM]. */
class ThemePreferences(private val context: Context) {

    val theme: Flow<AppTheme> = context.appDataStore.data.map { prefs ->
        prefs[THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM
    }

    suspend fun setTheme(theme: AppTheme) = context.appDataStore.edit { prefs ->
        prefs[THEME] = theme.name
    }

    private companion object {
        val THEME = stringPreferencesKey("app_theme")
    }
}
