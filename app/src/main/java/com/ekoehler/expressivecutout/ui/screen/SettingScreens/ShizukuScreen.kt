package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.ShapeLine
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutKind
import com.ekoehler.expressivecutout.data.LockscreenRule
import com.ekoehler.expressivecutout.data.StatusBarHideMode
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import com.ekoehler.expressivecutout.ui.components.OptionSelectionRows
import com.ekoehler.expressivecutout.ui.components.PageTitle
import com.ekoehler.expressivecutout.ui.components.SelectableOption
import com.ekoehler.expressivecutout.ui.components.groupedShape

/**
 * "Shizuku options" screen (reached from the settings list). Houses the tweaks that need shell
 * privileges we can't hold ourselves: hiding the system status bar's notification icons so the
 * island isn't reporting the same notification twice, and silencing the system's own alerts.
 *
 * Everything here is gated on Shizuku being reachable — surfaced as a dynamic-coloured card at the
 * top, matching how the settings list flags a missing accessibility grant. Shizuku stops on every
 * reboot, so that card is a normal sight rather than a one-time setup step.
 */
@Composable
internal fun ShizukuScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenPermissionDot: () -> Unit,
) {
    val context = LocalContext.current
    val iconsMode by viewModel.notificationIconsMode.collectAsStateWithLifecycle()
    val systemInfoMode by viewModel.systemInfoMode.collectAsStateWithLifecycle()
    val clockMode by viewModel.clockMode.collectAsStateWithLifecycle()
    val iconsTriggers by viewModel.notificationIconsAutoTriggers.collectAsStateWithLifecycle()
    val systemInfoTriggers by viewModel.systemInfoAutoTriggers.collectAsStateWithLifecycle()
    val clockTriggers by viewModel.clockAutoTriggers.collectAsStateWithLifecycle()
    val iconsLockscreen by viewModel.notificationIconsLockscreen.collectAsStateWithLifecycle()
    val systemInfoLockscreen by viewModel.systemInfoLockscreen.collectAsStateWithLifecycle()
    val clockLockscreen by viewModel.clockLockscreen.collectAsStateWithLifecycle()
    val silenceAlerts by viewModel.silenceSystemAlerts.collectAsStateWithLifecycle()
    val permissionDot by viewModel.permissionDotEnabled.collectAsStateWithLifecycle()
    val shizuku by ShizukuState.status.collectAsStateWithLifecycle()

    // Returning from the Shizuku app is the one moment the state reliably changes without a binder
    // callback firing first, so re-read on resume exactly like the permission helpers do.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ShizukuState.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ready = shizuku == ShizukuStatus.READY

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageTitle(text = stringResource(R.string.shizuku_options_title))

        AnimatedVisibility(
            visible = !ready,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        ) {
            ShizukuCard(
                status = shizuku,
                onClick = {
                    if (shizuku == ShizukuStatus.PERMISSION_REQUIRED) ShizukuState.requestPermission()
                    else Permissions.openShizuku(context)
                },
            )
        }

        // The preview stands for the resting status bar, so "Auto" reads as nothing hidden yet.
        StatusBarPreview(
            hideIcons = ready && iconsMode.hides(kind = null, triggers = emptySet()),
            hideSystem = ready && systemInfoMode.hides(kind = null, triggers = emptySet()),
            hideClock = ready && clockMode.hides(kind = null, triggers = emptySet()),
        )

        Text(
            text = stringResource(R.string.status_bar_hide_mode_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Text(
            text = stringResource(R.string.status_bar_hide_icons_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBarHideModeCard(
                shape = groupedShape(isFirst = true, isLast = false),
                title = stringResource(R.string.status_bar_hide_icons_title),
                description = stringResource(R.string.status_bar_hide_icons_desc),
                selected = iconsMode,
                onSelect = viewModel::setNotificationIconsMode,
                triggers = iconsTriggers,
                onTriggersChange = viewModel::setNotificationIconsAutoTriggers,
                lockscreen = iconsLockscreen,
                onLockscreenChange = viewModel::setNotificationIconsLockscreen,
                enabled = ready,
            )

            StatusBarHideModeCard(
                shape = groupedShape(isFirst = false, isLast = false),
                title = stringResource(R.string.status_bar_hide_system_info_title),
                description = stringResource(R.string.status_bar_hide_system_info_desc),
                selected = systemInfoMode,
                onSelect = viewModel::setSystemInfoMode,
                triggers = systemInfoTriggers,
                onTriggersChange = viewModel::setSystemInfoAutoTriggers,
                lockscreen = systemInfoLockscreen,
                onLockscreenChange = viewModel::setSystemInfoLockscreen,
                enabled = ready,
            )

            StatusBarHideModeCard(
                shape = groupedShape(isFirst = false, isLast = true),
                title = stringResource(R.string.status_bar_hide_clock_title),
                description = stringResource(R.string.status_bar_hide_clock_desc),
                selected = clockMode,
                onSelect = viewModel::setClockMode,
                triggers = clockTriggers,
                onTriggersChange = viewModel::setClockAutoTriggers,
                lockscreen = clockLockscreen,
                onLockscreenChange = viewModel::setClockLockscreen,
                enabled = ready,
            )
        }

        SettingsToggleCard(
            shape = groupedShape(isFirst = true, isLast = true),
            title = stringResource(R.string.status_bar_silence_alerts_title),
            description = stringResource(R.string.status_bar_silence_alerts_desc),
            checked = ready && silenceAlerts,
            onCheckedChange = viewModel::setSilenceSystemAlerts,
            enabled = ready,
        )

        SettingsToggleNavCard(
            shape = groupedShape(isFirst = true, isLast = true),
            title = stringResource(R.string.permission_dot_title),
            description = stringResource(R.string.permission_dot_desc),
            checked = ready && permissionDot,
            onCheckedChange = viewModel::setPermissionDotEnabled,
            onClick = onOpenPermissionDot,
        )
    }
}

/**
 * One status-bar hiding setting, picked as a [StatusBarHideMode] rather than switched on and off.
 * Three states are needed because the island's resting width covers the two ends of the bar
 * unequally: a pill wide enough to sit over the notification icons all the time may still leave the
 * battery and Wi-Fi alone until it grows, so each end wants its own rule.
 *
 * [StatusBarHideMode]'s declaration order is the option order, so the two can't drift apart.
 * A switch at the bottom opens a [LockscreenRule] that overrides the rule while the device is locked.
 */
@Composable
private fun StatusBarHideModeCard(
    shape: Shape,
    title: String,
    description: String,
    selected: StatusBarHideMode,
    onSelect: (StatusBarHideMode) -> Unit,
    triggers: Set<CutoutKind>,
    onTriggersChange: (Set<CutoutKind>) -> Unit,
    lockscreen: LockscreenRule,
    onLockscreenChange: (LockscreenRule) -> Unit,
    enabled: Boolean,
) {
    val modes = StatusBarHideMode.entries
    val labels = modes.map {
        stringResource(
            when (it) {
                StatusBarHideMode.OFF -> R.string.status_bar_hide_mode_off
                StatusBarHideMode.AUTO -> R.string.status_bar_hide_mode_auto
                StatusBarHideMode.ALWAYS -> R.string.status_bar_hide_mode_always
            }
        )
    }
    val contentAlpha = if (enabled) 1f else 0.38f
    val autoTriggerOptions = autoTriggerOptions()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )

            ExpressiveSegmentedRow(
                options = labels,
                selectedIndex = modes.indexOf(selected),
                onSelect = { onSelect(modes[it]) },
                modifier = Modifier.fillMaxWidth(),
                disabledIndices = if (enabled) emptySet() else modes.indices.toSet(),
            )

            // Auto triggers
            AnimatedVisibility(
                visible = selected == StatusBarHideMode.AUTO,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                OptionSelectionRows(
                    options = autoTriggerOptions,
                    selectedValues = triggers,
                    multiple = true,
                    enabled = enabled,
                    onSelectionChange = {
                        onTriggersChange(if (it in triggers) triggers - it else triggers + it)
                    },
                )
            }

            // Lock-screen switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.status_bar_lockscreen_separate_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    )
                    Text(
                        text = stringResource(R.string.status_bar_lockscreen_separate_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = lockscreen.separate,
                    enabled = enabled,
                    onCheckedChange = { onLockscreenChange(lockscreen.copy(separate = it)) },
                )
            }

            // Lock-screen mode
            AnimatedVisibility(
                visible = lockscreen.separate,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.status_bar_lockscreen_mode_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    )

                    ExpressiveSegmentedRow(
                        options = labels,
                        selectedIndex = modes.indexOf(lockscreen.mode),
                        onSelect = { onLockscreenChange(lockscreen.copy(mode = modes[it])) },
                        modifier = Modifier.fillMaxWidth(),
                        disabledIndices = if (enabled) emptySet() else modes.indices.toSet(),
                    )

                    // Lock-screen auto triggers
                    AnimatedVisibility(
                        visible = lockscreen.mode == StatusBarHideMode.AUTO,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        OptionSelectionRows(
                            options = autoTriggerOptions,
                            selectedValues = lockscreen.triggers,
                            multiple = true,
                            enabled = enabled,
                            onSelectionChange = {
                                val triggers = lockscreen.triggers
                                onLockscreenChange(
                                    lockscreen.copy(triggers = if (it in triggers) triggers - it else triggers + it)
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The cutout kinds "Auto" can react to, in display order, with their localised titles. */
@Composable
private fun autoTriggerOptions(): List<SelectableOption<CutoutKind>> {
    val normal = stringResource(R.string.status_bar_auto_trigger_normal)
    val normalDesc = stringResource(R.string.status_bar_auto_trigger_normal_desc)
    val mini = stringResource(R.string.status_bar_auto_trigger_mini)
    val miniDesc = stringResource(R.string.status_bar_auto_trigger_mini_desc)
    val wide = stringResource(R.string.status_bar_auto_trigger_wide)
    val wideDesc = stringResource(R.string.status_bar_auto_trigger_wide_desc)
    val expanded = stringResource(R.string.status_bar_auto_trigger_expanded)
    val expandedDesc = stringResource(R.string.status_bar_auto_trigger_expanded_desc)
    return remember(normal, mini, wide, expanded, normalDesc, miniDesc, wideDesc, expandedDesc) {
        listOf(
            SelectableOption(CutoutKind.NORMAL, normal, normalDesc),
            SelectableOption(CutoutKind.MINI, mini, miniDesc),
            SelectableOption(CutoutKind.WIDE, wide, wideDesc),
            SelectableOption(CutoutKind.EXPANDED, expanded, expandedDesc),
        )
    }
}

/**
 * A mock status bar showing what the chosen hiding options will actually look like, so the user can
 * see the effect without granting anything first.
 */
@Composable
private fun StatusBarPreview(hideIcons: Boolean, hideSystem: Boolean, hideClock: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape = RoundedCornerShape(24.dp))
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clock
        AnimatedVisibility(visible = !hideClock) {
            Text(
                text = stringResource(R.string.statusbar_preview_time),
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Bold
            )
        }

        // Notification icons
        AnimatedVisibility(visible = !hideIcons) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Square,
                    modifier = Modifier.rotate(45f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "Notification icon"
                )

                Icon(
                    imageVector = Icons.Rounded.Square,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "Notification icon"
                )

                Icon(
                    imageVector = Icons.Rounded.Circle,
                    modifier = Modifier.rotate(45f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "Notification icon"
                )
            }
        }

        // Pushes the system icons to the far end
        Spacer(modifier = Modifier.weight(1f))

        // System icons: wifi, signal, battery
        AnimatedVisibility(visible = !hideSystem) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.NetworkWifi,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "System icon"
                )

                Icon(
                    imageVector = Icons.Rounded.NetworkCell,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "System icon"
                )

                Icon(
                    imageVector = Icons.Rounded.BatterySaver,
                    modifier = Modifier.rotate(90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "System icon"
                )
            }
        }
    }
}

/** The dynamic-coloured "Shizuku isn't ready" card, worded for whichever step is missing. */
@Composable
private fun ShizukuCard(status: ShizukuStatus, onClick: () -> Unit) {
    val title = when (status) {
        ShizukuStatus.NOT_INSTALLED -> R.string.shizuku_not_installed_title
        ShizukuStatus.NOT_RUNNING -> R.string.shizuku_not_running_title
        else -> R.string.shizuku_permission_title
    }
    val subtitle = when (status) {
        ShizukuStatus.NOT_INSTALLED -> R.string.shizuku_not_installed_desc
        ShizukuStatus.NOT_RUNNING -> R.string.shizuku_not_running_desc
        else -> R.string.shizuku_permission_desc
    }

    val recoverable = status == ShizukuStatus.NOT_RUNNING || status == ShizukuStatus.PERMISSION_REQUIRED
    SettingsListItem(
        icon = Icons.Rounded.ErrorOutline,
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        onClick = onClick,
        bgColor = if (recoverable) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        fgColor = if (recoverable) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onErrorContainer,
    )
}
