package com.ekoehler.expressivecutout.core

/**
 * The shapes the island's cutout can take, ordered by width: each later entry covers more of the
 * status bar than the one before it.
 */
enum class CutoutKind {
    /** The smallest resting pill. */
    MINI,

    /** The normal collapsed cutout. */
    NORMAL,

    /** Wider than normal, reaching over part of the status bar. */
    WIDE,

    /** The fully expanded island. */
    EXPANDED;

    companion object {
        /** The kinds that trigger an AUTO status-bar hide unless the user picks others. */
        val DEFAULT_AUTO_TRIGGERS: Set<CutoutKind> = setOf(WIDE, EXPANDED)
    }
}
