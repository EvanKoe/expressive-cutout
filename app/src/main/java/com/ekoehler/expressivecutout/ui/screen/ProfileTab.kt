package com.ekoehler.expressivecutout.ui.screen

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import com.ekoehler.expressivecutout.ui.theme.AppTheme

/** The screens reachable from the Profile tab. Hoisted to MainScreen, like [SettingsRoute]. */
enum class ProfileRoute { List, Changelog }

/**
 * "Profile" destination: the app-wide theme choice, the version (which opens the changelog) and
 * links out to the project. The selected theme is persisted and applied at the root of the activity.
 */
@Composable
fun ProfileTab(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    route: ProfileRoute,
    onOpenChangelog: () -> Unit,
) {
    // Same motion as the Settings tab: deeper routes slide in from the right, back from the left.
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val dir = if (targetState == ProfileRoute.Changelog) 1 else -1
            (slideInHorizontally(tween(300)) { w -> dir * w } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(300)) { w -> -dir * w } + fadeOut(tween(300)))
        },
        label = "profileRoute",
    ) { current ->
        when (current) {
            ProfileRoute.List -> ProfileList(viewModel, contentPadding, onOpenChangelog)
            ProfileRoute.Changelog -> ChangelogScreen(contentPadding)
        }
    }
}

@Composable
private fun ProfileList(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenChangelog: () -> Unit,
) {
    val context = LocalContext.current
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeCard(
            selected = theme,
            onSelect = viewModel::setTheme,
        )

        VersionCard(versionName = versionName, onClick = onOpenChangelog)

        val githubUrl = stringResource(R.string.profile_github_url)
        GitHubCard(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, githubUrl.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )

        val coffeeUrl = stringResource(R.string.profile_coffee_url)
        BuyMeACoffeeCard(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, coffeeUrl.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
    }
}

/** The app-wide theme choice: a title over the segmented selector, in a card of its own. */
@Composable
private fun ThemeCard(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_theme),
                style = MaterialTheme.typography.titleMedium,
            )
            ExpressiveSegmentedRow(
                options = AppTheme.entries.map { stringResource(it.labelRes) },
                selectedIndex = selected.ordinal,
                onSelect = { onSelect(AppTheme.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The installed version, shown large, opening the full changelog on tap. A pre-release suffix
 * ("0.1.0-beta") becomes the trailing badge instead of being spelled out in the big number.
 */
@Composable
private fun VersionCard(versionName: String, onClick: () -> Unit) {
    val number = versionName.substringBefore('-')
    val preRelease = versionName.contains('-')

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_version),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (preRelease) {
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
                    text = stringResource(R.string.profile_version_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A clickable card that opens the project's GitHub repository in the browser. */
@Composable
private fun GitHubCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_github_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.profile_github_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A clickable card that opens the developer's Buy Me a Coffee page in the browser. */
@Composable
private fun BuyMeACoffeeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Coffee,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_coffee_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.profile_coffee_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
