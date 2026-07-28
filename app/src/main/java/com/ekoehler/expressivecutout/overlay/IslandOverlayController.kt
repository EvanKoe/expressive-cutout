package com.ekoehler.expressivecutout.overlay

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.NowPlayingBus
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimerBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.AppearancePreferences
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.DynamicTilePreferences
import com.ekoehler.expressivecutout.data.EventPreferences
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.data.MusicTilePreferences
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.data.PhoneTilePreferences
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import com.ekoehler.expressivecutout.data.TimerTilePreferences
import com.ekoehler.expressivecutout.data.TimerTileSettings
import com.ekoehler.expressivecutout.service.CutoutNotificationListenerService
import com.ekoehler.expressivecutout.ui.theme.ExpressiveCutoutTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.lang.reflect.Proxy

/**
 * Owns the single overlay window and drives it from the [IslandEventBus]. Created and
 * destroyed by the accessibility service, whose context is required to add a
 * TYPE_ACCESSIBILITY_OVERLAY window without the SYSTEM_ALERT_WINDOW permission.
 *
 * The window is a fixed, full-width band (its height only changes when the layout config
 * changes). The island's size, position and corners are animated inside it by Compose, so
 * expand/collapse never resizes the window per frame — that was the source of the jank. The
 * window is made non-touchable while nothing is showing so it doesn't block the screen.
 *
 * So the fixed window doesn't swallow touches around the island (e.g. the notification-shade
 * pull), a touchable region tracking just the pill's rectangle is installed on the window (see
 * [installTouchableRegion]); everything outside it falls through to the app behind. Crucially this
 * only marks which pixels are touchable — it never resizes the window — so the animation stays
 * smooth. It relies on a semi-private API and degrades gracefully (window stays fully touchable)
 * where that isn't available.
 */
