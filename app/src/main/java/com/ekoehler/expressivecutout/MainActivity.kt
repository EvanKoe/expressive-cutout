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

        if (Permissions.isNotificationAccessGranted(this) &&
            !CutoutNotificationListenerService.bound.value
        ) {
            CutoutNotificationListenerService.requestRebind(this)
        }
    }
}
