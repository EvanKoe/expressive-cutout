package com.ekoehler.expressivecutout.data

import com.ekoehler.expressivecutout.core.CutoutKind

/**
 * How one status-bar hiding setting behaves while the device is locked. With [separate] false the
 * unlocked rule is used as is; [mode] and [triggers] only matter once [separate] is true.
 */
data class LockscreenRule(
    val separate: Boolean = false,
    val mode: StatusBarHideMode = StatusBarHideMode.OFF,
    val triggers: Set<CutoutKind> = CutoutKind.DEFAULT_AUTO_TRIGGERS,
)
