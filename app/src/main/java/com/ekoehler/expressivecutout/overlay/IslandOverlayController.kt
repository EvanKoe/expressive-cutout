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
 *
 * The window is a fixed, full-width band (its height only changes when the layout config
 * changes). The island's size, position and corners are animated inside it by Compose, so
 * expand/collapse never resizes the window per frame — that was the source of the jank. The
 * window is made non-touchable while nothing is showing so it doesn't block the screen.
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

    // Full display width, used by the island to size itself as a percentage of the screen.
    private val displayWidthPx: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            context.resources.displayMetrics.widthPixels
        }
    private val displayWidthDp: Int = (displayWidthPx / density).toInt()

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
    private var windowResizeJob: Job? = null

    fun start() {
        lifecycleOwner.onCreate()
        addOverlay()
        observeIconPreferences()
        observeLayout()
        observeBehaviour()
        observeEventPreferences()
        observePreviewPin()
        observeSignals()
        observeVisibility()
    }

    fun stop() {
        dismissJob?.cancel()
        windowResizeJob?.cancel()
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
                    displayWidthDp = displayWidthDp,
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
            syncWindowHeight()
        }
    }

    /** Only intercept touches while the island is actually on screen. */
    private fun observeVisibility() = scope.launch {
        currentEvent.collect { setTouchable(it != null) }
    }

    /**
     * Resize the window to hug the current island state (collapsed vs expanded height), so the
     * empty band below a collapsed pill stops swallowing touches. The pill itself is still
     * animated inside Compose — this only changes the window at the two rest states, never per
     * frame, so the expand/collapse animation stays smooth.
     *
     * Grow/shrink is asymmetric: growing (expand) happens immediately so the window always has
     * room for the pill before it animates open; shrinking (collapse) is deferred until the pill
     * has finished collapsing, so the window never clips it mid-animation.
     */
    private fun syncWindowHeight() {
        requestWindowHeight(windowHeightPx(layoutState.value, expanded))
    }

    private fun requestWindowHeight(targetHeightPx: Int) {
        val params = layoutParams ?: return
        windowResizeJob?.cancel()
        if (targetHeightPx >= params.height) {
            resizeWindowHeight(targetHeightPx)
        } else {
            windowResizeJob = scope.launch {
                delay(WINDOW_SHRINK_DELAY_MS)
                resizeWindowHeight(targetHeightPx)
            }
        }
    }

    private fun resizeWindowHeight(targetHeightPx: Int) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        if (params.height != targetHeightPx) {
            params.height = targetHeightPx
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun setTouchable(touchable: Boolean) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val flag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val newFlags = if (touchable) params.flags and flag.inv() else params.flags or flag
        if (newFlags != params.flags) {
            params.flags = newFlags
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    /** Tall enough for whichever state extends lowest — used for the initial, safe window size. */
    private fun windowHeightPx(layout: IslandLayout): Int {
        val collapsed = layout.collapsed
        val expanded = layout.expanded
        val lowestDp = maxOf(
            collapsed.offsetYDp + collapsed.heightDp,
            expanded.offsetYDp + expanded.heightDp,
        )
        return ((lowestDp + WINDOW_MARGIN_DP) * density).toInt()
    }

    /** Height needed to contain just one state's pill. */
    private fun windowHeightPx(layout: IslandLayout, expanded: Boolean): Int {
        val dims = if (expanded) layout.expanded else layout.collapsed
        return ((dims.offsetYDp + dims.heightDp + WINDOW_MARGIN_DP) * density).toInt()
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
                } else {
                    forcedExpanded.value = null
                    expanded = false
                    currentEvent.value = null
                }
                syncWindowHeight()
            }
    }

    private fun observeSignals() = scope.launch {
        IslandEventBus.signals.collect { signal ->
            // Master switch: nothing shows when the cutout is disabled.
            if (!behaviourState.value.cutoutEnabled) return@collect
            // Skip system events the user disabled for the pill.
            if (signal is CutoutSignal.System && eventEnabled[signal.type] == false) return@collect

            val autoExpand = signal is CutoutSignal.Notification &&
                behaviourState.value.notificationsAutoExpand
            forcedExpanded.value = null
            expanded = autoExpand
            currentEvent.value = resolver.resolve(signal, customIcons)
                .copy(initiallyExpanded = autoExpand)
            syncWindowHeight()
            scheduleDismiss()
        }
    }

    /** Pause auto-dismiss while expanded; on collapse either hide or return to the normal cutout. */
    private fun onExpandedChanged(isExpanded: Boolean) {
        val wasExpanded = expanded
        expanded = isExpanded
        syncWindowHeight()
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
            syncWindowHeight()
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        // Full-width, fixed-height band. Starts non-touchable (nothing showing) and becomes
        // touchable only while the island is visible (so tap-to-expand works).
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            windowHeightPx(IslandLayout.DEFAULT),
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    private companion object {
        const val WINDOW_MARGIN_DP = 24

        // Hold the (larger) expanded window size until the pill has finished its ~220ms collapse
        // animation, then shrink — so the collapse never clips and the freed area becomes tappable.
        const val WINDOW_SHRINK_DELAY_MS = 300L
    }
}
