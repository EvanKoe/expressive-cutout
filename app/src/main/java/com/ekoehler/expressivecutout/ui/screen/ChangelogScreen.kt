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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R

/**
 * One entry in the release history. The bullet text is kept here rather than in strings.xml so a
 * release can be described in one place when it is cut; only the section headers are localised.
 */
internal data class Release(
    val version: String,
    val headline: String,
    val major: List<String> = emptyList(),
    val minor: List<String> = emptyList(),
    val fixes: List<String> = emptyList(),
)

/** The release history, newest first — the first entry is treated as the current release. */
internal val Releases: List<Release> = listOf(
    Release(
        version = "1.0.0",
        headline = "Incoming calls and expressive animations",
        major = listOf(
            "Incoming-call tile with the caller's name, their number, and decline / answer buttons, opening the in-call view on tap",
            "Animations screen: choose an expressive spring or ease-in-out style and tune speed and bounce against a live example",
        ),
        minor = listOf(
            "Opt-in two-row incoming-call layout, with the number ellipsized under the name so it clears the camera",
            "The expanded incoming layout now matches the expanded cutout size and uses a single bottom-aligned label",
            "Incoming tile pinned to 80% of the screen width, with its own test-call button",
            "Call cutout width adapts to the length of the caller's name",
            "The in-app test call simulates a real incoming call",
            "\"Buy me a coffee\" card in Profile",
        ),
    ),
    Release(
        version = "0.8.0",
        headline = "Per-event customisation",
        major = listOf(
            "Material icon selector for events, replacing the app-icon picker, with a search filter over the full icon library",
        ),
        minor = listOf(
            "Per-event duration settings",
            "Animated-icon and loop toggles for each event",
            "Per-event colour override with a reset",
            "The charging event now animates",
        ),
    ),
    Release(
        version = "0.7.0",
        headline = "Timer tile",
        major = listOf(
            "Timer dynamic tile that mirrors the system countdown",
        ),
        minor = listOf(
            "Behaviour: slider to scale the island animation duration between 0 and 1000 ms",
            "Appearance: stroke options animate in and out",
            "Icon container colour for the phone and timer tiles, previewed in the tile list",
        ),
        fixes = listOf(
            "Detect Android 16+ live-update timers, so Google Clock countdowns are picked up",
            "Show the clock app's real button labels instead of substituted text",
            "Pause flips to Resume live while the timer is paused",
            "The timer pill dismisses immediately when the timer is reset",
            "The settings list refreshes the accessibility grant on resume",
        ),
    ),
    Release(
        version = "0.6.0",
        headline = "Phone tile and lockscreen behaviour",
        major = listOf(
            "Phone dynamic tile: live call with contact photo, call duration, and call actions",
            "Hide on lockscreen — the overlay tears itself down while the device is locked",
        ),
        minor = listOf(
            "Predictive back gesture with a matching navigation animation",
            "Unlock animation",
            "Separate hang-up and secondary button colours on the phone tile",
            "Event icons renamed to Events, with a dynamic primary-colour toggle for every event",
            "Events can pick a Material You colour role and an opacity",
            "Appearance and disappearance animation for the normal cutout",
            "Overlay access card in the main settings list, and a reordered permissions screen",
            "QUERY_ALL_PACKAGES replaced with a scoped queries declaration for launcher apps",
        ),
    ),
    Release(
        version = "0.5.0",
        headline = "Dynamic tiles and music playback",
        major = listOf(
            "Dynamic tiles, starting with a now-playing music tile: album art, playback controls, and per-tile settings screens",
        ),
        minor = listOf(
            "Album art spins while playback runs and freezes when paused",
            "The music cutout stays pinned up for as long as playback is live",
            "Customisable music tile buttons: colour, opacity, rounded corners, and shape presets",
            "Swipe to dismiss, with its own settings",
            "One shared colour picker with dynamic roles, hex entry, and overridable presets",
        ),
        fixes = listOf(
            "Dynamic tiles split away from system events into their own model, prefs, and screen",
            "The music tile no longer reappears after its notification is dismissed",
            "More material play / pause button",
        ),
    ),
    Release(
        version = "0.4.0",
        headline = "Backgrounds and swipe gestures",
        major = listOf(
            "Dedicated background screen: separate normal and expanded fills, gradients, and opacity",
        ),
        minor = listOf(
            "Shrink the expanded cutout by swiping up, toggleable in Behaviour",
            "Segmented reply field — cancel, input, and send as one connected bar",
            "Card in Profile that opens the GitHub repository",
        ),
        fixes = listOf(
            "Touches around the cutout reach the notification shade without resizing the window",
        ),
    ),
    Release(
        version = "0.3.0",
        headline = "Notifications and inline replies",
        major = listOf(
            "Notification actions and inline reply, plus the Wi-Fi network name on connect",
        ),
        minor = listOf(
            "Style options for the action buttons and the reply field",
            "Customisable send / cancel reply button colours",
            "Test notification with action buttons, inline reply, and a 15-second auto-dismiss",
        ),
        fixes = listOf(
            "The preview reflects the action-button toggle, and swatch selection rings are no longer clipped",
            "Back from Action buttons returns to Appearance, then to Settings",
            "Removed the feature that required the background location permission",
        ),
    ),
    Release(
        version = "0.2.0",
        headline = "Size, position and behaviour",
        major = listOf(
            "Size and position controls for the island, alongside the first behaviour options",
        ),
        minor = listOf(
            "Global switch to turn the cutout off entirely",
        ),
        fixes = listOf(
            "Smoother animation, and a window-resize delay that stops the island from jumping",
        ),
    ),
    Release(
        version = "0.1.0",
        headline = "First release",
        major = listOf(
            "Expressive Cutout: a dynamic island overlay for punch-hole displays",
        ),
    ),
)

/**
 * "What's new": the full release history, newest first, with each release split into major
 * features, minor features and bug fixes. Reached from the version card in Profile.
 */
@Composable
fun ChangelogScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.changelog_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Releases.forEachIndexed { index, release ->
            ReleaseCard(release = release, isCurrent = index == 0)
        }
    }
}

@Composable
private fun ReleaseCard(release: Release, isCurrent: Boolean) {
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
                Text(
                    text = release.version,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (isCurrent) {
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_current),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(
                text = release.headline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReleaseGroup(
                icon = Icons.Rounded.AutoAwesome,
                label = stringResource(R.string.changelog_major),
                accent = MaterialTheme.colorScheme.primary,
                items = release.major,
            )
            ReleaseGroup(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.changelog_minor),
                accent = MaterialTheme.colorScheme.tertiary,
                items = release.minor,
            )
            ReleaseGroup(
                icon = Icons.Rounded.BugReport,
                label = stringResource(R.string.changelog_fixes),
                accent = MaterialTheme.colorScheme.secondary,
                items = release.fixes,
            )
        }
    }
}

/** One labelled group of bullets, or nothing at all when the release had none of that kind. */
@Composable
private fun ReleaseGroup(
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
