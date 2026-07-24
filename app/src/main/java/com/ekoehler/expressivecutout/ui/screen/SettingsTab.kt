package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.ui.AppViewModel

/**
 * "Settings" destination. A lightweight list that navigates to focused sub-screens — so the
 * heavy live preview (and the pinned overlay) only exist while the "Size & position" screen
 * is open, never on the list or the icons screen.
 *
 * The individual sub-screens live in the SettingScreens/ folder (same package).
 */
@Composable
fun SettingsTab(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    route: SettingsRoute,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenBehaviour: () -> Unit,
    onOpenAppearance: () -> Unit,
) {
    // Routing (and back navigation, via the bottom bar) is owned by MainScreen.
    when (route) {
        SettingsRoute.List -> {
            val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
            SettingsList(
                contentPadding = contentPadding,
                cutoutEnabled = behaviour.cutoutEnabled,
                onCutoutEnabledChange = viewModel::setCutoutEnabled,
                onOpenSizePosition = onOpenSizePosition,
                onOpenEventIcons = onOpenEventIcons,
                onOpenBehaviour = onOpenBehaviour,
                onOpenAppearance = onOpenAppearance,
            )
        }

        SettingsRoute.SizePosition -> SizePositionScreen(viewModel, contentPadding)
        SettingsRoute.EventIcons -> EventIconsScreen(viewModel, contentPadding)
        SettingsRoute.Behaviour -> BehaviourScreen(viewModel, contentPadding)
        SettingsRoute.Appearance -> AppearanceScreen(viewModel, contentPadding)
    }
}

/** The screens reachable from the Settings tab. Hoisted to MainScreen so the bottom bar can
 *  switch to a back pill on the detail screens. */
enum class SettingsRoute { List, SizePosition, EventIcons, Behaviour, Appearance }

@Composable
private fun SettingsList(
    contentPadding: PaddingValues,
    cutoutEnabled: Boolean,
    onCutoutEnabledChange: (Boolean) -> Unit,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenBehaviour: () -> Unit,
    onOpenAppearance: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CutoutEnableCard(enabled = cutoutEnabled, onEnabledChange = onCutoutEnabledChange)

        Column(
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsListItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.appearance_title),
                subtitle = stringResource(R.string.settings_size_subtitle),
                onClick = onOpenSizePosition,
            )
            SettingsListItem(
                icon = Icons.Rounded.ColorLens,
                title = stringResource(R.string.appearance_section_title),
                subtitle = stringResource(R.string.settings_appearance_subtitle),
                onClick = onOpenAppearance,
            )
            SettingsListItem(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.section_icons_title),
                subtitle = stringResource(R.string.settings_icons_subtitle),
                onClick = onOpenEventIcons,
            )
            SettingsListItem(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.behaviour_title),
                subtitle = stringResource(R.string.settings_behaviour_subtitle),
                onClick = onOpenBehaviour,
            )
        }
    }
}

@Composable
private fun CutoutEnableCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cutout_enable_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.cutout_enable_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
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
