package com.ekoehler.expressivecutout.ui.screen

import android.Manifest
import android.graphics.Paint
import android.os.Build
import android.text.Layout
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
import androidx.compose.material.icons.automirrored.rounded.PhoneCallback
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.notifications.TestCaller
import com.ekoehler.expressivecutout.notifications.TestNotifier
import com.ekoehler.expressivecutout.permissions.Permissions

/**
 * "Permissions" destination: surfaces the notification, overlay (accessibility) and
 * battery-optimisation grants, re-reading live status on every resume so returning from a
 * system settings screen instantly reflects the change. Also offers a test notification.
 */
@Composable
fun PermissionsTab(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val status = rememberPermissionStatus()

    // Android 13+ gates posting behind a runtime permission; grant then run the pending post.
    var pendingPost by remember { mutableStateOf<(() -> Unit)?>(null) }
    val postPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) pendingPost?.invoke() }

    fun postWithPermission(send: () -> Unit) {
        if (TestNotifier.canPost(context)) {
            send()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingPost = send
            postPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun onTestNotification() = postWithPermission { TestNotifier.send(context) }

    fun onTestMultilineNotification() = postWithPermission { TestNotifier.sendMultiline(context) }

    fun onTestProgressNotification() = postWithPermission { TestNotifier.sendProgress(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        }

        Text(
            text = stringResource(R.string.perm_testing_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Send a test notification
            TestCard(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.action_send_test),
                onClick = ::onTestNotification,
            )

            // Send a multi-line test notification with action buttons
            TestCard(
                icon = Icons.AutoMirrored.Rounded.Subject,
                title = stringResource(R.string.action_send_test_multiline),
                onClick = ::onTestMultilineNotification,
            )

            // Send a test progress notification
            TestCard(
                icon = Icons.Rounded.Downloading,
                title = stringResource(R.string.action_send_test_progress),
                onClick = ::onTestProgressNotification,
            )

            // Test a running call
            TestCard(
                icon = Icons.Rounded.Call,
                title = stringResource(R.string.action_send_test_call),
                onClick = { TestCaller.toggle(context, TestCaller.Kind.CONNECTED) },
            )

            // Test an incoming call
            TestCard(
                icon = Icons.AutoMirrored.Rounded.PhoneCallback,
                title = stringResource(R.string.action_send_test_incoming_call),
                onClick = { TestCaller.toggle(context, TestCaller.Kind.INCOMING) },
            )

            // Test Wi-Fi connected (Green dot)
            TestCard(
                icon = Icons.Rounded.Wifi,
                title = stringResource(R.string.action_send_test_wifi_connected),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.WIFI_CONNECTED) },
            )

            // Test Wi-Fi disconnected (Red dot)
            TestCard(
                icon = Icons.Rounded.WifiOff,
                title = stringResource(R.string.action_send_test_wifi_disconnected),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.WIFI_DISCONNECTED) },
            )

            // Test Battery low (Yellow percentage)
            TestCard(
                icon = Icons.Rounded.BatteryAlert,
                title = stringResource(R.string.action_send_test_battery_low),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.BATTERY_LOW, 15) },
            )

            // Test Device locked (Yellow dot)
            TestCard(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.action_send_test_device_locked),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.DEVICE_LOCKED) },
            )

            // Test Charging started (Green percentage)
            TestCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = stringResource(R.string.action_send_test_charging_started),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.CHARGING_STARTED, 85) },
            )

            // Test Unplugged (Red dot)
            TestCard(
                icon = Icons.Rounded.PowerOff,
                title = stringResource(R.string.action_send_test_charging_stopped),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.CHARGING_STOPPED) },
            )

            // Test Headphones out (Red dot)
            TestCard(
                icon = Icons.Rounded.HeadsetOff,
                title = stringResource(R.string.action_send_test_headphones_disconnected),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.HEADPHONES_DISCONNECTED) },
            )

            // Test USB removed (Red dot)
            TestCard(
                icon = Icons.Rounded.Usb,
                title = stringResource(R.string.action_send_test_usb_unmounted),
                onClick = { TestNotifier.sendSystemEvent(com.ekoehler.expressivecutout.core.SystemEventType.USB_UNMOUNTED) },
            )
        }
    }
}

@Composable
private fun TestCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
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

private data class PermissionStatus(
    val notifications: Boolean,
    val accessibility: Boolean,
    val batteryIgnored: Boolean,
) {
    // Battery optimisation is a reliability nicety, not a hard requirement.
    val allEssentialGranted: Boolean get() = notifications && accessibility
}

/** Reads permission state now and again on every [Lifecycle.Event.ON_RESUME]. */
@Composable
private fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun read() = PermissionStatus(
        notifications = Permissions.isNotificationAccessGranted(context),
        accessibility = Permissions.isAccessibilityGranted(context),
        batteryIgnored = Permissions.isBatteryOptimizationIgnored(context),
    )

    var status by remember { mutableStateOf(read()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = read()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return status
}
