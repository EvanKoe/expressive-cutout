package com.ekoehler.expressivecutout.ui.screen

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.PermissionDotPosition
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import java.nio.file.WatchEvent

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
) {
    val context = LocalContext.current
    val hideIcons by viewModel.hideNotificationIcons.collectAsStateWithLifecycle()
    val hideSystemInfo by viewModel.hideSystemInfo.collectAsStateWithLifecycle()
    val hideClock by viewModel.hideClock.collectAsStateWithLifecycle()
    val silenceAlerts by viewModel.silenceSystemAlerts.collectAsStateWithLifecycle()
    val permissionDot by viewModel.permissionDotEnabled.collectAsStateWithLifecycle()
    val permissionDotPosition by viewModel.permissionDotPosition.collectAsStateWithLifecycle()
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

        StatusBarPreview(hideIcons = hideIcons, hideSystem = hideSystemInfo, hideClock = hideClock)

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.status_bar_hide_icons_title),
            description = stringResource(R.string.status_bar_hide_icons_desc),
            checked = ready && hideIcons,
            onCheckedChange = viewModel::setHideNotificationIcons,
            enabled = ready,
        )

        AnimatedVisibility(visible = ready && hideIcons) {
            Text(
                text = stringResource(R.string.status_bar_hide_icons_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.status_bar_hide_system_info_title),
            description = stringResource(R.string.status_bar_hide_system_info_desc),
            checked = ready && hideSystemInfo,
            onCheckedChange = viewModel::setHideSystemInfo,
            enabled = ready,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.status_bar_hide_clock_title),
            description = stringResource(R.string.status_bar_hide_clock_desc),
            checked = ready && hideClock,
            onCheckedChange = viewModel::setHideClock,
            enabled = ready,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.status_bar_silence_alerts_title),
            description = stringResource(R.string.status_bar_silence_alerts_desc),
            checked = ready && silenceAlerts,
            onCheckedChange = viewModel::setSilenceSystemAlerts,
            enabled = ready,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.permission_dot_title),
            description = stringResource(R.string.permission_dot_desc),
            checked = ready && permissionDot,
            onCheckedChange = viewModel::setPermissionDotEnabled,
            enabled = ready,
        )

        AnimatedVisibility(
            visible = ready && permissionDot,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        ) {
            PermissionDotPositionCard(
                selected = permissionDotPosition,
                onSelect = viewModel::setPermissionDotPosition,
            )
        }
    }
}

/**
 * The "Position" selector that appears under the permission-dot switch: which end of the collapsed
 * pill the dots sit on. [PermissionDotPosition]'s declaration order is the option order, so the two
 * can't drift apart.
 */
@Composable
private fun PermissionDotPositionCard(
    selected: PermissionDotPosition,
    onSelect: (PermissionDotPosition) -> Unit,
) {
    val options = PermissionDotPosition.entries
    val labels = options.map {
        stringResource(
            when (it) {
                PermissionDotPosition.LEFT -> R.string.permission_dot_position_left
                PermissionDotPosition.RIGHT -> R.string.permission_dot_position_right
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.permission_dot_position_title),
                style = MaterialTheme.typography.titleMedium,
            )
            ExpressiveSegmentedRow(
                options = labels,
                selectedIndex = options.indexOf(selected),
                onSelect = { onSelect(options[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
