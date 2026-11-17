package com.ekoehler.expressivecutout.data

/**
 * Visual treatment of the expanded island's action chips. Stored by [name] so it fits a single
 * preference key; a mix of Material 3 Expressive and Material You looks.
 */
enum class ActionButtonStyle {
    /** Material 3 Expressive: a translucent accent pill (the original look). */
    EXPRESSIVE_TONAL,
    /** Material 3 Expressive: a solid, fully-filled accent pill. */
    EXPRESSIVE_FILLED,
    /** Material You: a softly-rounded tonal container. */
    MATERIAL_YOU,
    /** An outlined chip over a transparent fill. */
    OUTLINED,
    ;

    companion object {
        fun deserialize(value: String?): ActionButtonStyle? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}

/** Horizontal placement of the expanded island's action-chip row. */
enum class ActionButtonAlignment {
    /** Chips hug the leading edge (the historical look). */
    LEFT,
    /** Chips are centred across the island. */
    CENTER,
    /** Chips hug the trailing edge. */
    RIGHT,

    /** Chips share the full width equally (each grows to fill), like CSS `flex: 1`. */
    FULL,
    ;

    companion object {
        fun deserialize(value: String?): ActionButtonAlignment? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}

/** Horizontal placement of the post-send "Sent" confirmation row shown before the island dismisses. */
enum class SentAlignment {
    /** Hugs the leading edge (the historical look). */
    LEFT,
    /** Centred across the island. */
    CENTER,
    /** Hugs the trailing edge. */
    RIGHT,
    ;

    companion object {
        fun deserialize(value: String?): SentAlignment? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}

/** Visual treatment of the inline reply text field. */
enum class ReplyInputStyle {
    /** Material 3 Expressive: a fully-rounded (pill) field. */
    EXPRESSIVE,
    /** Material You: a generously 16dp-rounded field. */
    MATERIAL_YOU,
    /** Material 2: a lightly 4dp-rounded field. */
    MATERIAL_2,

    /** Cancel, field and send joined as one connected bar with rounded end-caps. */
    SEGMENTED,
    ;

    companion object {
        fun deserialize(value: String?): ReplyInputStyle? =
            value?.let { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}
