package com.ekoehler.expressivecutout.overlay

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekoehler.expressivecutout.data.CenterShortcut
import com.ekoehler.expressivecutout.data.GlobalAction
import com.ekoehler.expressivecutout.data.SettingsPanel

/**
 * Maps a [CenterShortcut] to what the UI draws for it: a glyph and a label. Kept out of the data
 * layer so [CenterShortcut] stays free of Compose, mirroring how [MaterialIconCatalog] holds the
 * vectors for [com.ekoehler.expressivecutout.data.IconSource].
 */
object CenterShortcutCatalog {

    /** The glyph for a shortcut. For [CenterShortcut.LaunchApp] this is only a fallback — the UI
     *  should prefer the real launcher icon (see the empty-pill app picker). */
    fun iconFor(shortcut: CenterShortcut): ImageVector = when (shortcut) {
        CenterShortcut.Torch -> Icons.Rounded.FlashlightOn
        is CenterShortcut.Global -> when (shortcut.action) {
            GlobalAction.LOCK_SCREEN -> Icons.Rounded.Lock
            GlobalAction.SCREENSHOT -> Icons.Rounded.Screenshot
            GlobalAction.POWER_DIALOG -> Icons.Rounded.PowerSettingsNew
            GlobalAction.NOTIFICATIONS -> Icons.Rounded.Notifications
            GlobalAction.QUICK_SETTINGS -> Icons.Rounded.Tune
            GlobalAction.RECENTS -> Icons.Rounded.Apps
        }
        is CenterShortcut.Panel -> when (shortcut.panel) {
            SettingsPanel.INTERNET -> Icons.Rounded.SignalCellularAlt
            SettingsPanel.WIFI -> Icons.Rounded.Wifi
            SettingsPanel.NFC -> Icons.Rounded.Nfc
            SettingsPanel.VOLUME -> Icons.Rounded.VolumeUp
        }
        is CenterShortcut.LaunchApp -> Icons.Rounded.Android
    }

    /** The label string resource, or null for [CenterShortcut.LaunchApp] (resolved from PackageManager). */
    @StringRes
    fun labelResFor(shortcut: CenterShortcut): Int? = when (shortcut) {
        CenterShortcut.Torch -> com.ekoehler.expressivecutout.R.string.center_action_torch
        is CenterShortcut.Global -> shortcut.action.labelRes
        is CenterShortcut.Panel -> shortcut.panel.labelRes
        is CenterShortcut.LaunchApp -> null
    }
}
