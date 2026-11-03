package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.ekoehler.expressivecutout.core.CutoutKind
import org.json.JSONArray
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
 * the two want different rules on the same device. Each can also carry an optional
 * [LockscreenRule] that replaces it while the device is locked.
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

    /** The [CutoutKind]s that hide the notification icons while that setting is AUTO. */
    val notificationIconsAutoTriggers: Flow<Set<CutoutKind>> = context.statusBarDataStore.data.map { prefs ->
        prefs.autoTriggers(NOTIFICATION_ICONS_AUTO_TRIGGERS)
    }

    /** The [CutoutKind]s that hide the system info icons while that setting is AUTO. */
    val systemInfoAutoTriggers: Flow<Set<CutoutKind>> = context.statusBarDataStore.data.map { prefs ->
        prefs.autoTriggers(SYSTEM_INFO_AUTO_TRIGGERS)
    }

    /** The [CutoutKind]s that hide the clock while that setting is AUTO. */
    val clockAutoTriggers: Flow<Set<CutoutKind>> = context.statusBarDataStore.data.map { prefs ->
        prefs.autoTriggers(CLOCK_AUTO_TRIGGERS)
    }

    /** The lock-screen override of the notification icons setting. */
    val notificationIconsLockscreen: Flow<LockscreenRule> = context.statusBarDataStore.data.map { prefs ->
        prefs.lockscreenRule(NOTIFICATION_ICONS_LOCKSCREEN_SEPARATE, NOTIFICATION_ICONS_LOCKSCREEN_MODE, NOTIFICATION_ICONS_LOCKSCREEN_TRIGGERS)
    }

    /** The lock-screen override of the system info setting. */
    val systemInfoLockscreen: Flow<LockscreenRule> = context.statusBarDataStore.data.map { prefs ->
        prefs.lockscreenRule(SYSTEM_INFO_LOCKSCREEN_SEPARATE, SYSTEM_INFO_LOCKSCREEN_MODE, SYSTEM_INFO_LOCKSCREEN_TRIGGERS)
    }

    /** The lock-screen override of the clock setting. */
    val clockLockscreen: Flow<LockscreenRule> = context.statusBarDataStore.data.map { prefs ->
        prefs.lockscreenRule(CLOCK_LOCKSCREEN_SEPARATE, CLOCK_LOCKSCREEN_MODE, CLOCK_LOCKSCREEN_TRIGGERS)
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

    suspend fun setNotificationIconsAutoTriggers(triggers: Set<CutoutKind>) = context.statusBarDataStore.edit { prefs ->
        prefs[NOTIFICATION_ICONS_AUTO_TRIGGERS] = triggers.mapTo(mutableSetOf()) { it.name }
    }

    suspend fun setSystemInfoAutoTriggers(triggers: Set<CutoutKind>) = context.statusBarDataStore.edit { prefs ->
        prefs[SYSTEM_INFO_AUTO_TRIGGERS] = triggers.mapTo(mutableSetOf()) { it.name }
    }

    suspend fun setClockAutoTriggers(triggers: Set<CutoutKind>) = context.statusBarDataStore.edit { prefs ->
        prefs[CLOCK_AUTO_TRIGGERS] = triggers.mapTo(mutableSetOf()) { it.name }
    }

    suspend fun setNotificationIconsLockscreen(rule: LockscreenRule) = context.statusBarDataStore.edit { prefs ->
        prefs.putLockscreenRule(rule, NOTIFICATION_ICONS_LOCKSCREEN_SEPARATE, NOTIFICATION_ICONS_LOCKSCREEN_MODE, NOTIFICATION_ICONS_LOCKSCREEN_TRIGGERS)
    }

    suspend fun setSystemInfoLockscreen(rule: LockscreenRule) = context.statusBarDataStore.edit { prefs ->
        prefs.putLockscreenRule(rule, SYSTEM_INFO_LOCKSCREEN_SEPARATE, SYSTEM_INFO_LOCKSCREEN_MODE, SYSTEM_INFO_LOCKSCREEN_TRIGGERS)
    }

    suspend fun setClockLockscreen(rule: LockscreenRule) = context.statusBarDataStore.edit { prefs ->
        prefs.putLockscreenRule(rule, CLOCK_LOCKSCREEN_SEPARATE, CLOCK_LOCKSCREEN_MODE, CLOCK_LOCKSCREEN_TRIGGERS)
    }

    suspend fun setSilenceAlerts(silence: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[SILENCE_ALERTS] = silence
    }

    /** Writes the three keys of one [LockscreenRule] into an open edit. */
    private fun MutablePreferences.putLockscreenRule(
        rule: LockscreenRule,
        separateKey: Preferences.Key<Boolean>,
        modeKey: Preferences.Key<String>,
        triggersKey: Preferences.Key<Set<String>>,
    ) {
        this[separateKey] = rule.separate
        this[modeKey] = rule.mode.name
        this[triggersKey] = rule.triggers.mapTo(mutableSetOf()) { it.name }
    }

    /** Reads one [LockscreenRule] from its three keys, each field defaulting like [LockscreenRule] when unset or unreadable. */
    private fun Preferences.lockscreenRule(
        separateKey: Preferences.Key<Boolean>,
        modeKey: Preferences.Key<String>,
        triggersKey: Preferences.Key<Set<String>>,
    ): LockscreenRule {
        val default = LockscreenRule()
        return LockscreenRule(
            separate = this[separateKey] ?: default.separate,
            mode = this[modeKey]?.let { runCatching { StatusBarHideMode.valueOf(it) }.getOrNull() } ?: default.mode,
            triggers = autoTriggers(triggersKey),
        )
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

    /** Reads the trigger set at [key], defaulting to [CutoutKind.DEFAULT_AUTO_TRIGGERS] when never saved. */
    private fun Preferences.autoTriggers(key: Preferences.Key<Set<String>>): Set<CutoutKind> =
        this[key]?.mapNotNullTo(mutableSetOf()) { runCatching { CutoutKind.valueOf(it) }.getOrNull() }
            ?: CutoutKind.DEFAULT_AUTO_TRIGGERS

    private companion object {
        val NOTIFICATION_ICONS_MODE = stringPreferencesKey("notification_icons_mode")
        val SYSTEM_INFO_MODE = stringPreferencesKey("system_info_mode")
        val CLOCK_MODE = stringPreferencesKey("clock_mode")
        val NOTIFICATION_ICONS_AUTO_TRIGGERS = stringSetPreferencesKey("notification_icons_auto_triggers")
        val SYSTEM_INFO_AUTO_TRIGGERS = stringSetPreferencesKey("system_info_auto_triggers")
        val CLOCK_AUTO_TRIGGERS = stringSetPreferencesKey("clock_auto_triggers")
        val SILENCE_ALERTS = booleanPreferencesKey("silence_alerts")
        val NOTIFICATION_ICONS_LOCKSCREEN_SEPARATE = booleanPreferencesKey("notification_icons_lockscreen_separate")
        val NOTIFICATION_ICONS_LOCKSCREEN_MODE = stringPreferencesKey("notification_icons_lockscreen_mode")
        val NOTIFICATION_ICONS_LOCKSCREEN_TRIGGERS = stringSetPreferencesKey("notification_icons_lockscreen_triggers")
        val SYSTEM_INFO_LOCKSCREEN_SEPARATE = booleanPreferencesKey("system_info_lockscreen_separate")
        val SYSTEM_INFO_LOCKSCREEN_MODE = stringPreferencesKey("system_info_lockscreen_mode")
        val SYSTEM_INFO_LOCKSCREEN_TRIGGERS = stringSetPreferencesKey("system_info_lockscreen_triggers")
        val CLOCK_LOCKSCREEN_SEPARATE = booleanPreferencesKey("clock_lockscreen_separate")
        val CLOCK_LOCKSCREEN_MODE = stringPreferencesKey("clock_lockscreen_mode")
        val CLOCK_LOCKSCREEN_TRIGGERS = stringSetPreferencesKey("clock_lockscreen_triggers")

        /** Read only to migrate an install made before the three-state modes; never written. */
        val HIDE_NOTIFICATION_ICONS = booleanPreferencesKey("hide_notification_icons")
        val HIDE_SYSTEM_INFO = booleanPreferencesKey("hide_system_info")
        val HIDE_CLOCK = booleanPreferencesKey("hide_clock")
        val LEGACY_AUTO_HIDE = booleanPreferencesKey("auto_hide_with_cutout")
    }

    /**
     * Exports the status-bar settings in a JSON string
     * { notificationIcons, systemInfo, clock: "OFF" | "AUTO" | "ALWAYS", silenceAlerts: boolean,
     *   notificationIconsAutoTriggers, systemInfoAutoTriggers, clockAutoTriggers: CutoutKind names[],
     *   notificationIconsLockscreen, systemInfoLockscreen, clockLockscreen:
     *     { separate: boolean, mode: "OFF" | "AUTO" | "ALWAYS", triggers: CutoutKind names[] } }
     */
    override suspend fun toJson(): String {
        val notificationIcons = notificationIconsMode.first()
        val systemInfo = systemInfoMode.first()
        val clock = clockMode.first()
        val silence = silenceAlerts.first()
        val notificationTriggers = notificationIconsAutoTriggers.first()
        val systemInfoTriggers = systemInfoAutoTriggers.first()
        val clockTriggers = clockAutoTriggers.first()
        val notificationLockscreen = notificationIconsLockscreen.first()
        val systemInfoLockscreenRule = systemInfoLockscreen.first()
        val clockLockscreenRule = clockLockscreen.first()
        return JSONObject().apply {
            put("notificationIcons", notificationIcons.name)
            put("systemInfo", systemInfo.name)
            put("clock", clock.name)
            put("silenceAlerts", silence)
            put("notificationIconsAutoTriggers", JSONArray(notificationTriggers.map { it.name }))
            put("systemInfoAutoTriggers", JSONArray(systemInfoTriggers.map { it.name }))
            put("clockAutoTriggers", JSONArray(clockTriggers.map { it.name }))
            put("notificationIconsLockscreen", notificationLockscreen.toJson())
            put("systemInfoLockscreen", systemInfoLockscreenRule.toJson())
            put("clockLockscreen", clockLockscreenRule.toJson())
        }.toString()
    }

    /** The { separate, mode, triggers } object [toJson] writes for one rule. */
    private fun LockscreenRule.toJson(): JSONObject = JSONObject().apply {
        put("separate", separate)
        put("mode", mode.name)
        put("triggers", JSONArray(triggers.map { it.name }))
    }

    /**
     * Applies { notificationIcons, systemInfo, clock: "OFF" | "AUTO" | "ALWAYS",
     * silenceAlerts: boolean, notificationIconsAutoTriggers / systemInfoAutoTriggers /
     * clockAutoTriggers: CutoutKind names[], notificationIconsLockscreen / systemInfoLockscreen /
     * clockLockscreen: { separate, mode, triggers } } exported by [toJson], also accepting the booleans
     * older documents carry. Each missing field leaves its setting untouched — importing a document
     * from a build without this section shouldn't silently flip any flag.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        obj.hideMode("notificationIcons", "hideNotificationIcons")?.let { setNotificationIconsMode(it) }
        obj.hideMode("systemInfo", "hideSystemInfo")?.let { setSystemInfoMode(it) }
        obj.hideMode("clock", "hideClock")?.let { setClockMode(it) }
        if (obj.has("silenceAlerts")) {
            setSilenceAlerts(obj.optBoolean("silenceAlerts", false))
        }
        obj.triggers("notificationIconsAutoTriggers")?.let { setNotificationIconsAutoTriggers(it) }
        obj.triggers("systemInfoAutoTriggers")?.let { setSystemInfoAutoTriggers(it) }
        obj.triggers("clockAutoTriggers")?.let { setClockAutoTriggers(it) }
        obj.optJSONObject("notificationIconsLockscreen")?.let { setNotificationIconsLockscreen(it.toLockscreenRule()) }
        obj.optJSONObject("systemInfoLockscreen")?.let { setSystemInfoLockscreen(it.toLockscreenRule()) }
        obj.optJSONObject("clockLockscreen")?.let { setClockLockscreen(it.toLockscreenRule()) }
    }

    /** One rule out of a { separate, mode, triggers } object, each missing or unknown field taking the [LockscreenRule] default. */
    private fun JSONObject.toLockscreenRule(): LockscreenRule {
        val default = LockscreenRule()
        return LockscreenRule(
            separate = optBoolean("separate", default.separate),
            mode = runCatching { StatusBarHideMode.valueOf(optString("mode")) }.getOrNull() ?: default.mode,
            triggers = triggers("triggers") ?: default.triggers,
        )
    }

    /** The [CutoutKind] names in the array at [name], unknown names dropped; null when absent. */
    private fun JSONObject.triggers(name: String): Set<CutoutKind>? {
        val array = optJSONArray(name) ?: return null
        return (0 until array.length()).mapNotNullTo(mutableSetOf()) { i ->
            runCatching { CutoutKind.valueOf(array.getString(i)) }.getOrNull()
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
