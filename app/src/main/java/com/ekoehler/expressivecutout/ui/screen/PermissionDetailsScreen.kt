package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R

/**
 * One permission, explained: what it is, and the specific things the app does with it. The copy
 * lives in resources while the manifest identifiers remain untranslated.
 */
private data class PermissionDoc(
    val icon: ImageVector,
    val title: String,
    /** The manifest identifier, shown small and monospaced so it can be matched to the manifest. */
    val manifestName: String,
    val summary: String,
    val uses: List<String>,
    /** Only asked for on some Android versions, or only when a given feature is used. */
    val optional: Boolean = false,
)

/**
 * Every permission the app declares, in the order the user meets them during setup, ending with
 * the quiet ones — network state and package visibility — that are granted automatically and never
 * prompt. This list is the whole manifest — if an entry is added there, it belongs here too.
 */
@Composable
private fun permissionDocs(): List<PermissionDoc> = listOf(
    PermissionDoc(
        icon = Icons.Rounded.Notifications,
        title = stringResource(R.string.permission_doc_notification_title),
        manifestName = "BIND_NOTIFICATION_LISTENER_SERVICE",
        summary = stringResource(R.string.permission_doc_notification_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_notification_show),
            stringResource(R.string.permission_doc_notification_reply),
            stringResource(R.string.permission_doc_notification_dismiss),
            stringResource(R.string.permission_doc_notification_call_timer),
            stringResource(R.string.permission_doc_notification_music),
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.Vibration,
        title = stringResource(R.string.permission_doc_vibration_title),
        manifestName = "VIBRATE",
        summary = stringResource(R.string.permission_doc_vibration_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_vibration_navigation),
            stringResource(R.string.permission_doc_vibration_cutout)
        )
    ),
    PermissionDoc(
        icon = Icons.Rounded.Layers,
        title = stringResource(R.string.permission_doc_accessibility_title),
        manifestName = "BIND_ACCESSIBILITY_SERVICE",
        summary = stringResource(R.string.permission_doc_accessibility_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_accessibility_draw),
            stringResource(R.string.permission_doc_accessibility_lifecycle),
            stringResource(R.string.permission_doc_accessibility_gestures),
            stringResource(R.string.permission_doc_accessibility_foreground),
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.BatterySaver,
        title = stringResource(R.string.permission_doc_battery_title),
        manifestName = "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        summary = stringResource(R.string.permission_doc_battery_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_battery_running),
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.NotificationsActive,
        title = stringResource(R.string.permission_doc_post_notifications_title),
        manifestName = "POST_NOTIFICATIONS",
        summary = stringResource(R.string.permission_doc_post_notifications_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_post_notifications_samples),
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.VolumeUp,
        title = stringResource(R.string.permission_doc_audio_title),
        manifestName = "MODIFY_AUDIO_SETTINGS",
        summary = stringResource(R.string.permission_doc_audio_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_audio_mute),
            stringResource(R.string.permission_doc_audio_speaker),
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.Call,
        title = stringResource(R.string.permission_doc_phone_title),
        manifestName = "READ_PHONE_STATE",
        summary = stringResource(R.string.permission_doc_phone_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_phone_end),
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.Terminal,
        title = stringResource(R.string.permission_doc_shizuku_title),
        manifestName = "moe.shizuku.manager.permission.API_V23",
        summary = stringResource(R.string.permission_doc_shizuku_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_shizuku_status_bar),
            stringResource(R.string.permission_doc_shizuku_privacy),
            stringResource(R.string.permission_doc_shizuku_hotspot),
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.Wifi,
        title = stringResource(R.string.permission_doc_network_title),
        manifestName = "ACCESS_NETWORK_STATE",
        summary = stringResource(R.string.permission_doc_network_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_network_events),
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.WifiTethering,
        title = stringResource(R.string.permission_doc_wifi_title),
        manifestName = "ACCESS_WIFI_STATE",
        summary = stringResource(R.string.permission_doc_wifi_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_wifi_events),
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.Apps,
        title = stringResource(R.string.permission_doc_app_list_title),
        manifestName = "<queries>",
        summary = stringResource(R.string.permission_doc_app_list_summary),
        uses = listOf(
            stringResource(R.string.permission_doc_app_list_filter),
            stringResource(R.string.permission_doc_app_list_icon),
            stringResource(R.string.permission_doc_app_list_shizuku),
        ),
    ),
)

/**
 * "Permission details": one card per declared permission — what it is, then the specific things the
 * app does with it. Reached from the permissions card in Profile.
 */
@Composable
fun PermissionDetailsScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IntroCard()

        permissionDocs().forEach { doc -> PermissionDocCard(doc) }

        FooterCard()
    }
}

/** The promise the rest of the screen backs up: on-device only, no network, no tracking. */
@Composable
private fun IntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.permission_details_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * One permission explained at length: what it is for, and what the app deliberately does not do
 * with it.
 */
@Composable
private fun PermissionDocCard(doc: PermissionDoc) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Title row: icon, name, and the optional badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = doc.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (doc.optional) {
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.permission_details_optional),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // The manifest permission itself
            Text(
                text = doc.manifestName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            // Plain-language summary
            Text(
                text = doc.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            // What the app uses it for
            BulletGroup(
                icon = Icons.Rounded.Check,
                label = stringResource(R.string.permission_details_uses),
                accent = MaterialTheme.colorScheme.primary,
                items = doc.uses,
            )
        }
    }
}

/** One labelled group of bullets, matching the changelog's release groups. */
@Composable
private fun BulletGroup(
    icon: ImageVector,
    label: String,
    accent: Color,
    items: List<String>,
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
            )
        }
        items.forEach { item ->
            Row {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(5.dp)
                        .background(accent, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Closes the screen by pointing at the source, so none of the above has to be taken on trust. */
@Composable
private fun FooterCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.permission_details_footer_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.permission_details_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
