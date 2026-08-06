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
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Vibration
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
 * One permission, explained: what it is, and the specific things the app does with it. The copy
 * lives here rather than in strings.xml so a permission is described next to the manifest entry it
 * documents, the same way [Releases] keeps its bullets in code.
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
 * the quiet one (network state) that is granted automatically and never prompts. This list is the
 * whole manifest — if a permission is added there, it belongs here too.
 */
private val PermissionDocs: List<PermissionDoc> = listOf(
    PermissionDoc(
        icon = Icons.Rounded.Notifications,
        title = "Notification access",
        manifestName = "BIND_NOTIFICATION_LISTENER_SERVICE",
        summary = "The island exists to show your notifications, so it has to be allowed to see " +
            "them. This is the one broad permission the app needs. What it reads is held in " +
            "memory for as long as the island shows it, and never written to disk.",
        uses = listOf(
            "Show a notification on the island: the icon the app put on it, its title, text and action buttons",
            "Let you reply inline, or trigger an action, and pass that straight back to the app that posted it",
            "Dismiss the real notification when you swipe the island away",
            "Detect an ongoing or incoming call, and the system countdown behind a timer notification",
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.Vibration,
        title = "Vibration permission",
        manifestName = "VIBRATE",
        summary = "Used to produce vibrations and haptic feedback through the app and the cutout",
        uses = listOf(
            "Vibrate when going through the app",
            "Providing haptic feedback when clicking on the cutout"
        )
    ),
    PermissionDoc(
        icon = Icons.Rounded.Layers,
        title = "Accessibility service",
        manifestName = "BIND_ACCESSIBILITY_SERVICE",
        summary = "Android only lets an accessibility service draw a window that survives above " +
            "other apps and the lockscreen. That window is the island — the service is used as a " +
            "drawing surface. It reads no screen content: the one event it listens for tells it " +
            "only the name of the app in front, never anything shown on screen.",
        uses = listOf(
            "Draw the island over the camera cutout, above whatever app is in the foreground",
            "Keep it there across app switches, and tear it down while the device is locked if you asked for that",
            "Receive your taps and swipes on the island itself",
            "Read the name of the app in the foreground — its package name only, no screen content — " +
                "so the music tile can hide itself while the app playing the music is open",
        ),
    ),
    PermissionDoc(
        icon = Icons.Rounded.BatterySaver,
        title = "Ignore battery optimisation",
        manifestName = "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        summary = "Optional. Without it, aggressive power management can kill the overlay in the " +
            "background, and the island stops appearing until you reopen the app. There is no " +
            "sync, no polling and no scheduled job behind it — the app idles until something " +
            "reaches the island.",
        uses = listOf(
            "Ask the system, once, to leave the overlay service running",
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.NotificationsActive,
        title = "Post notifications",
        manifestName = "POST_NOTIFICATIONS",
        summary = "Optional, and only asked for on Android 13 and later, the first time you tap " +
            "one of the test buttons on the Permissions screen. The app posts nothing you did " +
            "not ask for.",
        uses = listOf(
            "Post the sample notification, call and reply used to preview the island without waiting for a real one",
        ),
        optional = true,
    ),
    PermissionDoc(
        icon = Icons.Rounded.Wifi,
        title = "Network state",
        manifestName = "ACCESS_NETWORK_STATE",
        summary = "Read-only, granted automatically, and never prompted for. It reports whether " +
            "you are connected and the name of the network you are on — it grants no internet " +
            "access, and cannot see networks you are not connected to.",
        uses = listOf(
            "Show the Wi-Fi connect and disconnect event on the island, with the network name",
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
