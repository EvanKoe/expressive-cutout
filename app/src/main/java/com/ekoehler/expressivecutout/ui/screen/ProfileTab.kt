package com.ekoehler.expressivecutout.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.AppLanguage
import com.ekoehler.expressivecutout.data.AppLanguages
import com.ekoehler.expressivecutout.system.AppLocale
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.pageTransition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.theme.AppTheme

/** The screens reachable from the Profile tab. Hoisted to MainScreen, like [SettingsRoute]. */
enum class ProfileRoute { List, Changelog, PermissionDetails, Testing }

/**
 * Takes a hex color as a Long (0xARGB)
 */
private fun getButtonBgColor(bgColor: Long, fgColor: Long = 0xFFFF): ButtonColors {
    return ButtonColors(
        containerColor = Color(bgColor),
        disabledContentColor = Color.Transparent,
        contentColor = Color(fgColor),
        disabledContainerColor = Color.Gray
    )
}

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
    onOpenPermissionDetails: () -> Unit,
    onOpenTesting: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val dir = if (targetState != ProfileRoute.List) 1 else -1
            pageTransition(appearance.pageTransitionStyle, dir)
        },
        label = "profileRoute",
    ) { current ->
        when (current) {
            ProfileRoute.List -> ProfileList(
                viewModel = viewModel,
                contentPadding = contentPadding,
                onOpenChangelog = onOpenChangelog,
                onOpenPermissionDetails = onOpenPermissionDetails,
                onOpenTesting = onOpenTesting,
                onExportSettings = onExportSettings,
                onImportSettings = onImportSettings,
            )
            ProfileRoute.Changelog -> ChangelogScreen(contentPadding)
            ProfileRoute.PermissionDetails -> PermissionDetailsScreen(contentPadding)
            ProfileRoute.Testing -> TestingScreen(contentPadding)
        }
    }
}

@Composable
private fun ProfileList(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenChangelog: () -> Unit,
    onOpenPermissionDetails: () -> Unit,
    onOpenTesting: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    val context = LocalContext.current
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val languages = remember(context) { AppLanguages.supported(context) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    val openUrl = { url: String ->
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppHeader()

        Spacer(Modifier.height(24.dp))

        // URLs
        val githubProjectUrl = stringResource(R.string.profile_github_project_url)
        val coffeeUrl = stringResource(R.string.profile_coffee_url)
        val linkedInUrl = stringResource(R.string.profile_linkedin_url)
        val edroidUrl = stringResource(R.string.edroid_github_url)
        val redditUrl = stringResource(R.string.profile_reddit_url)

        // Theme switcher
        ThemeCard(
            selected = theme,
            onSelect = viewModel::setTheme,
            shape = groupedShape(isFirst = true)
        )

        // Language dropdown
        LanguageCard(
            languages = languages,
            selectedTag = language,
            onSelect = { tag ->
                if (tag != language) {
                    viewModel.setLanguage(tag)
                    if (!AppLocale.appliedBySystem) (context as? Activity)?.recreate()
                }
            },
            shape = groupedShape()
        )

        // Testing triggers
        TestingCard(shape = groupedShape(), onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenTesting()
        })

        // Import/export settings
        ExportSettingsCard(groupedShape(isLast = true), onExportSettings, onImportSettings)

        Spacer(modifier = Modifier.height(12.dp))

        // Developer card
        DevCard(
            shape = groupedShape(isFirst = true),
            onOpenGitHub = { openUrl(githubProjectUrl) },
            onOpenCoffee = { openUrl(coffeeUrl) },
            onOpenLinkedIn = { openUrl(linkedInUrl) },
            onOpenReddit = { openUrl(redditUrl) }
        )

        // E-Droid card
        EDroidProjectCard(
            onClick = { openUrl(edroidUrl) },
            shape = groupedShape(isLast = true)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Version
        VersionCard(versionName = versionName, shape = groupedShape(isFirst = true), onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenChangelog()
        })

        // Permission detail screen
        PermissionDetailsCard(shape = groupedShape(isLast = true), onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenPermissionDetails()
        })
    }
}

