package com.ekoehler.expressivecutout.data

import com.ekoehler.expressivecutout.core.CutoutKind

/**
 * When one of the status-bar hiding settings applies. Declaration order is the order the options
 * are shown in, so the picker and the enum can't drift apart.
 */
enum class StatusBarHideMode {
    /** Never hidden — the system status bar draws it as usual. */
    OFF,

    /** Hidden only while the island's current [CutoutKind] is one of the user's chosen triggers. */
    AUTO,

    /** Hidden the whole time, whatever the island is doing. */
    ALWAYS;

    /**
     * Whether this mode hides right now, given the island's current [kind] (null when it has none)
     * and the [triggers] that AUTO reacts to.
     */
    fun hides(kind: CutoutKind?, triggers: Set<CutoutKind>): Boolean = when (this) {
        OFF -> false
        AUTO -> kind != null && kind in triggers
        ALWAYS -> true
    }
}
