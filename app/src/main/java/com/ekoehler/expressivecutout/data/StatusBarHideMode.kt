package com.ekoehler.expressivecutout.data

/**
 * When one of the status-bar hiding settings applies. Declaration order is the order the options
 * are shown in, so the picker and the enum can't drift apart.
 */
enum class StatusBarHideMode {
    /** Never hidden — the system status bar draws it as usual. */
    OFF,

    /** Hidden only while the island is drawn wider than its normal collapsed cutout. */
    AUTO,

    /** Hidden the whole time, whatever the island is doing. */
    ALWAYS;

    /** Whether this mode hides right now, given whether the cutout is currently [widened]. */
    fun hides(widened: Boolean): Boolean = when (this) {
        OFF -> false
        AUTO -> widened
        ALWAYS -> true
    }
}