/**
 * The app's identity at the top of the tab: the launcher icon, the app name and the tagline.
 * The icon is drawn from the monochrome launcher layer so it takes its colours from the theme —
 * a primary pill on a primaryContainer disc, with the sparkles knocked through to the disc.
 */
@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = stringResource(R.string.app_icon_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * The "about" card that closes the tab: the developer's photo and the two links out as filled
 * buttons. Name, version and tagline live in [AppHeader] instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DevCard(
    shape: Shape,
    onOpenGitHub: () -> Unit,
    onOpenCoffee: () -> Unit,
    onOpenLinkedIn: () -> Unit,
    onOpenReddit: () -> Unit,
) {
    /** Data for rendering social network buttons */
    data class ContactButtonData(
        val name: Int,
        val icon: Int,
        val onClick: () -> Unit,
        val bgColor: Long,
        val fgColor: Long = 0xFFFFFFFF
    )

    /**
     * A single social network button
     */
    @Composable
    fun ContactButton(data: ContactButtonData) {
        Button(
            onClick = data.onClick,
            colors = getButtonBgColor(data.bgColor, data.fgColor)
        ) {
            Icon(
                painter = painterResource(data.icon),
                contentDescription = null,
                tint = Color(data.fgColor),
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = stringResource(data.name),
                color = Color(data.fgColor)
            )
        }
    }

    // All buttons to render
    val buttons: Array<ContactButtonData> = arrayOf(
        ContactButtonData(R.string.dev_card_github, R.drawable.ic_github, onOpenGitHub, 0xFF1E1F22),
        ContactButtonData(R.string.profile_coffee_title, R.drawable.buy_me_coffee_icon, onOpenCoffee, 0xFFFFDD00, 0xFF000000),
        ContactButtonData(R.string.dev_card_linkedin, R.drawable.ic_linkedin, onOpenLinkedIn, 0xFF0A66C2),
        ContactButtonData(R.string.dev_card_reddit, R.drawable.ic_reddit_logo, onOpenReddit, 0xFFF74300),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Dev profile picture
            DevAvatar()

            // Made by Evan
            Text(
                text = stringResource(R.string.dev_card_author),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 20.dp),
            )

            // Social network buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buttons.toList().forEach { ContactButton(it) }
            }
        }
    }
}

/** The developer's photo, cropped square with the same corner radius as the cards. */
@Composable
private fun DevAvatar() {
    Image(
        painter = painterResource(R.drawable.dev_avatar),
        contentDescription = stringResource(R.string.dev_card_avatar),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(28.dp)),
    )
}

/** The app-wide theme choice: a title over the segmented selector, in a card of its own. */
@Composable
private fun ThemeCard(selected: AppTheme, onSelect: (AppTheme) -> Unit, shape: Shape) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
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
                style = MaterialTheme.typography.labelLarge,
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
 * The language the app is shown in. Tapping the card opens [LanguageDialog] over the list; the
 * entries come from res/xml/locales_config.xml, one per shipped values-<tag> folder.
 */
