package com.ekoehler.expressivecutout.data

/**
 * A settings store whose current values can be serialised to a JSON string. Implemented by every
 * preference store so they can be exported uniformly — see `AppViewModel.exportSettingsToJson`,
 * which iterates them without knowing each concrete type.
 */
interface JsonExportable {
    /** This store's current settings as a JSON object string. */
    suspend fun toJson(): String
}
