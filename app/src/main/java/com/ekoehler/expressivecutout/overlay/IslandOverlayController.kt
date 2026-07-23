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
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.EventPreferences
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
import kotlinx.coroutines.flow.combine
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
    private val behaviourPreferences = BehaviourPreferences(context)
    private val eventPreferences = EventPreferences(context)
    private val density = context.resources.displayMetrics.density

    // The true full display width in px — the reference the width percentage is applied to.
    private val displayWidthPx: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            context.resources.displayMetrics.widthPixels
        }
    private val horizontalPaddingPx: Int = (16 * density).toInt()

    private val currentEvent = MutableStateFlow<IslandEvent?>(null)
    private val layoutState = MutableStateFlow(IslandLayout.DEFAULT)
    private val forcedExpanded = MutableStateFlow<Boolean?>(null)
    private val behaviourState = MutableStateFlow(BehaviourSettings())
    private var customIcons: Map<SystemEventType, IconSource> = emptyMap()
    private var eventEnabled: Map<SystemEventType, Boolean> = emptyMap()
    private var previewPinned = false
    private var previewExpanded = false
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
        observeBehaviour()
        observeEventPreferences()
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
                val forced by forcedExpanded.collectAsStateWithLifecycle()
                val behaviour by behaviourState.collectAsStateWithLifecycle()
                DynamicIsland(
                    event = event,
                    collapsed = layout.collapsed,
                    expanded = layout.expanded,
                    forcedExpanded = forced,
                    autoCollapse = behaviour.expandedAutoCollapse,
                    autoCollapseMs = behaviour.expandedCollapseSeconds * 1_000L,
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

    private fun observeBehaviour() = scope.launch {
        behaviourPreferences.settings.collect { behaviourState.value = it }
    }

    private fun observeEventPreferences() = scope.launch {
        eventPreferences.enabled.collect { eventEnabled = it }
    }

    private fun observeLayout() = scope.launch {
        layoutPreferences.layout.collect { layout ->
            layoutState.value = layout
            applyWindowSize()
        }
    }

    /**
     * Width and position are window-level concerns for whichever state is showing. The width
     * is set explicitly in pixels (a percentage of the real display width) rather than left to
     * WRAP_CONTENT, which the system clamps to the safe/rounded-corner area — the cause of the
     * island never growing past a fraction of the screen.
     */
    private fun applyWindowSize() {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val dims = if (expanded) layoutState.value.expanded else layoutState.value.collapsed
        params.width = (displayWidthPx * dims.widthPercent / 100) + horizontalPaddingPx
        params.x = (dims.offsetXDp * density).toInt()
        params.y = (dims.offsetYDp * density).toInt()
        windowManager.updateViewLayout(view, params)
    }

    /** While pinned (settings open), keep a persistent preview matching the tab being edited. */
    private fun observePreviewPin() = scope.launch {
        combine(IslandPreviewBus.active, IslandPreviewBus.expandedPreview, ::Pair)
            .collect { (pinned, expandedTab) ->
                previewPinned = pinned
                previewExpanded = expandedTab
                if (pinned) {
                    dismissJob?.cancel()
                    forcedExpanded.value = expandedTab
                    expanded = expandedTab
                    currentEvent.value = previewEvent
                    applyWindowSize()
                } else {
                    forcedExpanded.value = null
                    expanded = false
                    currentEvent.value = null
                }
            }
    }

    private fun observeSignals() = scope.launch {
        IslandEventBus.signals.collect { signal ->
            // Skip system events the user disabled for the pill.
            if (signal is CutoutSignal.System && eventEnabled[signal.type] == false) return@collect

            val autoExpand = signal is CutoutSignal.Notification &&
                behaviourState.value.notificationsAutoExpand
            forcedExpanded.value = null
            expanded = autoExpand
            currentEvent.value = resolver.resolve(signal, customIcons)
                .copy(initiallyExpanded = autoExpand)
            applyWindowSize()
            scheduleDismiss()
        }
    }

    /** Pause auto-dismiss while expanded; on collapse either hide or return to the normal cutout. */
    private fun onExpandedChanged(isExpanded: Boolean) {
        val wasExpanded = expanded
        expanded = isExpanded
        applyWindowSize()
        when {
            isExpanded -> dismissJob?.cancel()
            previewPinned -> Unit
            // Shrinking back from expanded and configured to vanish rather than stay.
            wasExpanded && behaviourState.value.expandedDisappearOnShrink -> {
                dismissJob?.cancel()
                currentEvent.value = null
            }

            else -> scheduleDismiss()
        }
    }

    private fun scheduleDismiss() {
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(behaviourState.value.normalDurationSeconds * 1_000L)
            // Return to the pinned preview if settings is still open, else hide.
            expanded = if (previewPinned) previewExpanded else false
            forcedExpanded.value = if (previewPinned) previewExpanded else null
            currentEvent.value = if (previewPinned) previewEvent else null
            applyWindowSize()
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        // Explicit width (set per state in applyWindowSize); the height still wraps the pill so
        // only the island's band intercepts touches, never the whole screen.
        val initialWidth =
            (displayWidthPx * IslandLayout.DEFAULT.collapsed.widthPercent / 100) + horizontalPaddingPx
        return WindowManager.LayoutParams(
            initialWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Seed with the collapsed default until the persisted layout arrives.
            x = (IslandLayout.DEFAULT.collapsed.offsetXDp * density).toInt()
            y = (IslandLayout.DEFAULT.collapsed.offsetYDp * density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }
}
