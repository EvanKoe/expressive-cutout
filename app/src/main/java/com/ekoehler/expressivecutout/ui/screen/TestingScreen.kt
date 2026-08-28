package com.ekoehler.expressivecutout.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneCallback
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.notifications.TestCaller
import com.ekoehler.expressivecutout.notifications.TestNotifier

/**
 * "Testing triggers" destination: fires each kind of island content on demand, so the current
 * layout and animation settings can be seen without waiting for a real notification or call.
 */
@Composable
fun TestingScreen(contentPadding: PaddingValues) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_testing_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
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
                onClick = { postWithPermission { TestNotifier.send(context) } },
            )

            TestCard(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.action_send_test_multiline),
                onClick = { postWithPermission { TestNotifier.sendMultiline(context) } },
            )

            TestCard(
                icon = Icons.Rounded.NotificationsNone,
                title = stringResource(R.string.action_send_test_plain),
                onClick = { postWithPermission { TestNotifier.sendPlain(context) } },
            )

            TestCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.action_send_test_double),
                onClick = { postWithPermission { TestNotifier.sendPair(context) } },
            )

            TestCard(
                icon = Icons.Rounded.Downloading,
                title = stringResource(R.string.action_send_test_progress),
                onClick = { postWithPermission { TestNotifier.sendProgress(context) } },
            )

            TestCard(
                icon = Icons.Rounded.Call,
                title = stringResource(R.string.action_send_test_call),
                onClick = { TestCaller.toggle(context, TestCaller.Kind.CONNECTED) },
            )

            TestCard(
                icon = Icons.Rounded.PhoneCallback,
                title = stringResource(R.string.action_send_test_incoming_call),
                onClick = { TestCaller.toggle(context, TestCaller.Kind.INCOMING) },
            )
        }
    }
}

/** One trigger in the list: an icon, its label, and the action it fires on tap. */
@Composable
private fun TestCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        onClick = onClick,
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
