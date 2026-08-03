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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.ui.components.BackNavBar
import com.ekoehler.expressivecutout.ui.components.ExpressiveNavBar
import com.ekoehler.expressivecutout.ui.components.NavBarItem
import com.ekoehler.expressivecutout.ui.screen.PermissionsTab
import com.ekoehler.expressivecutout.ui.screen.ProfileRoute
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
 * Root of the in-app UI: the current tab's content between two scrims, and a floating expressive
 * navigation bar. Content padding is computed once here so every tab clears both the top scrim and
 * the floating nav bar without each having to know about them.
 */
@Composable
fun MainScreen(viewModel: AppViewModel = viewModel()) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var settingsRoute by rememberSaveable { mutableStateOf(SettingsRoute.List) }
    var profileRoute by rememberSaveable { mutableStateOf(ProfileRoute.List) }
    // Which tile's settings are open (saved by name so it survives config change / process death).
    var selectedTileName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTile = selectedTileName?.let { name -> DynamicTile.entries.firstOrNull { it.name == name } }
    // Likewise for the event whose detail (icon + duration) screen is open.
    var selectedEventName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedEvent = selectedEventName?.let { name -> SystemEventType.entries.firstOrNull { it.name == name } }
    val tabs = HomeTab.entries
    val current = tabs[selectedIndex]
    val haptics = LocalHapticFeedback.current

    // On a detail screen the bottom bar becomes a back pill instead of the tab bar.
    val inSubScreen = (current == HomeTab.Settings && settingsRoute != SettingsRoute.List) ||
        (current == HomeTab.Profile && profileRoute != ProfileRoute.List)

    val navigateBack: () -> Unit = {
        if (current == HomeTab.Profile) {
            profileRoute = ProfileRoute.List
        } else {
            settingsRoute = settingsRoute.parent
        }
    }

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
            navigateBack()
            backProgress.snapTo(0f)
        } catch (_: CancellationException) {
            // Gesture cancelled: ease the peek back to rest.
            backProgress.animateTo(0f)
        }
    }

    // No tab has an app bar: each screen carries its own heading (the app identity or a section
    // title on the list, the screen title in the back pill), so a bar would only waste height at
    // the top. A scrim mirroring the bottom one stands in for it.
    Scaffold { _ ->
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // The scrim is only a fade for *scrolled* content, so content at rest starts below it —
        // same relationship the bottom padding has with the bottom scrim.
        val topScrimHeight = statusBarTopInset + 40.dp
        val contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = topScrimHeight + 8.dp,
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
                            selectedEvent = selectedEvent,
                            onOpenSizePosition = { settingsRoute = SettingsRoute.SizePosition },
                            onOpenEventIcons = { settingsRoute = SettingsRoute.EventIcons },
                            onOpenEvent = { event ->
                                selectedEventName = event.name
                                settingsRoute = SettingsRoute.EventDetail
                            },
                            onOpenDynamicTiles = { settingsRoute = SettingsRoute.DynamicTiles },
                            onOpenTile = { tile ->
                                selectedTileName = tile.name
                                settingsRoute = SettingsRoute.DynamicTileDetail
                            },
                            onOpenApps = { settingsRoute = SettingsRoute.Apps },
                            onOpenBehaviour = { settingsRoute = SettingsRoute.Behaviour },
                            onOpenShowsWhenEmpty = { settingsRoute = SettingsRoute.ShowsWhenEmpty },
                            onOpenAnimation = { settingsRoute = SettingsRoute.Animation },
                            onOpenAppearance = { settingsRoute = SettingsRoute.Appearance },
                            onOpenBackground = { settingsRoute = SettingsRoute.Background },
                            onOpenActionButtons = { settingsRoute = SettingsRoute.ActionButtons },
                        )

                        HomeTab.Permissions -> PermissionsTab(contentPadding)
                        HomeTab.Profile -> ProfileTab(
                            viewModel = viewModel,
                            contentPadding = contentPadding,
                            route = profileRoute,
                            onOpenChangelog = { profileRoute = ProfileRoute.Changelog },
                            onOpenPermissionDetails = { profileRoute = ProfileRoute.PermissionDetails },
                        )
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

            // Standing in for the app bar: content fades out under the status bar as it scrolls,
            // instead of colliding with it.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topScrimHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.surfaceContainer,
                            0.3f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                            1f to Color.Transparent,
                        ),
                    ),
            )

            val barModifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)

            if (inSubScreen) {
                val title = if (current == HomeTab.Profile) {
                    when (profileRoute) {
                        ProfileRoute.PermissionDetails -> stringResource(R.string.profile_permissions_title)
                        else -> stringResource(R.string.profile_version)
                    }
                } else when (settingsRoute) {
                    SettingsRoute.SizePosition -> stringResource(R.string.appearance_title)
                    SettingsRoute.DynamicTiles -> stringResource(R.string.dynamic_tiles_title)
                    SettingsRoute.DynamicTileDetail ->
                        selectedTile?.let { stringResource(it.labelRes) } ?: stringResource(R.string.dynamic_tiles_title)
                    SettingsRoute.EventDetail ->
                        selectedEvent?.let { stringResource(it.labelRes) } ?: stringResource(R.string.section_icons_title)
                    SettingsRoute.Apps -> stringResource(R.string.apps_title)
                    SettingsRoute.Behaviour -> stringResource(R.string.behaviour_title)
                    SettingsRoute.ShowsWhenEmpty -> stringResource(R.string.behaviour_empty_pill)
                    SettingsRoute.Animation -> stringResource(R.string.animation_title)
                    SettingsRoute.Appearance -> stringResource(R.string.appearance_section_title)
                    SettingsRoute.Background -> stringResource(R.string.appearance_background_color)
                    SettingsRoute.ActionButtons -> stringResource(R.string.action_buttons_title)
                    else -> stringResource(R.string.section_icons_title)
                }
                BackNavBar(
                    title = title,
                    onBack = navigateBack,
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
