package com.ekoehler.expressivecutout.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.ekoehler.expressivecutout.ui.components.PageTitle

/**
 * "Permissions" destination: surfaces the notification, overlay (accessibility) and
 * battery-optimisation grants, re-reading live status on every resume so returning from a
 * system settings screen instantly reflects the change.
 */
@Composable
fun PermissionsTab(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var status by rememberPermissionStatus()
    val shizuku by ShizukuState.status.collectAsStateWithLifecycle()

    // Answered in place rather than in system settings, so take the result rather than waiting for
    // the resume rememberPermissionStatus reads on.
    val requestPhoneState = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> status = status.copy(phoneState = granted) }

    // Shizuku can be started while we're backgrounded, and returning here is the natural moment to
    // notice, so re-read on resume alongside the grants rememberPermissionStatus already refreshes.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ShizukuState.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageTitle(text = stringResource(R.string.nav_permissions))

        AnimatedVisibility(visible = status.allEssentialGranted) {
            AllSetCard()
        }

        Column(
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PermissionCard(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.perm_notifications_title),
                description = stringResource(R.string.perm_notifications_desc),
                granted = status.notifications,
                onClick = { Permissions.openNotificationAccessSettings(context) },
            )
            PermissionCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.perm_accessibility_title),
                description = stringResource(R.string.perm_accessibility_desc),
                granted = status.accessibility,
                onClick = { Permissions.openAccessibilitySettings(context) },
            )
            PermissionCard(
                icon = Icons.Rounded.BatterySaver,
                title = stringResource(R.string.perm_battery_title),
                description = stringResource(R.string.perm_battery_desc),
                granted = status.batteryIgnored,
                onClick = { Permissions.requestIgnoreBatteryOptimization(context) },
            )
            // Optional too: without it the phone tile still closes when a call ends, just from the
            // notification panel rather than from the platform's own call state.
            PermissionCard(
                icon = Icons.Rounded.Call,
                title = stringResource(R.string.perm_phone_state_title),
                description = stringResource(R.string.perm_phone_state_desc),
                granted = status.phoneState,
                onClick = { requestPhoneState.launch(Permissions.PHONE_STATE) },
            )
            // Optional, so it's deliberately outside status.allEssentialGranted — a missing Shizuku
            // must never stop the "All set" card from showing. Shizuku dies on every reboot, and
            // this is where people already come to check why something stopped working.
            PermissionCard(
                icon = Icons.Rounded.Terminal,
                title = stringResource(R.string.perm_shizuku_title),
                description = stringResource(R.string.perm_shizuku_desc),
                granted = shizuku == ShizukuStatus.READY,
                onClick = {
                    if (shizuku == ShizukuStatus.PERMISSION_REQUIRED) ShizukuState.requestPermission()
                    else Permissions.openShizuku(context)
                },
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
    isCheckButton: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isCheckButton) {
                Spacer(Modifier.width(12.dp))

                if (granted) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.status_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Button(onClick = onClick) {
                        Text(stringResource(R.string.status_needed))
                    }
                }
            }
        }
    }
}

/**
 * Replaces the permission list once everything is granted, so a fully set-up app doesn't show a
 * wall of ticks.
 */
@Composable
private fun AllSetCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clip(shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.all_set_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.all_set_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * A snapshot of every grant the island needs, re-read on resume because the user can revoke any of
 * them in system settings while the app is open.
 */
private data class PermissionStatus(
    val notifications: Boolean,
    val accessibility: Boolean,
    val batteryIgnored: Boolean,
    val phoneState: Boolean,
) {
    // Battery optimisation and the call state are reliability niceties, not hard requirements.
    val allEssentialGranted: Boolean get() = notifications && accessibility
}

/**
 * Reads permission state now and again on every [Lifecycle.Event.ON_RESUME]. Returned as a mutable
 * state so a runtime prompt answered in place can write its result straight back.
 */
@Composable
private fun rememberPermissionStatus(): MutableState<PermissionStatus> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun read() = PermissionStatus(
        notifications = Permissions.isNotificationAccessGranted(context),
        accessibility = Permissions.isAccessibilityGranted(context),
        batteryIgnored = Permissions.isBatteryOptimizationIgnored(context),
        phoneState = Permissions.isPhoneStateGranted(context),
    )

    val status = remember { mutableStateOf(read()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status.value = read()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return status
}
