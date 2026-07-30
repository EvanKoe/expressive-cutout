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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.service.CutoutAccessibilityService

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

/**
 * True while the accessibility service is actually bound, as opposed to merely granted. Android
 * keeps the grant in Settings.Secure across a reinstall or an app update but does not always
 * rebind the service, leaving the island silently dead while every permission check still reads
 * green. Collected from a [kotlinx.coroutines.flow.StateFlow] rather than re-read on resume so the
 * warning clears the instant the service binds, instead of lingering until the next resume.
 */
@Composable
fun rememberAccessibilityRunning(): Boolean {
    val bound by CutoutAccessibilityService.bound.collectAsStateWithLifecycle()
    return bound
}
