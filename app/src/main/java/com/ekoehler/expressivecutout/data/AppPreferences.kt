package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

// Deliberately not "app_prefs" — ThemePreferences already owns that file, and a second delegate
// over the same file throws "multiple DataStores active for the same file" on first read.
private val Context.perAppDataStore: DataStore<Preferences> by preferencesDataStore(name = "per_app_prefs")

/**
 * Per-app overrides set on the Apps screen:
 *
 * - [disabledPackages] — apps muted outright. Nothing they post reaches the cutout, neither their
 *   notifications nor their media.
 * - [normalOnlyPackages] — apps kept on the normal cutout, which for them has no expanded state at
 *   all. Tapping the pill opens the app instead of toggling it open, the same treatment the phone
 *   tile gets by its nature.
 *
 * Both store only the opt-outs (absent means the default), so newly installed apps behave normally
 * and the sets stay small, mirroring [DynamicTilePreferences].
 */
class AppPreferences(private val context: Context) {

    val disabledPackages: Flow<Set<String>> = context.perAppDataStore.data.map { prefs ->
        prefs[DISABLED_KEY].orEmpty()
    }

    val normalOnlyPackages: Flow<Set<String>> = context.perAppDataStore.data.map { prefs ->
        prefs[NORMAL_ONLY_KEY].orEmpty()
    }

    suspend fun setEnabled(packageName: String, enabled: Boolean) = context.perAppDataStore.edit { prefs ->
        val current = prefs[DISABLED_KEY].orEmpty()
        prefs[DISABLED_KEY] = if (enabled) current - packageName else current + packageName
    }

    suspend fun setNormalOnly(packageName: String, normalOnly: Boolean) = context.perAppDataStore.edit { prefs ->
        val current = prefs[NORMAL_ONLY_KEY].orEmpty()
        prefs[NORMAL_ONLY_KEY] = if (normalOnly) current + packageName else current - packageName
    }

    private companion object {
        val DISABLED_KEY = stringSetPreferencesKey("disabled_packages")
        val NORMAL_ONLY_KEY = stringSetPreferencesKey("normal_only_packages")
    }

    /**
     * Exports the AppPreferences class in a JSON string
     * { disabledPackages: string[], normalOnlyPackages: string[] }
     */
    suspend fun toJson(): String {
        fun toJsonStr(value: List<Set<String>>): String {
            return value.joinToString(separator = ",", prefix = "\"", postfix = "\"")
        }

        var resp = "{\"disabledPackages\":[" + toJsonStr(disabledPackages.toList()) + "],"
        resp += "\"normalOnlyPackages\":[" + toJsonStr(normalOnlyPackages.toList()) + "]}"
        return resp
    }
}