class IslandOverlayController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val windowManager = requireNotNull(context.getSystemService<WindowManager>())
    private val keyguardManager = context.getSystemService<KeyguardManager>()
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val resolver = IconResolver(context)
    private val iconPreferences = IconPreferences(context)
    private val layoutPreferences = LayoutPreferences(context)
    private val behaviourPreferences = BehaviourPreferences(context)
    private val appearancePreferences = AppearancePreferences(context)
    private val eventPreferences = EventPreferences(context)
    private val dynamicTilePreferences = DynamicTilePreferences(context)
    private val musicTilePreferences = MusicTilePreferences(context)
    private val phoneTilePreferences = PhoneTilePreferences(context)
    private val timerTilePreferences = TimerTilePreferences(context)
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
    private val appearanceState = MutableStateFlow(AppearanceSettings())
    private var customIcons: Map<SystemEventType, IconSource> = emptyMap()
    private var eventEnabled: Map<SystemEventType, Boolean> = emptyMap()
    private var eventDurations: Map<SystemEventType, Int> = emptyMap()
    private var eventAnimatedIcons: Map<SystemEventType, Boolean> = emptyMap()
    private var eventAnimatedIconLoops: Map<SystemEventType, Boolean> = emptyMap()
    // The system event currently on the pill, so its auto-dismiss uses that event's own duration
    // override (null while a notification or live tile is showing → the global normal duration).
    private var currentSystemEventType: SystemEventType? = null
    private var eventDynamicColor: Boolean = false
    private var eventDynamicColorRole: DynamicRole = DynamicRole.PRIMARY
    private var eventDynamicColorOpacity: Float = 1f
    private var tileEnabled: Map<DynamicTile, Boolean> = emptyMap()
    private var musicSettings: MusicTileSettings = MusicTileSettings()
    private var phoneSettings: PhoneTileSettings = PhoneTileSettings()
    private var timerSettings: TimerTileSettings = TimerTileSettings()
    private var previewPinned = false
    private var previewExpanded = false
    private var expanded = false
    // True while a media session is actively playing; keeps the music cutout pinned up (no
    // auto-dismiss) for as long as playback lasts.
    private var musicPlaying = false
    // The last resolved music event, so the pill can return after a notification/system event that
    // briefly took over the cutout while playback carried on.
    private var lastMusicEvent: IslandEvent? = null
    // True while a phone call is present; keeps the call cutout pinned up (no auto-dismiss) for the
    // whole call, and — like [lastMusicEvent] — lets the pill return after an interruption.
    private var callActive = false
    private var lastCallEvent: IslandEvent? = null
    // True while a countdown timer is running; keeps the timer cutout pinned up (no auto-dismiss) for
    // the whole countdown, and — like [lastCallEvent] — lets the pill return after an interruption.
    private var timerActive = false
    private var lastTimerEvent: IslandEvent? = null

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

    // The installed OnComputeInternalInsetsListener (a reflection Proxy), kept so it can be removed.
    private var insetsListener: Any? = null

    // True while the window has been torn down because "hide on lockscreen" is on and the device is
    // locked. Guards signal handling and drives whether the window currently exists.
    private var lockHidden = false

    // Re-evaluate lock visibility whenever the screen or lock state changes. All are protected
    // system broadcasts.
    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = applyLockVisibility()
    }

    fun start() {
        lifecycleOwner.onCreate()
        addOverlay()
        registerLockReceiver()
        observeIconPreferences()
        observeLayout()
        observeBehaviour()
        observeAppearance()
        observeEventPreferences()
        observeEventDurations()
        observeEventAnimatedIcons()
        observeEventDynamicColor()
        observeEventDynamicColorRole()
        observeEventDynamicColorOpacity()
        observeTilePreferences()
        observeMusicSettings()
        observePhoneSettings()
        observeTimerSettings()
        observeNowPlaying()
        observeOnCall()
        observeRunningTimer()
        observePreviewPin()
        observeSignals()
        observeVisibility()
    }

    fun stop() {
        dismissJob?.cancel()
        windowResizeJob?.cancel()
        runCatching { context.unregisterReceiver(lockReceiver) }
        removeOverlay()
        lifecycleOwner.onDestroy()
        scope.cancel()
    }

    private fun registerLockReceiver() {
        ContextCompat.registerReceiver(
            context,
            lockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /**
     * Enforce "hide on lockscreen" by fully adding or removing the overlay window as the lock state
     * changes. Tearing the window down — rather than just hiding it — means nothing is composed or
     * drawn while the device is locked, which matters on low-end hardware. Re-checked on screen
     * on/off, on unlock, and whenever the setting itself is toggled. Idempotent: the [lockHidden]
     * guard makes repeat calls in the same state no-ops.
     */
    private fun applyLockVisibility() {
        val shouldHide = behaviourState.value.hideOnLockscreen &&
            keyguardManager?.isKeyguardLocked == true
        when {
            shouldHide && !lockHidden -> {
                lockHidden = true
                dismissJob?.cancel()
                windowResizeJob?.cancel()
                currentEvent.value = null
                removeOverlay()
            }

            !shouldHide && lockHidden -> {
                lockHidden = false
                addOverlay()
                syncWindowHeight()
            }
        }
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
                val appearance by appearanceState.collectAsStateWithLifecycle()
                // Theme the overlay so the "Dynamic color for all events" badge picks up the app's
                // real primary / on-primary (Material You or the brand fallback), not the M3 baseline.
                ExpressiveCutoutTheme {
                    DynamicIsland(
                        event = event,
                        collapsed = layout.collapsed,
                        expanded = layout.expanded,
                        displayWidthDp = displayWidthDp,
                        forcedExpanded = forced,
                        animationDurationMs = behaviour.animationDurationMs,
                        autoCollapse = behaviour.expandedAutoCollapse,
                        autoCollapseMs = behaviour.expandedCollapseSeconds * 1_000L,
                        appearance = appearance,
                        showActions = behaviour.showActionButtons,
                        shrinkOnSwipeUp = behaviour.shrinkOnSwipeUp,
                        swipeToDismiss = behaviour.swipeToDismiss,
                        swipeDismissDirection = behaviour.swipeDismissDirection,
                        swipeDismissTarget = behaviour.swipeDismissTarget,
                        onExpandedChange = ::onExpandedChanged,
                        onActivate = ::onActivate,
                        onAction = ::onAction,
                        onReply = ::onReply,
                        onReplyActiveChange = ::onReplyActive,
                        onDismiss = ::onDismiss,
                    )
                }
            }
        }
        val params = buildLayoutParams()
        windowManager.addView(view, params)
        composeView = view
        layoutParams = params
        installTouchableRegion(view)
    }

    private fun removeOverlay() {
        composeView?.let { windowManager.removeViewImmediate(it) }
        composeView = null
        insetsListener = null
    }

    private fun observeIconPreferences() = scope.launch {
        iconPreferences.customIcons.collect { customIcons = it }
    }

    private fun observeBehaviour() = scope.launch {
        behaviourPreferences.settings.collect {
            behaviourState.value = it
            // Toggling "hide on lockscreen" (or first load while already locked) must take effect now.
            applyLockVisibility()
        }
    }

    private fun observeAppearance() = scope.launch {
        appearancePreferences.settings.collect {
            appearanceState.value = it
            // The action-button height feeds the expanded window's extra room; keep them in step.
            syncWindowHeight()
        }
    }

    private fun observeEventPreferences() = scope.launch {
        eventPreferences.enabled.collect { eventEnabled = it }
    }

    private fun observeEventDurations() = scope.launch {
        eventPreferences.durations.collect { eventDurations = it }
    }

    private fun observeEventAnimatedIcons() {
        scope.launch { eventPreferences.animatedIcons.collect { eventAnimatedIcons = it } }
        scope.launch { eventPreferences.animatedIconLoops.collect { eventAnimatedIconLoops = it } }
    }

    private fun observeEventDynamicColor() = scope.launch {
        eventPreferences.dynamicColor.collect { eventDynamicColor = it }
    }

    private fun observeEventDynamicColorRole() = scope.launch {
        eventPreferences.dynamicColorRole.collect { eventDynamicColorRole = it }
    }

    private fun observeEventDynamicColorOpacity() = scope.launch {
        eventPreferences.dynamicColorOpacity.collect { eventDynamicColorOpacity = it }
    }

    private fun observeTilePreferences() = scope.launch {
        dynamicTilePreferences.enabled.collect { tileEnabled = it }
    }

    private fun observeMusicSettings() = scope.launch {
        musicTilePreferences.settings.collect { musicSettings = it }
    }

    private fun observePhoneSettings() = scope.launch {
        phoneTilePreferences.settings.collect { phoneSettings = it }
    }

    private fun observeTimerSettings() = scope.launch {
        timerTilePreferences.settings.collect { timerSettings = it }
    }

    /**
     * Follow live playback so the music cutout stays up for exactly as long as music plays. While
     * something is playing we hold the (already-shown) music pill open indefinitely; when it pauses
     * or the session ends we hand it back to the normal auto-dismiss timer so it fades out.
     */
    private fun observeNowPlaying() = scope.launch {
        NowPlayingBus.state.collect { now ->
            musicPlaying = now?.isPlaying == true
            // Once the session ends there's nothing to return to.
            if (now == null) lastMusicEvent = null
            // Only steer the music pill; leave notifications/system events to their own timers.
            if (previewPinned || currentEvent.value?.media == null) return@collect
            if (musicPlaying) dismissJob?.cancel() else scheduleDismiss()
        }
    }

    /** The music cutout should stay pinned up (no auto-dismiss) while music is playing. */
    private fun isPinnedMusic(): Boolean = musicPlaying && currentEvent.value?.media != null

    /**
     * Follow the live call so the phone cutout stays up for exactly as long as the call lasts. The
     * call is "active" while the dialer's notification exists ([OnCallBus] non-null); when it ends we
     * hand the pill back to the normal auto-dismiss timer so it fades out. Mirrors [observeNowPlaying].
     */
    private fun observeOnCall() = scope.launch {
        OnCallBus.state.collect { call ->
            callActive = call != null
            if (call == null) lastCallEvent = null
            // Only steer the call pill; leave notifications/system events to their own timers.
            if (previewPinned || currentEvent.value?.call == null) return@collect
            if (callActive) dismissJob?.cancel() else scheduleDismiss()
        }
    }

    /** The phone cutout should stay pinned up (no auto-dismiss) while a call is in progress. */
    private fun isPinnedCall(): Boolean = callActive && currentEvent.value?.call != null

    /**
     * Follow the live countdown so the timer cutout stays up for exactly as long as the timer runs.
     * The timer is "active" while the clock app's count-down notification exists ([RunningTimerBus]
     * non-null); when it is reset or finishes we hand the pill back to the normal auto-dismiss timer.
     * Mirrors [observeOnCall].
     */
    private fun observeRunningTimer() = scope.launch {
        RunningTimerBus.state.collect { timer ->
            timerActive = timer != null
            if (timer == null) lastTimerEvent = null
            // Only steer the timer pill; leave notifications/system events to their own timers.
            if (previewPinned || currentEvent.value?.timer == null) return@collect
            // The clock re-posts (updating this bus) whenever the timer's state changes, so refresh the
            // shown pill's buttons and label in place — that's how Pause / Add 1 min flip to Resume /
            // Reset when paused. Done here rather than by re-emitting the signal so the pill's expanded
            // state and dismiss timing are left untouched.
            if (timer != null) {
                currentEvent.value = currentEvent.value?.copy(
                    actions = resolver.timerActions(timer.actions),
                    label = timer.label?.takeIf { it.isNotBlank() }
                        ?: context.getString(DynamicTile.TIMER.labelRes),
                )
                lastTimerEvent = currentEvent.value
            }
            if (timerActive) dismissJob?.cancel() else scheduleDismiss()
        }
    }

    /** The timer cutout should stay pinned up (no auto-dismiss) while a countdown is running. */
    private fun isPinnedTimer(): Boolean = timerActive && currentEvent.value?.timer != null

    /** Any live tile (music, a call or a running timer) is currently pinned up. */
    private fun isPinnedLiveTile(): Boolean = isPinnedMusic() || isPinnedCall() || isPinnedTimer()

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

    /**
     * Restrict the (fixed) window's touchable area to the pill's rectangle, so touches anywhere
     * else fall through to whatever is behind — the app, and the status bar / notification shade.
     * This is done with [ViewTreeObserver]'s hidden OnComputeInternalInsetsListener: it only marks
     * which pixels are touchable and is re-evaluated on the view's normal traversals, so the pill
     * can animate freely without the window ever being resized. Reflection + a Proxy are needed
     * because the listener type is not public; if any part is unavailable we simply skip it and the
     * window stays fully touchable (the pre-existing behaviour), never crashing.
     */
    private fun installTouchableRegion(view: View) {
        runCatching {
            val listenerClass =
                Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val infoClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")
            val setTouchableInsets = infoClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
            val touchableRegionField = infoClass.getField("touchableRegion")
            val touchableInsetsRegion = infoClass.getField("TOUCHABLE_INSETS_REGION").getInt(null)
            val addListener =
                ViewTreeObserver::class.java.getMethod("addOnComputeInternalInsetsListener", listenerClass)

            val proxy = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { self, method, args ->
                when (method.name) {
                    "onComputeInternalInsets" -> {
                        val info = args?.getOrNull(0)
                        if (info != null) {
                            setTouchableInsets.invoke(info, touchableInsetsRegion)
                            (touchableRegionField.get(info) as Region).set(pillTouchRect(view.width, view.height))
                        }
                        null
                    }
                    "equals" -> self === args?.getOrNull(0)
                    "hashCode" -> System.identityHashCode(self)
                    "toString" -> "IslandTouchableRegionListener"
                    else -> null
                }
            }
            addListener.invoke(view.viewTreeObserver, proxy)
            insetsListener = proxy
        }.onFailure { Log.w(TAG, "Touchable region unavailable; overlay stays fully touchable", it) }
    }

    /**
     * The pill's rectangle in the (full-width) window's own coordinates: centred, shifted by the
     * state's offset, tall enough to include the expanded action chips, and grown by a small margin
     * so the rounded edges, drop shadow and tap "boop" scale all stay comfortably tappable.
     */
    private fun pillTouchRect(viewWidth: Int, viewHeight: Int): Rect {
        val dims = if (expanded) layoutState.value.expanded else layoutState.value.collapsed
        val bonusDp = if (expanded) expandedActionsBonusDp() else 0
        val pillWidthPx = displayWidthPx * dims.widthPercent / 100
        val margin = (TOUCH_MARGIN_DP * density).toInt()
        val centerX = viewWidth / 2 + (dims.offsetXDp * density).toInt()
        val topPx = (dims.offsetYDp * density).toInt()
        val bottomPx = ((dims.offsetYDp + dims.heightDp + bonusDp) * density).toInt()
        return Rect(
            (centerX - pillWidthPx / 2 - margin).coerceAtLeast(0),
            (topPx - margin).coerceAtLeast(0),
            (centerX + pillWidthPx / 2 + margin).coerceAtMost(viewWidth),
            (bottomPx + margin).coerceAtMost(if (viewHeight > 0) viewHeight else bottomPx + margin),
        )
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

    /** Height needed to contain just one state's pill (plus room for action chips when expanded). */
    private fun windowHeightPx(layout: IslandLayout, expanded: Boolean): Int {
        val dims = if (expanded) layout.expanded else layout.collapsed
        val bonus = if (expanded) expandedActionsBonusDp() else 0
        return ((dims.offsetYDp + dims.heightDp + bonus + WINDOW_MARGIN_DP) * density).toInt()
    }

    /** The extra height the expanded island claims for its bottom control row, mirroring the composable. */
    private fun expandedActionsBonusDp(): Int {
        val event = currentEvent.value
        val hasActions = behaviourState.value.showActionButtons && event?.actions?.isNotEmpty() == true
        val hasMediaControls = event?.media?.showControls == true
        val hasCallActions = event?.call?.showActions == true && event.actions.isNotEmpty()
        val hasTimerActions = event?.timer?.showActions == true && event.actions.isNotEmpty()
        return if (hasActions || hasMediaControls || hasCallActions || hasTimerActions) {
            expandedActionsExtraDp(appearanceState.value.actionButtonHeightDp)
        } else {
            0
        }
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
            // Window is torn down for the lockscreen — drop signals so nothing is queued behind it.
            if (lockHidden) return@collect
            // Master switch: nothing shows when the cutout is disabled.
            if (!behaviourState.value.cutoutEnabled) return@collect
            // Skip system events the user disabled for the pill.
            if (signal is CutoutSignal.System && eventEnabled[signal.type] == false) return@collect
            // Skip now-playing media when the music tile is turned off.
            if (signal is CutoutSignal.Music && tileEnabled[DynamicTile.MUSIC] == false) return@collect
            // Skip the current call when the phone tile is turned off.
            if (signal is CutoutSignal.Call && tileEnabled[DynamicTile.PHONE] == false) return@collect
            // Skip the running timer when the timer tile is turned off.
            if (signal is CutoutSignal.Timer && tileEnabled[DynamicTile.TIMER] == false) return@collect

            // Expand for a notification (when configured) or the music / phone tile, so its details
            // show. The timer rests collapsed — it is the countdown pill; a tap opens its controls.
            val autoExpand = when (signal) {
                is CutoutSignal.Notification -> behaviourState.value.notificationsAutoExpand
                is CutoutSignal.Music -> true
                is CutoutSignal.Call -> true
                is CutoutSignal.Timer -> false
                is CutoutSignal.System -> false
            }
            // Remember the system event (if any) so its auto-dismiss honours its per-event duration.
            currentSystemEventType = (signal as? CutoutSignal.System)?.type
            forcedExpanded.value = null
            expanded = autoExpand
            currentEvent.value = resolver.resolve(
                signal,
                customIcons,
                musicSettings,
                phoneSettings,
                timerSettings,
                eventDynamicColor,
                eventDynamicColorRole,
                eventDynamicColorOpacity,
                eventAnimatedIcons,
                eventAnimatedIconLoops,
            ).copy(initiallyExpanded = autoExpand)
            syncWindowHeight()
            // A music/call signal is only emitted while that tile is live, so pin it up rather than
            // starting the auto-dismiss timer — it stays for as long as playback / the call lasts.
            when (signal) {
                is CutoutSignal.Music -> {
                    musicPlaying = true
                    lastMusicEvent = currentEvent.value
                    dismissJob?.cancel()
                }

                is CutoutSignal.Call -> {
                    callActive = true
                    lastCallEvent = currentEvent.value
                    dismissJob?.cancel()
                }

                is CutoutSignal.Timer -> {
                    timerActive = true
                    lastTimerEvent = currentEvent.value
                    dismissJob?.cancel()
                }

                else -> scheduleDismiss()
            }
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
            // While music plays or a call is live, keep the collapsed pill up instead of dismissing.
            isPinnedLiveTile() -> dismissJob?.cancel()
            // Shrinking back from expanded and configured to vanish rather than stay.
            wasExpanded && behaviourState.value.expandedDisappearOnShrink -> {
                dismissJob?.cancel()
                currentEvent.value = null
            }

            else -> scheduleDismiss()
        }
    }

    /**
     * Fire the current notification's tap action and dismiss the island, mirroring what tapping
     * the real notification does.
     */
    private fun onActivate() {
        val intent = currentEvent.value?.contentIntent
        dismissIsland()
        intent?.let(::sendPendingIntent)
    }

    /**
     * Swipe-to-dismiss: hide the island and, when it mirrors a real notification, clear that
     * notification from the system too (like swiping it away in the shade).
     */
    private fun onDismiss() {
        currentEvent.value?.notificationKey?.let { CutoutNotificationListenerService.dismiss(it) }
        dismissIsland()
    }

    /** Fire one of the notification's action buttons, then dismiss the island. */
    private fun onAction(action: IslandAction) {
        // Timer actions act on the clock's own countdown notification. A destructive one (Reset / Stop)
        // ends the timer, so dismiss the pill right away for instant feedback — like a call's hang-up —
        // rather than letting it linger until the removed notification trips the auto-dismiss timer.
        // The others (Pause / Resume / Add 1 min) only change a running timer, so keep the pill up.
        if (currentEvent.value?.timer != null) {
            if (action.destructive) dismissIsland()
            sendPendingIntent(action.intent)
            return
        }
        dismissIsland()
        sendPendingIntent(action.intent)
    }

    /**
     * Send a typed reply through the action's intent by packing the text into the [RemoteInput]s
     * the action declared, then dismiss the island (the message is on its way).
     */
    private fun onReply(action: IslandAction, text: String) {
        val reply = action.reply ?: return
        dismissIsland()
        val fillIn = Intent()
        val results = Bundle().apply { putCharSequence(reply.resultKey, text) }
        RemoteInput.addResultsToIntent(reply.remoteInputs.toTypedArray(), fillIn, results)
        sendPendingIntent(action.intent, fillIn)
    }

    /**
     * A reply field opened or closed. The overlay window is normally non-focusable so it never
     * steals input; while typing we clear that flag so the soft keyboard can reach the field, and
     * pause auto-dismiss so the island can't vanish mid-message.
     */
    private fun onReplyActive(active: Boolean) {
        if (active) dismissJob?.cancel()
        setFocusable(active)
    }

    private fun setFocusable(focusable: Boolean) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val newFlags = if (focusable) params.flags and flag.inv() else params.flags or flag
        if (newFlags != params.flags) {
            params.flags = newFlags
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    /**
     * Cancel any pending auto-dismiss and hide the island immediately. If the event being hidden was
     * a notification/system event that had briefly taken over from live music, return to the music
     * pill — but only after a short beat, so it eases back in rather than snapping in the instant the
     * interruption clears (which read as laggy). Re-checks playback after the delay in case it ended.
     */
    private fun dismissIsland() {
        dismissJob?.cancel()
        forcedExpanded.value = null
        expanded = false
        val returnToLive = livePillToReturnTo() != null
        currentEvent.value = null
        syncWindowHeight()
        if (returnToLive) {
            dismissJob = scope.launch {
                delay(MUSIC_RETURN_DELAY_MS)
                livePillToReturnTo()?.let { pill ->
                    forcedExpanded.value = null
                    expanded = false
                    currentEvent.value = pill
                    syncWindowHeight()
                }
            }
        }
    }

    /**
     * The collapsed live-tile pill to fall back to when an interrupting event is hidden, or null to
     * clear the island. A live call takes precedence over music. Each returns its pill only when the
     * hidden event wasn't a live pill itself, that tile is still live, and the tile is enabled.
     */
    private fun livePillToReturnTo(): IslandEvent? =
        callPillToReturnTo() ?: musicPillToReturnTo() ?: timerPillToReturnTo()

    /** True while the shown event is itself a live tile (so we never "return" on top of one). */
    private fun showingLiveTile(): Boolean = currentEvent.value?.let {
        it.media != null || it.call != null || it.timer != null
    } == true

    private fun musicPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!musicPlaying) return null
        if (tileEnabled[DynamicTile.MUSIC] == false) return null
        return lastMusicEvent?.copy(initiallyExpanded = false)
    }

    private fun callPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!callActive) return null
        if (tileEnabled[DynamicTile.PHONE] == false) return null
        return lastCallEvent?.copy(initiallyExpanded = false)
    }

    private fun timerPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!timerActive) return null
        if (tileEnabled[DynamicTile.TIMER] == false) return null
        return lastTimerEvent?.copy(initiallyExpanded = false)
    }

    /**
     * Fired from an accessibility overlay (not a foreground activity), so on Android 14+ we must
     * explicitly opt the pending intent into starting an activity from the background, or the
     * launch is silently dropped.
     */
    private fun sendPendingIntent(intent: PendingIntent, fillIn: Intent? = null) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                    .toBundle()
                intent.send(context, 0, fillIn, null, null, null, options)
            } else {
                intent.send(context, 0, fillIn)
            }
        }.onFailure { Log.w(TAG, "Failed to send pending intent", it) }
    }

    private fun scheduleDismiss() {
        dismissJob?.cancel()
        // Never time out a live cutout while it's active — it stays until playback / the call stops.
        if (isPinnedLiveTile()) return
        // A system event with its own duration override wins; everything else uses the global normal.
        val seconds = currentSystemEventType?.let { eventDurations[it] }
            ?: behaviourState.value.normalDurationSeconds
        dismissJob = scope.launch {
            delay(seconds * 1_000L)
            val livePill = livePillToReturnTo()
            when {
                // Return to the pinned preview if settings is still open.
                previewPinned -> {
                    expanded = previewExpanded
                    forcedExpanded.value = previewExpanded
                    currentEvent.value = previewEvent
                }
                // A live tile outlived an interrupting event — fall back to its pill, collapsed.
                livePill != null -> {
                    expanded = false
                    forcedExpanded.value = null
                    currentEvent.value = livePill
                }
                else -> {
                    expanded = false
                    forcedExpanded.value = null
                    currentEvent.value = null
                }
            }
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
        const val TAG = "IslandOverlay"
        const val WINDOW_MARGIN_DP = 24

        // Slack around the pill's touchable rectangle so its rounded edges, shadow and tap "boop"
        // scale stay tappable — kept small so the shade-pull area beside the pill stays free.
        const val TOUCH_MARGIN_DP = 12

        // Hold the (larger) expanded window size until the pill has finished its ~220ms collapse
        // animation, then shrink — so the collapse never clips and the freed area becomes tappable.
        const val WINDOW_SHRINK_DELAY_MS = 300L

        // Beat between a dismissed interruption fading out and the music pill easing back in, so the
        // hand-off doesn't feel like an instant, janky swap.
        const val MUSIC_RETURN_DELAY_MS = 350L
    }
}
