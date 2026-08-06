package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ekoehler.expressivecutout.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/** Persists the selected [AppTheme], defaulting to [AppTheme.SYSTEM]. */
class ThemePreferences(private val context: Context) : JsonSerializable {
    val theme: Flow<AppTheme> = context.appDataStore.data.map { prefs ->
        prefs[THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM
    }

    /** Sets the theme using AppTheme class */
    suspend fun setTheme(theme: AppTheme) = context.appDataStore.edit { prefs ->
        prefs[THEME] = theme.name
    }

    /** Sets a theme by name */
    suspend fun setThemeByName(name: String) {
        val theme = AppTheme.entries.first { it.name == name }
        this.setTheme(theme)
    }

    private companion object {
        val THEME = stringPreferencesKey("app_theme")
    }

    /**
     * Exports settings to JSON { theme: string }
     */
    override suspend fun toJson(): String {
        val t = theme.first()
        return JSONObject().apply {
            put("theme", t.name)
        }.toString()
    }

    /** Applies { theme: string } exported by [toJson]; an unknown name is ignored. */
    override suspend fun fromJson(json: String) {
        val name = JSONObject(json).optString("theme").takeIf { it.isNotEmpty() } ?: return
        val theme = runCatching { AppTheme.valueOf(name) }.getOrNull() ?: return
        setTheme(theme)
    }
}
