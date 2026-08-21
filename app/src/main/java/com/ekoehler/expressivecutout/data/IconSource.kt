package com.ekoehler.expressivecutout.data

import androidx.compose.runtime.Immutable

/**
 * A user-chosen override for a system event's icon. Persisted as a single tagged string so
 * it round-trips through DataStore; an absent value means "use the built-in default".
 */
@Immutable
sealed interface IconSource {

    /** A picked image file, referenced by a persistable content URI. */
    data class Image(val uri: String) : IconSource

    /** A built-in Material icon, referenced by its stable catalog key (see MaterialIconCatalog). */
    data class Material(val iconName: String) : IconSource

    /**
     * Encodes to a single tagged string so the icon choice fits one preference key. Read back by
     * [decode].
     */
    fun encode(): String = when (this) {
        is Image -> "$IMAGE_TAG$SEPARATOR$uri"
        is Material -> "$MATERIAL_TAG$SEPARATOR$iconName"
    }

    companion object {
        private const val IMAGE_TAG = "image"
        private const val MATERIAL_TAG = "material"
        private const val SEPARATOR = "|"

        fun decode(raw: String): IconSource? {
            val index = raw.indexOf(SEPARATOR)
            if (index <= 0) return null
            val tag = raw.substring(0, index)
            val value = raw.substring(index + 1)
            if (value.isEmpty()) return null
            return when (tag) {
                IMAGE_TAG -> Image(value)
                MATERIAL_TAG -> Material(value)
                else -> null
            }
        }
    }
}
