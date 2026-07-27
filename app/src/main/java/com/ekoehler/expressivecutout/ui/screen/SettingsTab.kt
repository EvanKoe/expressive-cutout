package com.ekoehler.expressivecutout.ui.screen

import android.graphics.Color
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.screen.tiles.TileSettingsScreen
import java.nio.file.WatchEvent

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
    selectedTile: DynamicTile?,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenDynamicTiles: () -> Unit,
    onOpenTile: (DynamicTile) -> Unit,
    onOpenBehaviour: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBackground: () -> Unit,
    onOpenActionButtons: () -> Unit,
) {
    // Routing (and back navigation, via the bottom bar) is owned by MainScreen.
    // Deeper routes slide in from the right; stepping back slides in from the left, so the
    // motion mirrors the predictive-back peek.
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            val dir = if (forward) 1 else -1
            (slideInHorizontally(tween(300)) { w -> dir * w } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(300)) { w -> -dir * w } + fadeOut(tween(300)))
        },
        label = "settingsRoute",
    ) { current ->
        when (current) {
            SettingsRoute.List -> {
                val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
                SettingsList(
                    contentPadding = contentPadding,
                    cutoutEnabled = behaviour.cutoutEnabled,
                    onCutoutEnabledChange = viewModel::setCutoutEnabled,
                    onOpenSizePosition = onOpenSizePosition,
                    onOpenEventIcons = onOpenEventIcons,
                    onOpenDynamicTiles = onOpenDynamicTiles,
                    onOpenBehaviour = onOpenBehaviour,
                    onOpenAppearance = onOpenAppearance,
                )
            }

            SettingsRoute.SizePosition -> SizePositionScreen(viewModel, contentPadding)
            SettingsRoute.EventIcons -> EventIconsScreen(viewModel, contentPadding)
            SettingsRoute.DynamicTiles -> DynamicTilesScreen(viewModel, contentPadding, onOpenTile)
            SettingsRoute.DynamicTileDetail ->
                selectedTile?.let { TileSettingsScreen(it, viewModel, contentPadding) }
            SettingsRoute.Behaviour -> BehaviourScreen(viewModel, contentPadding)
            SettingsRoute.Appearance -> AppearanceScreen(viewModel, contentPadding, onOpenBackground, onOpenActionButtons)
            SettingsRoute.Background -> BackgroundScreen(viewModel, contentPadding)
            SettingsRoute.ActionButtons -> ButtonScreen(viewModel, contentPadding)
        }
    }
}

/** The screens reachable from the Settings tab. Hoisted to MainScreen so the bottom bar can
 *  switch to a back pill on the detail screens. */
enum class SettingsRoute { List, SizePosition, EventIcons, DynamicTiles, DynamicTileDetail, Behaviour, Appearance, Background, ActionButtons }

/**
 * The screen that back navigation returns to. Most detail screens go straight back to the list,
 * but Background and ActionButtons are reached from Appearance, so they step back there first.
 */
val SettingsRoute.parent: SettingsRoute
    get() = when (this) {
        SettingsRoute.Background, SettingsRoute.ActionButtons -> SettingsRoute.Appearance
        SettingsRoute.DynamicTileDetail -> SettingsRoute.DynamicTiles
        else -> SettingsRoute.List
    }

/** How far down the navigation stack a route sits, used to pick the slide direction. */
val SettingsRoute.depth: Int
    get() = when (this) {
        SettingsRoute.List -> 0
        SettingsRoute.Background, SettingsRoute.ActionButtons, SettingsRoute.DynamicTileDetail -> 2
        else -> 1
    }

@Composable
private fun SettingsList(
    contentPadding: PaddingValues,
    cutoutEnabled: Boolean,
    onCutoutEnabledChange: (Boolean) -> Unit,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenDynamicTiles: () -> Unit,
    onOpenBehaviour: () -> Unit,
    onOpenAppearance: () -> Unit,
) {
    val context = LocalContext.current
    // Re-reads on resume so returning from the system Accessibility settings updates immediately.
    val accessibilityAvailable = rememberAccessibilityGranted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Accessibility access permission request if needed
        AnimatedVisibility(
            visible = !accessibilityAvailable,
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp))
        ) {
            SettingsListItem(
                icon = Icons.Rounded.ErrorOutline,
                subtitle = stringResource(R.string.settings_access_missing),
                title = stringResource(R.string.perm_accessibility_title),
                onClick = { Permissions.openAccessibilitySettings(context) },
                bgColor = MaterialTheme.colorScheme.primaryContainer,
                fgColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        CutoutEnableCard(
            enabled = if (accessibilityAvailable) cutoutEnabled else false,
            onEnabledChange = onCutoutEnabledChange,
            canEdit = accessibilityAvailable
        )

        // Customization of the cutout
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
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.behaviour_title),
                subtitle = stringResource(R.string.settings_behaviour_subtitle),
                onClick = onOpenBehaviour,
            )
        }

        // Events and tiles that trigger the cutout
        Column(
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsListItem(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.section_icons_title),
                subtitle = stringResource(R.string.settings_icons_subtitle),
                onClick = onOpenEventIcons,
            )
            SettingsListItem(
                icon = Icons.Rounded.GridView,
                title = stringResource(R.string.dynamic_tiles_title),
                subtitle = stringResource(R.string.settings_dynamic_tiles_subtitle),
                onClick = onOpenDynamicTiles,
            )
        }
    }
}

@Composable
private fun CutoutEnableCard(
    enabled: Boolean,
    canEdit: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
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
            Switch(checked = enabled, onCheckedChange = onEnabledChange, enabled = canEdit)
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    bgColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    fgColor: androidx.compose.ui.graphics.Color? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
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
                tint = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
