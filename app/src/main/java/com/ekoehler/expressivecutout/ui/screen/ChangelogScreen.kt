package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import org.json.JSONArray

/** The asset holding the release history, edited by hand when a release is cut. */
private const val ChangelogAsset = "changelog.json"

/**
 * One entry in the release history. The bullet text lives in [ChangelogAsset] rather than in
 * strings.xml so a release can be described in one place when it is cut; only the section headers
 * are localised.
 */
internal data class Release(
    val version: String,
    val description: String,
    val features: List<String> = emptyList(),
    val bugfixes: List<String> = emptyList(),
)

/**
 * Reads the release history from [ChangelogAsset], newest first — the first entry is taken as the
 * current build. A missing or malformed asset yields an empty history rather than a crash, so a
 * typo while filling the file in only costs the changelog its content.
 */
internal fun loadReleases(context: Context): List<Release> = runCatching {
    val text = context.assets.open(ChangelogAsset).bufferedReader().use { it.readText() }
    val entries = JSONArray(text)
    List(entries.length()) { index ->
        val entry = entries.getJSONObject(index)
        Release(
            version = entry.optString("version"),
            description = entry.optString("description"),
            features = entry.optJSONArray("features").toStringList(),
            bugfixes = entry.optJSONArray("bugfixes").toStringList(),
        )
    }
}.getOrDefault(emptyList())

/** Flattens a bullet array into a list, treating an absent key as no bullets of that kind. */
private fun JSONArray?.toStringList(): List<String> =
    if (this == null) emptyList() else List(length()) { getString(it) }

/**
 * "What's new": the full release history, newest first, with each release split into features and
 * bug fixes. Reached from the version card in Profile.
 */
@Composable
fun ChangelogScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val releases = remember { loadReleases(context) }

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

        releases.forEachIndexed { index, release ->
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
                            text = stringResource(R.string.version_beta),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(
                text = release.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReleaseGroup(
                icon = Icons.Rounded.AutoAwesome,
                label = stringResource(R.string.changelog_features),
                accent = MaterialTheme.colorScheme.primary,
                items = release.features,
            )
            ReleaseGroup(
                icon = Icons.Rounded.BugReport,
                label = stringResource(R.string.changelog_fixes),
                accent = MaterialTheme.colorScheme.secondary,
                items = release.bugfixes,
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
