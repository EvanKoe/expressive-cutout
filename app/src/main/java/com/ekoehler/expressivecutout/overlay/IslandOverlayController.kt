package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.data.LayoutPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the single overlay window and drives it from the [IslandEventBus]. Created and
 * destroyed by the accessibility service, whose context is required to add a
 * TYPE_ACCESSIBILITY_OVERLAY window without the SYSTEM_ALERT_WINDOW permission.
 */
class IslandOverlayController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val windowManager = requireNotNull(context.getSystemService<WindowManager>())
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val resolver = IconResolver(context)
    private val iconPreferences = IconPreferences(context)
    private val layoutPreferences = LayoutPreferences(context)
    private val density = context.resources.displayMetrics.density

    private val currentEvent = MutableStateFlow<IslandEvent?>(null)
    private val layoutState = MutableStateFlow(IslandLayout.DEFAULT)
    private var customIcons: Map<SystemEventType, IconSource> = emptyMap()
    private var previewPinned = false
    private var expanded = false

    // A neutral sample shown while the settings screen pins the island open.
    private val previewEvent by lazy {
        IslandEvent(
            id = -1L,
            icon = IslandIcon.Vector(Icons.Rounded.Tune),
            label = context.getString(R.string.preview_label),
            detail = context.getString(R.string.preview_detail),
            accent = Color(0xFF60A5FA),
        )
    }

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var dismissJob: Job? = null

    fun start() {
        lifecycleOwner.onCreate()
        addOverlay()
        observeIconPreferences()
        observeLayout()
        observePreviewPin()
        observeSignals()
    }

    fun stop() {
        dismissJob?.cancel()
        removeOverlay()
        lifecycleOwner.onDestroy()
        scope.cancel()
    }

    private fun addOverlay() {
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val event by currentEvent.collectAsStateWithLifecycle()
                val layout by layoutState.collectAsStateWithLifecycle()
                DynamicIsland(
                    event = event,
                    widthDp = layout.widthDp,
                    heightDp = layout.heightDp,
                    onExpandedChange = ::onExpandedChanged,
                )
            }
        }
        val params = buildLayoutParams()
        windowManager.addView(view, params)
        composeView = view
        layoutParams = params
    }

    private fun removeOverlay() {
        composeView?.let { windowManager.removeViewImmediate(it) }
        composeView = null
    }

    private fun observeIconPreferences() = scope.launch {
        iconPreferences.customIcons.collect { customIcons = it }
    }

    private fun observeLayout() = scope.launch {
        layoutPreferences.layout.collect { layout ->
            layoutState.value = layout
            applyPosition(layout)
        }
    }

    /** Position is a window-level concern, so it is pushed straight to the LayoutParams. */
    private fun applyPosition(layout: IslandLayout) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        params.x = (layout.offsetXDp * density).toInt()
        params.y = (layout.offsetYDp * density).toInt()
        windowManager.updateViewLayout(view, params)
    }

    /** While pinned (settings screen open), keep a persistent preview island on screen. */
    private fun observePreviewPin() = scope.launch {
        IslandPreviewBus.active.collect { pinned ->
            previewPinned = pinned
            expanded = false
            if (pinned) {
                dismissJob?.cancel()
                currentEvent.value = previewEvent
            } else {
                currentEvent.value = null
            }
        }
    }

    private fun observeSignals() = scope.launch {
        IslandEventBus.signals.collect { signal ->
            expanded = false
            currentEvent.value = resolver.resolve(signal, customIcons)
            scheduleDismiss()
        }
    }

    /** Pause auto-dismiss while the island is expanded; resume once it collapses. */
    private fun onExpandedChanged(isExpanded: Boolean) {
        expanded = isExpanded
        if (isExpanded) {
            dismissJob?.cancel()
        } else if (!previewPinned) {
            scheduleDismiss()
        }
    }

    private fun scheduleDismiss() {
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(DISPLAY_DURATION_MS)
            expanded = false
            // Fall back to the pinned preview if the settings screen is still open.
            currentEvent.value = if (previewPinned) previewEvent else null
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        // The window wraps the island so it only intercepts touches over the pill itself
        // (needed for tap-to-expand); the rest of the screen stays fully interactive.
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Seed with the default until the persisted layout arrives.
            x = (IslandLayout.DEFAULT.offsetXDp * density).toInt()
            y = (IslandLayout.DEFAULT.offsetYDp * density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    private companion object {
        const val DISPLAY_DURATION_MS = 3_200L
    }
}
