package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

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
class AppPreferences(private val context: Context) : JsonSerializable {

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
    override suspend fun toJson(): String {
        // .first() reads the current snapshot and cancels; .toList() would hang forever, since the
        // DataStore flow never completes (see the export bug this replaced).
        val disabled = disabledPackages.first()
        val normalOnly = normalOnlyPackages.first()
        return JSONObject().apply {
            put("disabledPackages", JSONArray(disabled))
            put("normalOnlyPackages", JSONArray(normalOnly))
        }.toString()
    }

    /**
     * Applies { disabledPackages: [...], normalOnlyPackages: [...] } exported by [toJson] as a full
     * replacement of both opt-out sets. A missing array clears that set, matching the snapshot.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.perAppDataStore.edit {
            it[DISABLED_KEY] = obj.optJSONArray("disabledPackages").toStringSet()
            it[NORMAL_ONLY_KEY] = obj.optJSONArray("normalOnlyPackages").toStringSet()
        }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }.toSet()
    }
}
