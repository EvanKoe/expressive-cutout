package com.ekoehler.expressivecutout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.MainScreen
import com.ekoehler.expressivecutout.ui.theme.ExpressiveCutoutTheme

/** Single-activity host. The overlay itself runs independently in the services. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            ExpressiveCutoutTheme(appTheme = theme) {
                MainScreen(viewModel)
            }
        }
    }
}
