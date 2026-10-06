package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Backing store for the status-bar hiding settings. */
private val Context.statusBarDataStore: DataStore<Preferences> by preferencesDataStore(name = "status_bar_prefs")

/**
 * What the user wants done to the system status bar, applied through Shizuku by
 * `StatusBarIconController`.
 *
 * This stores the *intent*, not the achieved state: the flags themselves live in system_server and
 * are lost whenever our process dies or the device reboots. Keeping the wish here is what lets the
 * controller re-apply it once Shizuku is reachable again.
 *
 * Each of the three hiding settings is a [StatusBarHideMode] rather than a switch, because a pill
 * wide enough to cover the notification icons at rest may still leave the system icons alone — so
 * the two want different rules on the same device.
 */
class StatusBarPreferences(private val context: Context) : JsonSerializable {

    val notificationIconsMode: Flow<StatusBarHideMode> = context.statusBarDataStore.data.map { prefs ->
        prefs.hideMode(NOTIFICATION_ICONS_MODE, HIDE_NOTIFICATION_ICONS)
    }

    val systemInfoMode: Flow<StatusBarHideMode> = context.statusBarDataStore.data.map { prefs ->
        prefs.hideMode(SYSTEM_INFO_MODE, HIDE_SYSTEM_INFO)
    }

    val clockMode: Flow<StatusBarHideMode> = context.statusBarDataStore.data.map { prefs ->
        prefs.hideMode(CLOCK_MODE, HIDE_CLOCK)
    }

    val silenceAlerts: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[SILENCE_ALERTS] ?: false
    }

    suspend fun setNotificationIconsMode(mode: StatusBarHideMode) = context.statusBarDataStore.edit { prefs ->
        prefs[NOTIFICATION_ICONS_MODE] = mode.name
    }

    suspend fun setSystemInfoMode(mode: StatusBarHideMode) = context.statusBarDataStore.edit { prefs ->
        prefs[SYSTEM_INFO_MODE] = mode.name
    }

    suspend fun setClockMode(mode: StatusBarHideMode) = context.statusBarDataStore.edit { prefs ->
        prefs[CLOCK_MODE] = mode.name
    }

    suspend fun setSilenceAlerts(silence: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[SILENCE_ALERTS] = silence
    }

    /**
     * Reads [modeKey], falling back to the booleans these settings were stored as before they grew
     * a third state, so an existing install keeps behaving the way the user left it: the old
     * "hide automatically" switch covered all three at once and becomes [StatusBarHideMode.AUTO],
     * and a plain on/off wish becomes [StatusBarHideMode.ALWAYS] or [StatusBarHideMode.OFF].
     */
    private fun Preferences.hideMode(
        modeKey: Preferences.Key<String>,
        legacyKey: Preferences.Key<Boolean>,
    ): StatusBarHideMode {
        val stored = this[modeKey]?.let { runCatching { StatusBarHideMode.valueOf(it) }.getOrNull() }
        if (stored != null) return stored
        if (this[LEGACY_AUTO_HIDE] == true) return StatusBarHideMode.AUTO
        return if (this[legacyKey] == true) StatusBarHideMode.ALWAYS else StatusBarHideMode.OFF
    }

    private companion object {
        val NOTIFICATION_ICONS_MODE = stringPreferencesKey("notification_icons_mode")
        val SYSTEM_INFO_MODE = stringPreferencesKey("system_info_mode")
        val CLOCK_MODE = stringPreferencesKey("clock_mode")
        val SILENCE_ALERTS = booleanPreferencesKey("silence_alerts")

        /** Read only to migrate an install made before the three-state modes; never written. */
        val HIDE_NOTIFICATION_ICONS = booleanPreferencesKey("hide_notification_icons")
        val HIDE_SYSTEM_INFO = booleanPreferencesKey("hide_system_info")
        val HIDE_CLOCK = booleanPreferencesKey("hide_clock")
        val LEGACY_AUTO_HIDE = booleanPreferencesKey("auto_hide_with_cutout")
    }

    /**
     * Exports the status-bar settings in a JSON string
     * { notificationIcons, systemInfo, clock: "OFF" | "AUTO" | "ALWAYS", silenceAlerts: boolean }
     */
    override suspend fun toJson(): String {
        val notificationIcons = notificationIconsMode.first()
        val systemInfo = systemInfoMode.first()
        val clock = clockMode.first()
        val silence = silenceAlerts.first()
        return JSONObject().apply {
            put("notificationIcons", notificationIcons.name)
            put("systemInfo", systemInfo.name)
            put("clock", clock.name)
            put("silenceAlerts", silence)
        }.toString()
    }

    /**
     * Applies { notificationIcons, systemInfo, clock: "OFF" | "AUTO" | "ALWAYS",
     * silenceAlerts: boolean } exported by [toJson], also accepting the booleans older documents
     * carry. Each missing field leaves its setting untouched — importing a document from a build
     * without this section shouldn't silently flip any flag.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        obj.hideMode("notificationIcons", "hideNotificationIcons")?.let { setNotificationIconsMode(it) }
        obj.hideMode("systemInfo", "hideSystemInfo")?.let { setSystemInfoMode(it) }
        obj.hideMode("clock", "hideClock")?.let { setClockMode(it) }
        if (obj.has("silenceAlerts")) {
            setSilenceAlerts(obj.optBoolean("silenceAlerts", false))
        }
    }

    /**
     * One setting's mode out of an exported document, read from [name] and falling back to the
     * boolean [legacyName] an older export wrote. Null when the document carries neither.
     */
    private fun JSONObject.hideMode(name: String, legacyName: String): StatusBarHideMode? {
        if (has(name)) {
            return runCatching { StatusBarHideMode.valueOf(optString(name)) }.getOrNull()
        }
        if (has(legacyName)) {
            return if (optBoolean(legacyName, false)) StatusBarHideMode.ALWAYS else StatusBarHideMode.OFF
        }
        return null
    }
}
