package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.statusBarDataStore: DataStore<Preferences> by preferencesDataStore(name = "status_bar_prefs")

/**
 * What the user wants done to the system status bar, applied through Shizuku by
 * `StatusBarIconController`.
 *
 * This stores the *intent*, not the achieved state: the flags themselves live in system_server and
 * are lost whenever our process dies or the device reboots. Keeping the wish here is what lets the
 * controller re-apply it once Shizuku is reachable again.
 */
class StatusBarPreferences(private val context: Context) : JsonSerializable {

    val hideNotificationIcons: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_NOTIFICATION_ICONS] ?: false
    }

    suspend fun setHideNotificationIcons(hide: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[HIDE_NOTIFICATION_ICONS] = hide
    }

    private companion object {
        val HIDE_NOTIFICATION_ICONS = booleanPreferencesKey("hide_notification_icons")
    }

    /**
     * Exports the status-bar settings in a JSON string
     * { hideNotificationIcons: boolean }
     */
    override suspend fun toJson(): String {
        val hide = hideNotificationIcons.first()
        return JSONObject().apply {
            put("hideNotificationIcons", hide)
        }.toString()
    }

    /**
     * Applies { hideNotificationIcons: boolean } exported by [toJson]. A missing field leaves the
     * setting untouched — importing a document from a build without this section shouldn't silently
     * turn the icons back on.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        if (!obj.has("hideNotificationIcons")) return
        setHideNotificationIcons(obj.optBoolean("hideNotificationIcons", false))
    }
}
