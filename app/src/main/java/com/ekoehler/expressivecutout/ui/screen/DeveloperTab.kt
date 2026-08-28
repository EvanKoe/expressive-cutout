package com.ekoehler.expressivecutout.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.PhoneCallback
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.notifications.TestCaller
import com.ekoehler.expressivecutout.notifications.TestNotifier

/**
 * "Developer Testing" destination: houses triggers to test notifications, calls, and progress events.
 */
@Composable
fun DeveloperTab(contentPadding: PaddingValues) {
    val context = LocalContext.current

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
        Text(
            text = stringResource(R.string.profile_testing_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TestCard(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.action_send_test),
                onClick = ::onTestNotification,
            )
            TestCard(
                icon = Icons.AutoMirrored.Rounded.Subject,
                title = stringResource(R.string.action_send_test_multiline),
                onClick = ::onTestMultilineNotification,
            )
            TestCard(
                icon = Icons.Rounded.Downloading,
                title = stringResource(R.string.action_send_test_progress),
                onClick = ::onTestProgressNotification,
            )
            TestCard(
                icon = Icons.Rounded.Call,
                title = stringResource(R.string.action_send_test_call),
                onClick = { TestCaller.toggle(context, TestCaller.Kind.CONNECTED) },
            )
            TestCard(
                icon = Icons.AutoMirrored.Rounded.PhoneCallback,
                title = stringResource(R.string.action_send_test_incoming_call),
                onClick = { TestCaller.toggle(context, TestCaller.Kind.INCOMING) },
            )
        }
    }
}

@Composable
private fun TestCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
