package com.ekoehler.expressivecutout.data

/** Where a notification's icon glyph, source app name and timestamp take their colour from. */
enum class NotificationColorMode {
    /** The island's own accent (the historical look). */
    MATERIAL,

    /** The colour extracted from the posting app's launcher icon. */
    APP,

    /** One colour the user picked, for every notification. */
    OTHER,
    ;

    companion object {
        fun deserialize(value: String?): NotificationColorMode? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}
