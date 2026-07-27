package com.ekoehler.expressivecutout.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.ui.components.BackNavBar
import com.ekoehler.expressivecutout.ui.components.ExpressiveNavBar
import com.ekoehler.expressivecutout.ui.components.NavBarItem
import com.ekoehler.expressivecutout.ui.screen.PermissionsTab
import com.ekoehler.expressivecutout.ui.screen.ProfileTab
import com.ekoehler.expressivecutout.ui.screen.SettingsRoute
import com.ekoehler.expressivecutout.ui.screen.SettingsTab
import com.ekoehler.expressivecutout.ui.screen.parent
import kotlin.coroutines.cancellation.CancellationException

private enum class HomeTab(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Settings(R.string.nav_settings, Icons.Rounded.Tune),
    Permissions(R.string.nav_permissions, Icons.Rounded.Shield),
    Profile(R.string.nav_profile, Icons.Rounded.Person),
}

/**
 * Root of the in-app UI: a top title, the current tab's content, and a floating expressive
 * navigation bar. Content padding is computed once here so every tab clears both the app
 * bar and the floating nav bar without each having to know about them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel = viewModel()) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var settingsRoute by rememberSaveable { mutableStateOf(SettingsRoute.List) }
    // Which tile's settings are open (saved by name so it survives config change / process death).
    var selectedTileName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTile = selectedTileName?.let { name -> DynamicTile.entries.firstOrNull { it.name == name } }
    val tabs = HomeTab.entries
    val current = tabs[selectedIndex]
    val haptics = LocalHapticFeedback.current

    // On a Settings detail screen the bottom bar becomes a back pill instead of the tab bar.
    val inSubScreen = current == HomeTab.Settings && settingsRoute != SettingsRoute.List

    // Drives the predictive-back "peek" animation: 0f = at rest, 1f = fully committed.
    val backProgress = remember { Animatable(0f) }
    var backEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    PredictiveBackHandler(enabled = inSubScreen) { progress ->
        try {
            progress.collect { event ->
                backEdge = event.swipeEdge
                backProgress.snapTo(event.progress)
            }
            // Gesture committed: navigate back, then reset the transform for the new screen.
            settingsRoute = settingsRoute.parent
            backProgress.snapTo(0f)
        } catch (_: CancellationException) {
            // Gesture cancelled: ease the peek back to rest.
            backProgress.animateTo(0f)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text(stringResource(current.labelRes)) },
            )
        },
    ) { innerPadding ->
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            // Clear the floating nav bar (≈64dp tall) plus its bottom margin and the system bar.
            bottom = 96.dp + navBarBottomInset,
        )

        Box(modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surfaceContainer)) {
            // Peek animation: as the user drags back, the content shrinks, rounds its corners
            // and slides toward the swiped edge, revealing the surface beneath.
            val contentTransform = Modifier.graphicsLayer {
                val p = backProgress.value
                if (p > 0f) {
                    val scale = 1f - 0.08f * p
                    scaleX = scale
                    scaleY = scale
                    translationX = (if (backEdge == BackEventCompat.EDGE_LEFT) 1f else -1f) * 16.dp.toPx() * p
                    shape = RoundedCornerShape(32.dp.toPx() * p)
                    clip = true
                }
            }
            Box(modifier = Modifier.fillMaxSize().then(contentTransform)) {
                // Fade-through between the three top-level tabs.
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90))
                    },
                    label = "homeTab",
                ) { tab ->
                    when (tab) {
                        HomeTab.Settings -> SettingsTab(
                            viewModel = viewModel,
                            contentPadding = contentPadding,
                            route = settingsRoute,
                            selectedTile = selectedTile,
                            onOpenSizePosition = { settingsRoute = SettingsRoute.SizePosition },
                            onOpenEventIcons = { settingsRoute = SettingsRoute.EventIcons },
                            onOpenDynamicTiles = { settingsRoute = SettingsRoute.DynamicTiles },
                            onOpenTile = { tile ->
                                selectedTileName = tile.name
                                settingsRoute = SettingsRoute.DynamicTileDetail
                            },
                            onOpenBehaviour = { settingsRoute = SettingsRoute.Behaviour },
                            onOpenAppearance = { settingsRoute = SettingsRoute.Appearance },
                            onOpenBackground = { settingsRoute = SettingsRoute.Background },
                            onOpenActionButtons = { settingsRoute = SettingsRoute.ActionButtons },
                        )

                        HomeTab.Permissions -> PermissionsTab(contentPadding)
                        HomeTab.Profile -> ProfileTab(viewModel, contentPadding)
                    }
                }
            }

            // Soft scrim so scrolled content fades out beneath the floating bottom bar.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.7f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                            1f to MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ),
            )

            val barModifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)

            if (inSubScreen) {
                val title = when (settingsRoute) {
                    SettingsRoute.SizePosition -> stringResource(R.string.appearance_title)
                    SettingsRoute.DynamicTiles -> stringResource(R.string.dynamic_tiles_title)
                    SettingsRoute.DynamicTileDetail ->
                        selectedTile?.let { stringResource(it.labelRes) } ?: stringResource(R.string.dynamic_tiles_title)
                    SettingsRoute.Behaviour -> stringResource(R.string.behaviour_title)
                    SettingsRoute.Appearance -> stringResource(R.string.appearance_section_title)
                    SettingsRoute.Background -> stringResource(R.string.appearance_background_color)
                    SettingsRoute.ActionButtons -> stringResource(R.string.action_buttons_title)
                    else -> stringResource(R.string.section_icons_title)
                }
                BackNavBar(
                    title = title,
                    onBack = { settingsRoute = settingsRoute.parent },
                    modifier = barModifier,
                )
            } else {
                ExpressiveNavBar(
                    items = tabs.map { NavBarItem(stringResource(it.labelRes), it.icon) },
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        if (index != selectedIndex) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        selectedIndex = index
                    },
                    modifier = barModifier,
                )
            }
        }
    }
}
