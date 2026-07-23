package com.ekoehler.expressivecutout.data

/**
 * A user-chosen override for a system event's icon. Persisted as a single tagged string so
 * it round-trips through DataStore; an absent value means "use the built-in default".
 */
sealed interface IconSource {

    /** A picked image file, referenced by a persistable content URI. */
    data class Image(val uri: String) : IconSource

    /** The launcher icon of an installed app, referenced by its package name. */
    data class App(val packageName: String) : IconSource

    fun encode(): String = when (this) {
        is Image -> "$IMAGE_TAG$SEPARATOR$uri"
        is App -> "$APP_TAG$SEPARATOR$packageName"
    }

    companion object {
        private const val IMAGE_TAG = "image"
        private const val APP_TAG = "app"
        private const val SEPARATOR = "|"

        fun decode(raw: String): IconSource? {
            val index = raw.indexOf(SEPARATOR)
            if (index <= 0) return null
            val tag = raw.substring(0, index)
            val value = raw.substring(index + 1)
            if (value.isEmpty()) return null
            return when (tag) {
                IMAGE_TAG -> Image(value)
                APP_TAG -> App(value)
                else -> null
            }
        }
    }
}
