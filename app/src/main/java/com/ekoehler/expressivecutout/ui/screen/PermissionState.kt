package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ekoehler.expressivecutout.permissions.Permissions

/**
 * Live accessibility-grant state that re-reads on every [Lifecycle.Event.ON_RESUME], so returning
 * from the system Accessibility settings instantly reflects the change instead of waiting for the
 * next unrelated recomposition (which previously only happened when navigating between screens).
 */
@Composable
fun rememberAccessibilityGranted(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember { mutableStateOf(Permissions.isAccessibilityGranted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = Permissions.isAccessibilityGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}
