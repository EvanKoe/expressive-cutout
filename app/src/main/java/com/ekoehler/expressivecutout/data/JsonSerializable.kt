package com.ekoehler.expressivecutout.data

/**
 * A settings store whose current values round-trip through a JSON object string. Implemented by
 * every preference store so they can be exported and imported uniformly — see [JsonSettings], which
 * iterates them without knowing each concrete type.
 *
 * The two directions are symmetric: whatever [toJson] writes, [fromJson] reads back. Keeping both
 * next to the store's preference keys is what lets "add a new store" stay a one-line change in
 * [JsonSettings].
 */
interface JsonSerializable {
    /** This store's current settings as a JSON object string. */
    suspend fun toJson(): String

    /**
     * Applies settings from a JSON object string previously produced by [toJson]. Implementations
     * are lenient: a missing or malformed field is skipped, leaving that setting untouched, so a
     * partial or slightly-outdated document still imports what it can.
     */
    suspend fun fromJson(json: String)
}
