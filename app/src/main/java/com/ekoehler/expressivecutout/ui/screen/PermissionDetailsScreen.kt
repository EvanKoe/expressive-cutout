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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Wifi
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
 * One permission, explained. [uses] is what the grant is actually needed for and [notUsedFor] is
 * the reassurance half — the things people reasonably fear a permission like this is doing. The
 * copy lives here rather than in strings.xml so a permission is described next to the manifest
 * entry it documents, the same way [Releases] keeps its bullets in code.
 */
private data class PermissionDoc(
    val icon: ImageVector,
    val title: String,
    /** The manifest identifier, shown small and monospaced so it can be matched to the manifest. */
    val manifestName: String,
    val summary: String,
    val uses: List<String>,
    val notUsedFor: List<String>,
    /** Only asked for on some Android versions, or only when a given feature is used. */
    val optional: Boolean = false,
)

/**
 * Every permission the app declares, in the order the user meets them during setup, followed by
 * the two quiet ones (network state and the scoped package query) that never show a prompt.
 */
private val PermissionDocs: List<PermissionDoc> = listOf(
    PermissionDoc(
        icon = Icons.Rounded.Notifications,
        title = "Notification access",
        manifestName = "BIND_NOTIFICATION_LISTENER_SERVICE",
        summary = "The island exists to show your notifications, so it has to be allowed to see " +
            "them. This is the one broad permission the app needs.",
        uses = listOf(
            "Show a notification on the island: its app icon, title, text and action buttons",
            "Let you reply inline, or trigger an action, and pass that straight back to the app that posted it",
            "Read the media session behind a now-playing notification, so the music tile can show the album art and control playback",
            "Detect an ongoing or incoming call, and the system countdown behind a timer notification",
        ),
        notUsedFor = listOf(
            "Storing your notifications — they are held in memory only, for as long as the island shows them, and never written to disk",
            "Sending anything anywhere: the app declares no internet permission, so it physically cannot upload a notification",
            "Reading notifications you have hidden from the island in the settings",
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.Layers,
        title = "Accessibility service",
        manifestName = "BIND_ACCESSIBILITY_SERVICE",
        summary = "Android only lets an accessibility service draw a window that survives above " +
            "other apps and the lockscreen. That window is the island — the service is used as a " +
            "drawing surface, nothing more.",
        uses = listOf(
            "Draw the island over the camera cutout, above whatever app is in the foreground",
            "Keep it there across app switches, and tear it down while the device is locked if you asked for that",
            "Receive your taps and swipes on the island itself",
        ),
        notUsedFor = listOf(
            "Reading the content of the screen: the service requests no window-content events and inspects no view tree",
            "Watching what you type, or which apps you open",
            "Performing actions on your behalf in other apps",
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.BatterySaver,
        title = "Ignore battery optimisation",
        manifestName = "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        summary = "Optional. Without it, aggressive power management can kill the overlay in the " +
            "background, and the island stops appearing until you reopen the app.",
        uses = listOf(
            "Ask the system, once, to leave the overlay service running",
        ),
        notUsedFor = listOf(
            "Background work of any kind — there is no sync, no polling and no scheduled job. The app is idle unless something is on the island",
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.NotificationsActive,
        title = "Post notifications",
        manifestName = "POST_NOTIFICATIONS",
        summary = "Optional, and only asked for on Android 13 and later, the first time you tap " +
            "one of the test buttons on the Permissions screen.",
        uses = listOf(
            "Post the sample notification, call and reply used to preview the island without waiting for a real one",
        ),
        notUsedFor = listOf(
            "Sending you anything else — the app never posts a notification you did not ask for. No ads, no promotions, no update nags",
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.Wifi,
        title = "Network state",
        manifestName = "ACCESS_NETWORK_STATE",
        summary = "Read-only, granted automatically, and never prompted for. It reports whether " +
            "you are connected and the name of the current Wi-Fi network.",
        uses = listOf(
            "Show the Wi-Fi connect and disconnect event on the island, with the network name",
        ),
        notUsedFor = listOf(
            "Using the network. This permission only reads connection state — it grants no internet access, and the app has no internet permission",
            "Location: it cannot see nearby networks, only the one you are already connected to",
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.Apps,
        title = "Installed launcher apps",
        manifestName = "<queries> — launcher intent",
        summary = "Not a permission you grant, but worth naming: to draw an app's icon on the " +
            "island the app has to resolve it. It asks the system only for apps with a launcher " +
            "entry, rather than taking the blanket \"see all apps\" permission.",
        uses = listOf(
            "Resolve the icon and name of an app that posted a notification, or one you picked yourself",
        ),
        notUsedFor = listOf(
            "Building a list of what you have installed. QUERY_ALL_PACKAGES was deliberately removed in favour of this narrower declaration",
        ),
    ),
)

/**
 * "Permission details": one card per declared permission, each split into what the grant is used
 * for and what it is not. Reached from the permissions card in Profile.
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

        PermissionDocs.forEach { doc -> PermissionDocCard(doc) }

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

            Text(
                text = doc.manifestName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = doc.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            BulletGroup(
                icon = Icons.Rounded.Check,
                label = stringResource(R.string.permission_details_uses),
                accent = MaterialTheme.colorScheme.primary,
                items = doc.uses,
            )
            BulletGroup(
                icon = Icons.Rounded.Block,
                label = stringResource(R.string.permission_details_not),
                accent = MaterialTheme.colorScheme.tertiary,
                items = doc.notUsedFor,
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
