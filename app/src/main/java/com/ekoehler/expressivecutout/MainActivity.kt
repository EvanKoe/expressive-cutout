package com.ekoehler.expressivecutout

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.service.CutoutNotificationListenerService
import com.ekoehler.expressivecutout.system.AppLocale
import com.ekoehler.expressivecutout.system.FoldablePostureMonitor
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.MainScreen
import com.ekoehler.expressivecutout.ui.theme.ExpressiveCutoutTheme
import com.ekoehler.expressivecutout.ui.theme.isDark
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

/** Single-activity host. The overlay itself runs independently in the services. */
class MainActivity : ComponentActivity() {
    private var foldablePostureMonitor: FoldablePostureMonitor? = null

    /** Localises the whole settings UI to the picked language. See [AppLocale]. */
    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(AppLocale.wrap(newBase))

    /**
     * Hosts the whole settings UI: one edge-to-edge activity with the Compose tree rooted here, so
     * the system bars follow the app theme.
     */
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
        foldablePostureMonitor = FoldablePostureMonitor(this).also { it.start() }
        observeFoldablePosture()
    }

    /**
     * Uses the window's fold feature when available; a hinge sensor with no reported fold means the
     * device is on its closed display.
     */
    @OptIn(FlowPreview::class)
    private fun observeFoldablePosture() {
        val hasHingeSensor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
        val layoutPreferences = LayoutPreferences(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .debounce(250L)
                    .collect { layoutInfo ->
                        val hasFold = layoutInfo.displayFeatures.any { it is FoldingFeature }
                        when {
                            hasFold -> layoutPreferences.setDeviceClosed(false)
                            !hasHingeSensor -> layoutPreferences.setDeviceClosed(true)
                        }
                    }
                }
        }
    }

    /**
     * Releases the activity's sensor listener; the accessibility service keeps its own monitor
     * alive while the overlay remains active.
     */
    override fun onDestroy() {
        foldablePostureMonitor?.stop()
        foldablePostureMonitor = null
        super.onDestroy()
    }

    /**
     * Re-arms the notification listener on the way back into the app. The framework silently
     * unbinds it after an app update while the grant still reads as given, which would leave every
     * dynamic tile starved with nothing to show the user is wrong.
     */
    override fun onResume() {
        super.onResume()

        if (Permissions.isNotificationAccessGranted(this) &&
            !CutoutNotificationListenerService.bound.value
        ) {
            CutoutNotificationListenerService.requestRebind(this)
        }
    }
}
