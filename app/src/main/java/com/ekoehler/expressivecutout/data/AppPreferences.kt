package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Deliberately not "app_prefs" — ThemePreferences already owns that file, and a second delegate
// over the same file throws "multiple DataStores active for the same file" on first read.
private val Context.perAppDataStore: DataStore<Preferences> by preferencesDataStore(name = "per_app_prefs")

/**
 * Per-app opt-outs: the packages the user muted on the Apps screen. Nothing they post reaches the
 * cutout — neither their notifications nor their media. Only the opt-outs are stored (absent means
 * enabled), so newly installed apps are allowed by default and the set stays small, mirroring
 * [DynamicTilePreferences].
 */
class AppPreferences(private val context: Context) {

    val disabledPackages: Flow<Set<String>> = context.perAppDataStore.data.map { prefs ->
        prefs[DISABLED_KEY].orEmpty()
    }

    suspend fun setEnabled(packageName: String, enabled: Boolean) = context.perAppDataStore.edit { prefs ->
        val current = prefs[DISABLED_KEY].orEmpty()
        prefs[DISABLED_KEY] = if (enabled) current - packageName else current + packageName
    }

    private companion object {
        val DISABLED_KEY = stringSetPreferencesKey("disabled_packages")
    }
}
