package com.ekoehler.expressivecutout.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.ui.components.ExpressiveNavBar
import com.ekoehler.expressivecutout.ui.components.NavBarItem
import com.ekoehler.expressivecutout.ui.screen.PermissionsTab
import com.ekoehler.expressivecutout.ui.screen.ProfileTab
import com.ekoehler.expressivecutout.ui.screen.SettingsTab

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
    val tabs = HomeTab.entries
    val current = tabs[selectedIndex]

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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

        Box(modifier = Modifier.fillMaxSize()) {
            when (current) {
                HomeTab.Settings -> SettingsTab(viewModel, contentPadding)
                HomeTab.Permissions -> PermissionsTab(contentPadding)
                HomeTab.Profile -> ProfileTab(viewModel, contentPadding)
            }

            ExpressiveNavBar(
                items = tabs.map { NavBarItem(stringResource(it.labelRes), it.icon) },
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
        }
    }
}
