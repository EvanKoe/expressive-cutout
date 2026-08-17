package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import com.ekoehler.expressivecutout.ui.AppViewModel

/**
 * "Status bar" screen (reached from Appearance). Hides the system status bar's notification icons
 * so the island isn't reporting the same notification twice.
 *
 * The change needs shell privileges we can't hold, so everything here is gated on Shizuku being
 * reachable — surfaced as a dynamic-coloured card at the top, matching how the settings list flags a
 * missing accessibility grant. Shizuku stops on every reboot, so that card is a normal sight rather
 * than a one-time setup step.
 */
@Composable
internal fun StatusBarScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val hideIcons by viewModel.hideNotificationIcons.collectAsStateWithLifecycle()
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
    // Installed-but-stopped is the recoverable everyday case (it dies on reboot), so it gets the
    // softer primary container; a missing install or refused grant is the harder stop.
    val recoverable = status == ShizukuStatus.NOT_RUNNING
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
