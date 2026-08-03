package com.ekoehler.expressivecutout.data

import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import androidx.annotation.StringRes
import com.ekoehler.expressivecutout.R

/**
 * A single actionable slot in the expanded "center" that the empty pill opens (see
 * [EmptyClickAction.OPEN_CENTER]). Each shortcut is persisted as a single tagged string so a user's
 * ordered list round-trips through DataStore, mirroring [IconSource]; the side effect of each variant
 * is carried out by [com.ekoehler.expressivecutout.core.CenterShortcutExecutor].
 */
sealed interface CenterShortcut {

    /** A system-wide accessibility action (lock, screenshot, shade…), free via the accessibility service. */
    data class Global(val action: GlobalAction) : CenterShortcut

    /** The camera flash, toggled on/off. */
    data object Torch : CenterShortcut

    /** Opens a system settings slide-up panel (Wi-Fi, internet, NFC, volume). */
    data class Panel(val panel: SettingsPanel) : CenterShortcut

    /** Launches an installed app by package name (reuses the empty-pill app picker). */
    data class LaunchApp(val packageName: String) : CenterShortcut

    /**
     * Whether performing this shortcut should leave the center open. In-place toggles (the torch)
     * keep it up so the user can flip it straight back or see it as a live toggle; anything that
     * navigates away, opens a system panel, or captures the screen closes the center.
     */
    val keepsCenterOpen: Boolean
        get() = when (this) {
            Torch -> true
            is Global, is Panel, is LaunchApp -> false
        }

    fun encode(): String = when (this) {
        is Global -> "$GLOBAL_TAG$SEPARATOR${action.name}"
        Torch -> TORCH_TAG
        is Panel -> "$PANEL_TAG$SEPARATOR${panel.name}"
        is LaunchApp -> "$APP_TAG$SEPARATOR$packageName"
    }

    companion object {
        private const val GLOBAL_TAG = "global"
        private const val TORCH_TAG = "torch"
        private const val PANEL_TAG = "panel"
        private const val APP_TAG = "app"
        private const val SEPARATOR = "|"
        private const val LIST_SEPARATOR = "\n"

        /** Round-trips a single [encode]d shortcut; returns null for an unknown tag or dropped enum. */
        fun decode(raw: String): CenterShortcut? {
            if (raw == TORCH_TAG) return Torch
            val index = raw.indexOf(SEPARATOR)
            if (index <= 0) return null
            val value = raw.substring(index + 1)
            if (value.isEmpty()) return null
            return when (raw.substring(0, index)) {
                GLOBAL_TAG -> runCatching { GlobalAction.valueOf(value) }.getOrNull()?.let(::Global)
                PANEL_TAG -> runCatching { SettingsPanel.valueOf(value) }.getOrNull()?.let(::Panel)
                APP_TAG -> LaunchApp(value)
                else -> null
            }
        }

        /** Packs an ordered list into one DataStore-friendly string. */
        fun encodeList(shortcuts: List<CenterShortcut>): String =
            shortcuts.joinToString(LIST_SEPARATOR) { it.encode() }

        /** Unpacks [encodeList]; a null/blank value (never customised) yields [DEFAULTS]. */
        fun decodeList(raw: String?): List<CenterShortcut> {
            if (raw.isNullOrBlank()) return DEFAULTS
            return raw.split(LIST_SEPARATOR).mapNotNull { if (it.isBlank()) null else decode(it) }
        }

        /** The starter row shown before the user customises: flashlight, screenshot, lock, quick settings. */
        val DEFAULTS: List<CenterShortcut> = listOf(
            Torch,
            Global(GlobalAction.SCREENSHOT),
            Global(GlobalAction.LOCK_SCREEN),
            Global(GlobalAction.QUICK_SETTINGS),
        )

        /**
         * Every built-in (non-app) shortcut, offered in the add-shortcut picker. Apps are added
         * separately via the app picker, so they aren't listed here.
         */
        val BUILTINS: List<CenterShortcut> =
            listOf(Torch) +
                GlobalAction.entries.map(::Global) +
                SettingsPanel.entries.map(::Panel)
    }
}

/**
 * System actions the accessibility service can perform for free via
 * [AccessibilityService.performGlobalAction]. [minSdk] guards the few that landed after our minSdk (29);
 * the executor skips a shortcut whose action isn't available on the running device.
 */
enum class GlobalAction(
    val action: Int,
    val minSdk: Int,
    @param:StringRes val labelRes: Int,
) {
    LOCK_SCREEN(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, 28, R.string.center_action_lock),
    SCREENSHOT(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, 30, R.string.center_action_screenshot),
    POWER_DIALOG(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, 21, R.string.center_action_power),
    NOTIFICATIONS(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, 16, R.string.center_action_notifications),
    QUICK_SETTINGS(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, 17, R.string.center_action_quick_settings),
    RECENTS(AccessibilityService.GLOBAL_ACTION_RECENTS, 16, R.string.center_action_recents),
}

/** System settings slide-up panels opened with a plain intent (all available from our minSdk 29). */
enum class SettingsPanel(
    val action: String,
    @param:StringRes val labelRes: Int,
) {
    INTERNET(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, R.string.center_panel_internet),
    WIFI(Settings.Panel.ACTION_WIFI, R.string.center_panel_wifi),
    NFC(Settings.Panel.ACTION_NFC, R.string.center_panel_nfc),
    VOLUME(Settings.Panel.ACTION_VOLUME, R.string.center_panel_volume),
}