@Composable
private fun LanguageCard(
    languages: List<AppLanguage>,
    selectedTag: String,
    onSelect: (String) -> Unit,
    shape: Shape,
) {
    val haptics = LocalHapticFeedback.current
    var showPicker by remember { mutableStateOf(false) }
    val selected = languages.firstOrNull { it.tag == selectedTag } ?: languages.first()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showPicker = true
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_language),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = selected.displayName,
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

    if (showPicker) {
        LanguageDialog(
            languages = languages,
            selectedTag = selected.tag,
            onSelect = { tag ->
                showPicker = false
                onSelect(tag)
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * The language picker itself: every shipped language as a row, each labelled in its own language so
 * the list still reads for someone who cannot read the language the app is in. Picking a row is the
 * whole interaction, so the dialog closes on the tap and only carries a way out.
 */
@Composable
private fun LanguageDialog(
    languages: List<AppLanguage>,
    selectedTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_language)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                languages.forEachIndexed { index, language ->
                    LanguageRow(
                        language = language,
                        selected = language.tag == selectedTag,
                        shape = groupedShape(
                            isFirst = index == 0,
                            isLast = index == languages.lastIndex,
                        ),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(language.tag)
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** One language in [LanguageDialog], filled with the secondary container while it is the active one. */
@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(
                selected = selected,
                enabled = !selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Grouped-list row shape, matching the option rows the settings screens use. */
private fun languageRowShape(first: Boolean, last: Boolean) = RoundedCornerShape(
    topStart = if (first) 24.dp else 4.dp,
    topEnd = if (first) 24.dp else 4.dp,
    bottomStart = if (last) 24.dp else 4.dp,
    bottomEnd = if (last) 24.dp else 4.dp,
)

/**
 * The installed version, shown large, opening the full changelog on tap. A pre-release suffix
 * ("0.1.0-beta") becomes the trailing badge instead of being spelled out in the big number.
 */
@Composable
private fun VersionCard(versionName: String, shape: Shape, onClick: () -> Unit) {
    val number = versionName.substringBefore('-')
    val preRelease = versionName.contains('-')

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
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

/** A clickable card that opens the per-permission explanations in [PermissionDetailsScreen]. */
@Composable
private fun PermissionDetailsCard(shape: Shape, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_permissions_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.profile_permissions_subtitle),
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

/** A clickable card that opens the on-demand island triggers in [TestingScreen]. */
@Composable
private fun TestingCard(shape: Shape, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Testing icon
            Icon(
                imageVector = Icons.Rounded.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = stringResource(R.string.profile_testing_title),
                    style = MaterialTheme.typography.titleMedium,
                )

                // Description
                Text(
                    text = stringResource(R.string.profile_testing_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Open arrow
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
private fun GitHubCard(shape: Shape, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
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

/** Export and import settings in a card */
@Composable
private fun ExportSettingsCard(
    shape: Shape,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.profile_export_title),
                style = MaterialTheme.typography.titleMedium
            )

            // Description
            Text(
                text = stringResource(R.string.profile_export_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Buttons
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Export button
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onExportSettings,
                ) {
                    Icon(imageVector = Icons.Rounded.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.profile_export_export))
                }

                // Import button
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onImportSettings
                ) {
                    Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.profile_export_import))
                }
            }
        }

    }
}

/**
 * E-Droid self-promotion card
 */
@Composable
private fun EDroidProjectCard(
    shape: RoundedCornerShape = groupedShape(),
    onClick: () -> Unit
) {
    // Colors
    val surface: Color = Color(0xFFEADDFF)
    val onSurface: Color = Color(0xFF1D1B20)
    val onSurfaceVariant: Color = Color(0xFF49454F)
    val primary: Color = Color(0xFF6750A4)
    val onPrimary: Color = Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // E-Droid logo
                Image(
                    painter = painterResource(R.drawable.edroid),
                    contentDescription = stringResource(R.string.edroid_desc),
                    modifier = Modifier.size(36.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                )

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.edroid_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )

                    // Description
                    Text(
                        text = stringResource(R.string.edroid_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }
            }

            // Wavy arrow panel, its outer corners cut by the card's own clip
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(color = primary, shape = RoundedStarShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = onPrimary,
                )
            }
        }
    }
}

/**
 * A rounded star ("cookie") larger than the panel it fills, centred towards the trailing edge.
 * Only its scalloped leading side shows: the rest spills past the panel and is cut off by the
 * card's clip. The outline is a circle whose radius swells and shrinks [POINTS] times around it,
 * so every tip and every dip is equally soft. Mirrored in right-to-left layouts.
 */
private object RoundedStarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = size.width * RADIUS_FRACTION
        val centerX = size.width *
            if (layoutDirection == LayoutDirection.Ltr) CENTER_X_FRACTION else 1f - CENTER_X_FRACTION
        val centerY = size.height / 2f

        val path = Path()
        for (i in 0..SAMPLES) {
            val angle = 2f * PI.toFloat() * i / SAMPLES
            val r = radius * (1f + DEPTH * cos(POINTS * angle))
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    /** Number of tips around the star. */
    private const val POINTS = 10

    /** How far the tips and dips swing from the base circle, as a fraction of the radius. */
    private const val DEPTH = 0.08f

    /** Base circle radius and centre, as fractions of the panel width. */
    private const val RADIUS_FRACTION = 0.6f
    private const val CENTER_X_FRACTION = 0.62f

    /** Line segments used to trace the outline; enough that the curve reads as smooth. */
    private const val SAMPLES = 180
}