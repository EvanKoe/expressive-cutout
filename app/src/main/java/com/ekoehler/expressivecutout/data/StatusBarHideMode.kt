package com.ekoehler.expressivecutout.data

/**
 * When one of the status-bar hiding settings applies. Declaration order is the order the options
 * are shown in, so the picker and the enum can't drift apart.
 */
enum class StatusBarHideMode {
    /** Never hidden — the system status bar draws it as usual. */
    OFF,

    /** Hidden while the cutout is visible in its collapsed state. */
    NORMAL,

    /** Hidden only while the cutout is expanded. */
    EXPANDED,

    /** Hidden the whole time, whatever the island is doing. */
    ALWAYS;

    /** Whether this mode hides right now, given the cutout's current [visible] and [expanded] state. */
    fun hides(visible: Boolean, expanded: Boolean = false): Boolean = when (this) {
        OFF -> false
        NORMAL -> visible && !expanded
        EXPANDED -> visible && expanded
        ALWAYS -> true
    }
}
