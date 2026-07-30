package com.ekoehler.expressivecutout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.service.CutoutNotificationListenerService
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.MainScreen
import com.ekoehler.expressivecutout.ui.theme.ExpressiveCutoutTheme
import com.ekoehler.expressivecutout.ui.theme.isDark

/** Single-activity host. The overlay itself runs independently in the services. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            // enableEdgeToEdge() picks the status-bar icon tint from the *system* dark setting, so
            // a user on the in-app Dark theme with a light system would get dark-on-dark icons.
            // Follow the resolved app theme instead: light icons whenever the app is dark.
            val darkTheme = theme.isDark()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !darkTheme
            }
            ExpressiveCutoutTheme(appTheme = theme) {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Android keeps the notification-access grant across an app update but often leaves the
        // listener unbound, silently starving every dynamic tile (music, phone, timer) while the
        // grant still reads green. If the grant is held but the listener isn't actually connected,
        // ask the framework to rebind it — healing the stale binding without making the user toggle
        // the permission off and on by hand.
        if (Permissions.isNotificationAccessGranted(this) &&
            !CutoutNotificationListenerService.bound.value
        ) {
            CutoutNotificationListenerService.requestRebind(this)
        }
    }
}
