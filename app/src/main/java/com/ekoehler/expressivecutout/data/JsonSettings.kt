package com.ekoehler.expressivecutout.data

import org.json.JSONObject

/**
 * Central JSON (de)serialisation for the whole settings document.
 *
 * Each preference store owns the shape of its own section — via [JsonSerializable.toJson] /
 * [JsonSerializable.fromJson] — and this object owns everything above that: the document envelope
 * (a [FORMAT_KEY] marker plus a [VERSION_KEY]), assembling every store's section into one document
 * for export, checking that an incoming file is really one of ours, and routing each section back to
 * its store on import.
 *
 * The store instances live in `AppViewModel`; callers hand in an ordered label -> store map so the
 * set of sections is declared in exactly one place. The section labels below double as those map
 * keys, so the map and the validity check can't drift apart.
 */
object JsonSettings {
    /** Written at the document root so an imported file can be recognised as one of ours. */
    const val FORMAT_KEY = "format"
    const val FORMAT_VALUE = "expressive-cutout-settings"
    const val VERSION_KEY = "version"
    const val VERSION = 2

    /** Section labels — also the keys of the store map AppViewModel builds. */
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val LAYOUT = "layout"
    const val ICONS = "icons"
    const val BEHAVIOUR = "behaviour"
    const val APPEARANCE = "appearance"
    const val EVENTS = "events"
    const val DYNAMIC_TILES = "dynamicTiles"
    const val MUSIC_TILE = "musicTile"
    const val PHONE_TILE = "phoneTile"
    const val TIMER_TILE = "timerTile"
    const val ASSISTANT_TILE = "assistantTile"
    const val APPS = "apps"
    const val RECENT_COLORS = "recentColors"
    const val STATUS_BAR = "statusBar"
    const val PERMISSION_DOT = "permissionDot"

    /** The outcome of an [import], so the UI can tell the user exactly what happened. */
    enum class ImportResult { SUCCESS, NOT_A_SETTINGS_FILE, ERROR }

    /**
     * Serialises every [sections] store into one document: each store's [JsonSerializable.toJson]
     * output nested under its label, wrapped in the [FORMAT_KEY] / [VERSION_KEY] envelope.
     */
    suspend fun export(sections: Map<String, JsonSerializable>): String {
        val root = JSONObject().apply {
            put(FORMAT_KEY, FORMAT_VALUE)
            put(VERSION_KEY, VERSION)
        }
        for ((label, store) in sections) {
            root.put(label, JSONObject(store.toJson()))
        }
        return root.toString()
    }

    /**
     * Exports shared settings once and each posture-scoped section into a separate profile object.
     */
    suspend fun exportProfiles(
        sharedSections: Map<String, JsonSerializable>,
        profiles: Map<String, Map<String, JsonSerializable>>,
    ): String {
        val root = JSONObject().apply {
            put(FORMAT_KEY, FORMAT_VALUE)
            put(VERSION_KEY, VERSION)
        }
        for ((label, store) in sharedSections) {
            root.put(label, JSONObject(store.toJson()))
        }
        root.put(
            "profiles",
            JSONObject().apply {
                for ((profile, sections) in profiles) {
                    put(
                        profile,
                        JSONObject().apply {
                            for ((label, store) in sections) {
                                put(label, JSONObject(store.toJson()))
                            }
                        },
                    )
                }
            },
        )
        return root.toString()
    }

    /**
     * Parses [json] and applies each recognised section to its store. A section absent from the
     * document is left as-is, and one store failing to apply its own section doesn't abort the
     * others. Returns [ImportResult.NOT_A_SETTINGS_FILE] when [json] can't be parsed or doesn't look
     * like one of our exports, so the caller never half-applies an unrelated file.
     */
    suspend fun import(json: String, sections: Map<String, JsonSerializable>): ImportResult {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return ImportResult.NOT_A_SETTINGS_FILE
        if (!looksLikeOurs(root, sections.keys)) return ImportResult.NOT_A_SETTINGS_FILE

        return try {
            for ((label, store) in sections) {
                val section = root.optJSONObject(label) ?: continue
                runCatching { store.fromJson(section.toString()) }
            }
            ImportResult.SUCCESS
        } catch (e: Exception) {
            ImportResult.ERROR
        }
    }

    /**
     * Imports shared settings and posture profiles, treating a legacy flat document as the open
     * profile so existing exports remain usable.
     */
    suspend fun importProfiles(
        json: String,
        sharedSections: Map<String, JsonSerializable>,
        profiles: Map<String, Map<String, JsonSerializable>>,
        legacyProfile: String,
        sharedSectionFallbackProfiles: Map<String, String> = emptyMap(),
    ): ImportResult {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return ImportResult.NOT_A_SETTINGS_FILE
        val knownSections = sharedSections.keys + profiles.values.flatMap { it.keys }
        if (!looksLikeOurs(root, knownSections)) return ImportResult.NOT_A_SETTINGS_FILE

        return try {
            val profileData = root.optJSONObject("profiles")
            for ((label, store) in sharedSections) {
                val section = root.optJSONObject(label)
                    ?: sharedSectionFallbackProfiles[label]
                        ?.let { profile -> profileData?.optJSONObject(profile)?.optJSONObject(label) }
                section?.let {
                    runCatching { store.fromJson(it.toString()) }
                }
            }
            if (profileData == null) {
                val legacySections = profiles[legacyProfile].orEmpty()
                for ((label, store) in legacySections) {
                    root.optJSONObject(label)?.let { section ->
                        runCatching { store.fromJson(section.toString()) }
                    }
                }
            } else {
                for ((profile, sections) in profiles) {
                    val savedProfile = profileData.optJSONObject(profile) ?: continue
                    for ((label, store) in sections) {
                        savedProfile.optJSONObject(label)?.let { section ->
                            runCatching { store.fromJson(section.toString()) }
                        }
                    }
                }
            }
            ImportResult.SUCCESS
        } catch (e: Exception) {
            ImportResult.ERROR
        }
    }

    /**
     * A file is ours if it carries our [FORMAT_VALUE] marker, or — for files exported before the
     * marker existed — if it's a JSON object holding at least [MIN_SECTIONS_FOR_MATCH] of our known
     * section objects. The threshold keeps an unrelated JSON file that happens to share one key name
     * from being mistaken for a settings export.
     */
    private fun looksLikeOurs(root: JSONObject, knownSections: Set<String>): Boolean {
        if (root.optString(FORMAT_KEY) == FORMAT_VALUE) return true
        val present = knownSections.count { root.optJSONObject(it) != null }
        return present >= MIN_SECTIONS_FOR_MATCH
    }

    private const val MIN_SECTIONS_FOR_MATCH = 3
}
