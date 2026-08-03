package com.ekoehler.expressivecutout.overlay

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CenterShortcutExecutor
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.ForegroundAppBus
import com.ekoehler.expressivecutout.core.NowPlayingBus
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.core.RunningTimerBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.AppPreferences
import com.ekoehler.expressivecutout.data.AppearancePreferences
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.CenterShortcut
import com.ekoehler.expressivecutout.data.EmptyClickAction
import com.ekoehler.expressivecutout.data.GlobalAction
import com.ekoehler.expressivecutout.data.HorizontalCutoutMode
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.DynamicTilePreferences
import com.ekoehler.expressivecutout.data.EventPreferences
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.data.asCallCutout
import com.ekoehler.expressivecutout.data.AssistantTilePreferences
import com.ekoehler.expressivecutout.data.AssistantTileSettings
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
import kotlin.math.abs

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
    private val assistantTilePreferences = AssistantTilePreferences(context)
    private val appPreferences = AppPreferences(context)
    private val density = context.resources.displayMetrics.density

    // Full display width, used by the island to size itself as a percentage of the screen. Read from
    // the *current* window metrics so it follows the device between portrait and landscape — recomputed
    // on rotation by [onOrientationChanged]. (maximumWindowMetrics would stay pinned to the natural
    // orientation, leaving the landscape pill and its touchable-region carve-out mis-sized.) The px value
    // is read live by the touchable region; the dp value is a flow so the pill re-sizes on rotation
    // without recreating the ComposeView.
    private var displayWidthPx: Int = computeDisplayWidthPx()
    private val displayWidthDp = MutableStateFlow((displayWidthPx / density).toInt())

    // The orientation the live window geometry was built for, so [onOrientationChanged] only reacts to
    // an actual portrait <-> landscape flip. Also the single source of truth for the current
    // orientation, ready for orientation-specific layout settings later.
    private var currentOrientation: Int = context.resources.configuration.orientation
    private val orientationState = MutableStateFlow(currentOrientation)

    private val displayHeightPx: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            context.resources.displayMetrics.heightPixels
        }
    private val displayHeightDp: Int = (displayHeightPx / density).toInt()

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
    private var eventColors: Map<SystemEventType, CutoutColor> = emptyMap()
    // The system event currently on the pill, so its auto-dismiss uses that event's own duration
    // override (null while a notification or live tile is showing → the global normal duration).
    private var currentSystemEventType: SystemEventType? = null
    private var eventDynamicColor: Boolean = false
    private var eventDynamicColorRole: DynamicRole = DynamicRole.PRIMARY
    private var eventDynamicColorOpacity: Float = 1f
    private var tileEnabled: Map<DynamicTile, Boolean> = emptyMap()
    // Packages the user muted on the Apps screen: nothing they post reaches the cutout.
    private var disabledApps: Set<String> = emptySet()
    // Packages allowed on the cutout but never allowed to expand it on their own.
    private var normalOnlyApps: Set<String> = emptySet()
    private var musicSettings: MusicTileSettings = MusicTileSettings()
    private var phoneSettings: PhoneTileSettings = PhoneTileSettings()
    private var timerSettings: TimerTileSettings = TimerTileSettings()
    private var assistantSettings: AssistantTileSettings = AssistantTileSettings()
    private var previewPinned = false
    private var previewExpanded = false
    private var expanded = false
    // True while a media session is actively playing; keeps the music cutout pinned up (no
    // auto-dismiss) for as long as playback lasts.
    private var musicPlaying = false
    // The last resolved music event, so the pill can return after a notification/system event that
    // briefly took over the cutout while playback carried on.
    private var lastMusicEvent: IslandEvent? = null
    // The package of the app currently in the foreground, from the accessibility service. Drives the
    // "Visible in player app" option: when off, the music cutout hides while this matches the player.
    private var foregroundPackage: String? = null
    // True while the music cutout is being held hidden because the playing app is in the foreground
    // and "Visible in player app" is off, so it can be brought back when the user leaves that app.
    private var playerAppHidden = false
    // True while the phone cutout is being held hidden because the phone app is full screen in the
    // foreground, so it can be brought back when the user leaves the phone app.
    private var phoneAppHidden = false
    // True while a phone call is present; keeps the call cutout pinned up (no auto-dismiss) for the
    // whole call, and — like [lastMusicEvent] — lets the pill return after an interruption.
    private var callActive = false
    private var lastCallEvent: IslandEvent? = null
    // True while a countdown timer is running; keeps the timer cutout pinned up (no auto-dismiss) for
    // the whole countdown, and — like [lastCallEvent] — lets the pill return after an interruption.
    private var timerActive = false
    private var lastTimerEvent: IslandEvent? = null
    private var assistantActive = false
    private var lastAssistantEvent: IslandEvent? = null

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

    // True while the window has been torn down because "hide on lockscreen" or "hide in landscape" is active.
    // Guards signal handling and drives whether the window currently exists.
    private var overlayHidden = false
    private var savedEventBeforeHide: IslandEvent? = null

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
        observeEventColors()
        observeEventDynamicColor()
        observeEventDynamicColorRole()
        observeEventDynamicColorOpacity()
        observeTilePreferences()
        observeAppPreferences()
        observeMusicSettings()
        observePhoneSettings()
        observeTimerSettings()
        observeAssistantSettings()
        observeNowPlaying()
        observeForegroundApp()
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
     * Enforce "hide on lockscreen" and "hide in landscape" by fully adding or removing the overlay window.
     * Tearing the window down — rather than just hiding it — means nothing is composed or drawn while
     * locked or in landscape, and gesture areas stay completely unblocked.
     */
    private fun applyLockVisibility() {
        val shouldHideLock = behaviourState.value.hideOnLockscreen &&
            keyguardManager?.isKeyguardLocked == true
        val isLandscapeHidden = behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.HIDDEN ||
            behaviourState.value.hideInLandscape
        val shouldHideLandscape = isLandscapeHidden &&
            currentOrientation == Configuration.ORIENTATION_LANDSCAPE
        val shouldHide = shouldHideLock || shouldHideLandscape

        when {
            shouldHide && !overlayHidden -> {
                overlayHidden = true
                dismissJob?.cancel()
                windowResizeJob?.cancel()
                if (currentEvent.value != null) {
                    savedEventBeforeHide = currentEvent.value
                }
                currentEvent.value = null
                removeOverlay()
            }

            !shouldHide && overlayHidden -> {
                overlayHidden = false
                addOverlay()
                syncWindowSize()
                restoreActiveState()
            }
        }
    }

    private fun restoreActiveState() {
        when {
            previewPinned -> {
                dismissJob?.cancel()
                forcedExpanded.value = previewExpanded
                expanded = previewExpanded
                currentEvent.value = previewEvent
            }
            callActive && lastCallEvent != null -> {
                dismissJob?.cancel()
                expanded = false
                currentEvent.value = lastCallEvent
            }
            musicPlaying && lastMusicEvent != null && !playerAppHidden -> {
                dismissJob?.cancel()
                currentEvent.value = lastMusicEvent
            }
            timerActive && lastTimerEvent != null -> {
                dismissJob?.cancel()
                currentEvent.value = lastTimerEvent
            }
            savedEventBeforeHide != null -> {
                dismissJob?.cancel()
                currentEvent.value = savedEventBeforeHide
                savedEventBeforeHide = null
                scheduleDismiss()
            }
        }
    }

    /** The current window width in px, following the device's live orientation. */
    private fun computeDisplayWidthPx(): Int {
        val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val metrics = context.resources.displayMetrics
            metrics.widthPixels to metrics.heightPixels
        }
        // In landscape mode, size relative to portrait width so the pill remains reasonably sized
        // and leaves ample space on either side of the top edge for the notification shade pull.
        return if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            minOf(width, height)
        } else {
            width
        }
    }

    /**
     * React to a device rotation. The pill sizes itself as a percentage of the screen width, so on a
     * portrait <-> landscape flip both the pill and the window that hugs it ([syncWindowSize]) must be
     * recomputed for the new width — otherwise the landscape window is sized for portrait and the band
     * ends up covering the shade-pull area. Recompute the width, let the pill re-size via [displayWidthDp],
     * and re-hug the window; the window is updated in place, never torn down (re-adding it leaves the
     * overlay fully touchable).
     *
     * Gated on an actual portrait <-> landscape flip via [currentOrientation]; other configuration
     * changes (font scale, night mode, …) are ignored. This is the hook for orientation-specific layout
     * settings later — [currentOrientation] is the single place the live orientation is tracked.
     */
    fun onOrientationChanged(orientation: Int) {
        if (orientation == currentOrientation) return
        currentOrientation = orientation
        orientationState.value = orientation
        displayWidthPx = computeDisplayWidthPx()
        displayWidthDp.value = (displayWidthPx / density).toInt()
        applyLockVisibility()
        if (overlayHidden) return
        syncWindowSize()
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
                val widthDp by displayWidthDp.collectAsStateWithLifecycle()
                val orientation by orientationState.collectAsStateWithLifecycle()
                val isNoExpandLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    (behaviour.horizontalCutoutMode == HorizontalCutoutMode.NORMAL_ONLY ||
                     behaviour.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA)
                val effectiveForced = if (isNoExpandLandscape) false else forced
                val isStickToCamera = orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    behaviour.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
                val rot270 = isRotation270()

                // Theme the overlay so the "Dynamic color for all events" badge picks up the app's
                // real primary / on-primary (Material You or the brand fallback), not the M3 baseline.
                ExpressiveCutoutTheme {
                    DynamicIsland(
                        event = event,
                        collapsed = layout.collapsed,
                        expanded = layout.expanded,
                        displayWidthDp = widthDp,
                        forcedExpanded = effectiveForced,
                        isStickToCamera = isStickToCamera,
                        isRotation270 = rot270,
                        offsetYDp = layout.collapsed.offsetYDp,
                        animationStyle = behaviour.animationStyle,
                        animationSpeed = behaviour.animationSpeed,
                        animationBounce = behaviour.animationBounce,
                        animationDurationMs = behaviour.animationDurationMs,
                        autoCollapse = behaviour.expandedAutoCollapse,
                        autoCollapseMs = behaviour.expandedCollapseSeconds * 1_000L,
                        appearance = appearance,
                        showActions = behaviour.showActionButtons,
                        shrinkOnSwipeUp = behaviour.shrinkOnSwipeUp,
                        swipeToDismiss = behaviour.swipeToDismiss,
                        swipeDismissDirection = behaviour.swipeDismissDirection,
                        swipeDismissTarget = behaviour.swipeDismissTarget,
                        showsWhenEmpty = behaviour.showsWhenEmpty && behaviour.cutoutEnabled,
                        emptyIcon = behaviour.showsWhenEmptyIcon.takeIf { behaviour.showsWhenEmptyShowIcon },
                        emptyIconColor = behaviour.showsWhenEmptyIconColor,
                        emptyOpensCenter = behaviour.showsWhenEmptyClickAction == EmptyClickAction.OPEN_CENTER,
                        centerShortcuts = behaviour.centerShortcuts,
                        centerShowLabels = behaviour.centerShowLabels,
                        centerFillContainers = behaviour.centerFillContainers,
                        centerThemedIcons = behaviour.centerThemedIcons,
                        actionButtonAnimation = behaviour.actionButtonAnimation,
                        onEmptyClick = ::onEmptyClick,
                        onCenterShortcut = ::onCenterShortcut,
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
            syncWindowSize()
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

    private fun observeEventColors() = scope.launch {
        eventPreferences.colors.collect { eventColors = it }
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

    private fun observeAppPreferences() {
        scope.launch { appPreferences.disabledPackages.collect { disabledApps = it } }
        scope.launch { appPreferences.normalOnlyPackages.collect { normalOnlyApps = it } }
    }

    private fun observeMusicSettings() = scope.launch {
        musicTilePreferences.settings.collect {
            musicSettings = it
            // Toggling "Visible in player app" should take effect immediately, even mid-playback.
            applyPlayerAppVisibility()
        }
    }

    private fun observePhoneSettings() = scope.launch {
        phoneTilePreferences.settings.collect { phoneSettings = it }
    }

    private fun observeTimerSettings() = scope.launch {
        timerTilePreferences.settings.collect { timerSettings = it }
    }

    private fun observeAssistantSettings() = scope.launch {
        assistantTilePreferences.settings.collect { assistantSettings = it }
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
            // Playback starting/stopping (or switching apps) can change whether the player is the
            // foreground app, so re-evaluate the "Visible in player app" hide.
            applyPlayerAppVisibility()
            // Only steer the music pill; leave notifications/system events to their own timers.
            if (previewPinned || currentEvent.value?.media == null) return@collect
            if (musicPlaying) dismissJob?.cancel() else scheduleDismiss()
        }
    }

    /**
     * Track the foreground app so the music cutout can hide while the playing app is open (the
     * "Visible in player app" option). The package name arrives from the accessibility service's
     * window-state-changed events; no window content is read.
     */
    private fun observeForegroundApp() = scope.launch {
        ForegroundAppBus.packageName.collect { pkg ->
            foregroundPackage = pkg
            applyPlayerAppVisibility()
            applyPhoneAppVisibility()
        }
    }

    /** True when the app handling the live call is the one in the foreground. */
    private fun phoneAppInForeground(): Boolean {
        val phonePkg = OnCallBus.state.value?.packageName ?: return false
        if (phonePkg == context.packageName) return false
        return foregroundPackage != null && foregroundPackage == phonePkg
    }

    /**
     * The phone cutout should be held hidden right now: a call is active and the phone app is
     * full screen in the foreground.
     */
    private fun shouldHideForPhoneApp(): Boolean =
        callActive && phoneAppInForeground()

    /**
     * Clear the call cutout while the phone app is in the foreground full screen; when the user
     * leaves that app while a call is still running, bring the call cutout back.
     */
    private fun applyPhoneAppVisibility() {
        if (previewPinned || overlayHidden) return
        if (shouldHideForPhoneApp()) {
            if (phoneAppHidden) return
            phoneAppHidden = true
            if (currentEvent.value?.call != null) {
                dismissJob?.cancel()
                forcedExpanded.value = null
                expanded = false
                currentEvent.value = null
                syncWindowSize()
            }
        } else {
            if (!phoneAppHidden) return
            phoneAppHidden = false
            if (currentEvent.value == null) {
                callPillToReturnTo()?.let { pill ->
                    forcedExpanded.value = null
                    expanded = false
                    currentEvent.value = pill
                    syncWindowSize()
                }
            }
        }
    }

    /** True when the app currently playing music is the one in the foreground. */
    private fun musicPlayerInForeground(): Boolean {
        val player = NowPlayingBus.state.value?.packageName ?: return false
        return foregroundPackage != null && foregroundPackage == player
    }

    /**
     * The music cutout should be held hidden right now: "Visible in player app" is off, music is
     * playing, and the playing app is the one on screen.
     */
    private fun shouldHideForPlayerApp(): Boolean =
        !musicSettings.visibleInPlayerApp && musicPlaying && musicPlayerInForeground()

    /**
     * Enforce "Visible in player app": while the playing app is in the foreground and the option is
     * off, clear the music cutout (only the music pill — notifications and other events are left
     * alone); when the user leaves that app, bring the music pill back if playback is still live and
     * nothing else has taken the cutout. Idempotent via [playerAppHidden].
     */
    private fun applyPlayerAppVisibility() {
        if (previewPinned || overlayHidden) return
        if (shouldHideForPlayerApp()) {
            if (playerAppHidden) return
            playerAppHidden = true
            if (currentEvent.value?.media != null) {
                dismissJob?.cancel()
                forcedExpanded.value = null
                expanded = false
                currentEvent.value = null
                syncWindowSize()
            }
        } else {
            if (!playerAppHidden) return
            playerAppHidden = false
            if (currentEvent.value == null) {
                musicPillToReturnTo()?.let { pill ->
                    forcedExpanded.value = null
                    expanded = false
                    currentEvent.value = pill
                    syncWindowSize()
                }
            }
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
            if (call == null) {
                lastCallEvent = null
                phoneAppHidden = false
            }
            applyPhoneAppVisibility()
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

    /** The assistant cutout should stay pinned up (no auto-dismiss) while assistant is active. */
    private fun isPinnedAssistant(): Boolean = assistantActive && currentEvent.value?.assistant != null

    /** Any live tile (music, a call, a running timer, or assistant) is currently pinned up. */
    private fun isPinnedLiveTile(): Boolean = isPinnedMusic() || isPinnedCall() || isPinnedTimer() || isPinnedAssistant()

    private fun observeLayout() = scope.launch {
        layoutPreferences.layout.collect { layout ->
            layoutState.value = layout
            syncWindowSize()
        }
    }

    /**
     * Only intercept touches while the island is actually on screen — plus while the resting empty
     * cutout is showing, so tapping it still gives the "boop" scale feedback. The touchable region
     * ([pillTouchRect]) keeps that to the pill's own rectangle, so the shade pull beside it is
     * unaffected; the pill's own footprint does stop passing touches through.
     */
    private fun observeVisibility() = scope.launch {
        combine(currentEvent, behaviourState, ::Pair).collect { (event, behaviour) ->
            setTouchable(event != null || (behaviour.showsWhenEmpty && behaviour.cutoutEnabled))
        }
    }

    /**
     * Resize the window to hug the current island state (collapsed vs expanded) in *both* width and
     * height, so the empty band around the pill stops swallowing touches — most importantly the
     * notification-shade pull, which happens beside the pill. Hugging the width (not spanning the whole
     * screen) is what keeps the shade reachable in landscape: the wide areas either side of the pill are
     * then outside the window entirely and fall through, rather than relying on the touchable-region
     * carve-out — which the framework does not honour for this overlay in landscape.
     *
     * The pill itself is still animated inside Compose — this only changes the window at the two rest
     * states, never per frame, so the expand/collapse animation stays smooth.
     *
     * Width is held at the widest state (never varied on expand/collapse): the window is centred, so
     * resizing its width mid-animation would re-centre it a frame out of step with the pill and make the
     * pill appear to slide sideways. Height is safe to vary because the window is top-anchored (it grows
     * downward), and is grown immediately on expand but shrunk only after the collapse animation finishes,
     * so the window always has room for the pill and never clips it mid-animation.
     */
    private fun syncWindowSize() {
        requestWindowSize(
            windowWidthPx(layoutState.value),
            windowHeightPx(layoutState.value, expanded),
        )
    }

    private fun requestWindowSize(targetWidthPx: Int, targetHeightPx: Int) {
        val params = layoutParams ?: return
        windowResizeJob?.cancel()
        val currentWidth = params.width
        val currentHeight = params.height
        // MATCH_PARENT is -1; treat it as "already large enough" so the first sizing shrinks straight to fit.
        val grownWidth = if (currentWidth < 0) targetWidthPx else maxOf(targetWidthPx, currentWidth)
        val grownHeight = maxOf(targetHeightPx, currentHeight)
        resizeWindow(grownWidth, grownHeight)
        val shrinks = (currentWidth >= 0 && targetWidthPx < currentWidth) || targetHeightPx < currentHeight
        if (shrinks) {
            windowResizeJob = scope.launch {
                delay(WINDOW_SHRINK_DELAY_MS)
                resizeWindow(targetWidthPx, targetHeightPx)
            }
        }
    }

    private fun resizeWindow(targetWidthPx: Int, targetHeightPx: Int) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val targetGravity = computeWindowGravity()
        if (params.width != targetWidthPx || params.height != targetHeightPx || params.gravity != targetGravity) {
            params.width = targetWidthPx
            params.height = targetHeightPx
            params.gravity = targetGravity
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
        val isStickToCamera = orientationState.value == Configuration.ORIENTATION_LANDSCAPE &&
            behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
        if (isStickToCamera) {
            return Rect(0, 0, viewWidth, viewHeight)
        }
        val dims = effectiveDims(layoutState.value, expanded)
        val bonusDp = currentHeightBonusDp(expanded)
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
        val isStickToCamera = orientationState.value == Configuration.ORIENTATION_LANDSCAPE &&
            behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
        if (isStickToCamera) {
            val islandLengthDp = displayWidthDp.value * (layout.collapsed.widthPercent / 100f)
            return ((islandLengthDp + TOUCH_MARGIN_DP * 2) * density).toInt()
        }
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
        val isStickToCamera = orientationState.value == Configuration.ORIENTATION_LANDSCAPE &&
            behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
        val dims = effectiveDims(layout, expanded)
        if (isStickToCamera) {
            val islandLengthDp = displayWidthDp.value * (dims.widthPercent / 100f)
            return ((islandLengthDp + TOUCH_MARGIN_DP * 2) * density).toInt()
        }
        val bonus = currentHeightBonusDp(expanded)
        return ((dims.offsetYDp + dims.heightDp + bonus + WINDOW_MARGIN_DP) * density).toInt()
    }

    /** Wide enough for whichever state is widest — used for the initial, safe window size. */
    private fun windowWidthPx(layout: IslandLayout): Int =
        maxOf(windowWidthPx(layout, expanded = false), windowWidthPx(layout, expanded = true))

    /**
     * Width needed to contain just one state's pill, centred, with room for its horizontal offset and a
     * margin on each side (for the rounded edges, shadow and tap "boop" scale). Capped at the display
     * width so a very wide pill falls back to a full-width band. Keeping this to the pill (rather than the
     * whole screen) is what lets the notification shade be pulled from beside the pill in landscape.
     */
    private fun windowWidthPx(layout: IslandLayout, expanded: Boolean): Int {
        val isStickToCamera = orientationState.value == Configuration.ORIENTATION_LANDSCAPE &&
            behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
        val dims = effectiveDims(layout, expanded)
        if (isStickToCamera) {
            val totalDp = dims.offsetYDp + dims.heightDp + TOUCH_MARGIN_DP * 2
            return (totalDp * density).toInt()
        }
        val islandWidthDp = displayWidthDp.value * (dims.widthPercent / 100f)
        return ((islandWidthDp + WINDOW_MARGIN_DP * 2) * density).toInt()
    }

    /**
     * The dimensions the island is actually drawn at right now: the expanded state when expanded, the
     * bigger call cutout when the shown event is a phone call (it has no expanded state), otherwise
     * the normal collapsed pill. Keeps the window height and touchable region in step with what
     * [DynamicIsland] renders.
     */
    private fun effectiveDims(layout: IslandLayout, expanded: Boolean): IslandDimensions {
        val event = currentEvent.value
        return when {
            expanded -> layout.expanded
            event?.call != null -> {
                val incoming = OnCallBus.state.value?.ongoing == false
                if (isTwoRowCall()) {
                    // The two-row incoming layout starts from the expanded cutout (grown by the button
                    // row via currentHeightBonusDp).
                    layout.expanded
                } else {
                    // Match the pill's name-driven width so the trailing call button(s) stay tappable:
                    // one for a connected call's hang-up, two for a one-line incoming's decline + answer.
                    val trailingButtons = when {
                        !(event.call.showActions && event.actions.isNotEmpty()) -> 0
                        incoming -> 2
                        else -> 1
                    }
                    layout.collapsed.asCallCutout(
                        callCutoutWidthPercent(event.label, trailingButtons, incoming, displayWidthDp.value, density),
                    )
                }
            }
            else -> layout.collapsed
        }
    }

    /**
     * The extra height the currently-drawn state claims below its base dimensions: the expanded island's
     * action row when expanded, or the incoming two-row call layout's button row. Mirrors the height
     * bonus [DynamicIsland] applies, so the window and touchable region stay as tall as what it renders.
     */
    private fun currentHeightBonusDp(expanded: Boolean): Int {
        val event = currentEvent.value
        if (expanded && event?.assistant != null && event.assistant.displayAnswerInCutout) {
            val maxCutoutDp = (displayHeightDp * event.assistant.maxCutoutHeightPercent / 100)
            return maxOf(expandedActionsBonusDp(), maxCutoutDp - layoutState.value.expanded.heightDp)
        }
        return when {
            // The empty pill's expanded "center" (no event) claims room for its shortcut row.
            expanded && event == null &&
                behaviourState.value.showsWhenEmptyClickAction == EmptyClickAction.OPEN_CENTER ->
                CENTER_SHORTCUTS_EXTRA_DP
            expanded -> expandedActionsBonusDp()
            isTwoRowCall() -> callIncomingExtraDp()
            else -> 0
        }
    }

    /** Whether the shown event is an incoming call rendered in the taller two-row layout. */
    private fun isTwoRowCall(): Boolean {
        val event = currentEvent.value ?: return false
        val call = event.call ?: return false
        val incoming = OnCallBus.state.value?.ongoing == false
        return incoming && call.incomingExpandedLayout && call.showActions && event.actions.isNotEmpty()
    }

    /** The extra height the expanded island claims for its bottom control row, mirroring the composable. */
    private fun expandedActionsBonusDp(): Int {
        val event = currentEvent.value
        val hasActions = behaviourState.value.showActionButtons && event?.actions?.isNotEmpty() == true
        val hasMediaControls = event?.media?.showControls == true
        val hasCallActions = event?.call?.showActions == true && event.actions.isNotEmpty()
        val hasTimerActions = event?.timer?.showActions == true && event.actions.isNotEmpty()
        val hasAssistantActions = behaviourState.value.showActionButtons && event?.assistant != null
        return if (hasActions || hasMediaControls || hasCallActions || hasTimerActions || hasAssistantActions) {
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
                val isNoExpandLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE &&
                    (behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.NORMAL_ONLY ||
                     behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA)
                val targetExpanded = if (isNoExpandLandscape) false else expandedTab
                if (pinned) {
                    dismissJob?.cancel()
                    forcedExpanded.value = targetExpanded
                    expanded = targetExpanded
                    currentEvent.value = previewEvent
                } else {
                    forcedExpanded.value = if (isNoExpandLandscape) false else null
                    expanded = false
                    currentEvent.value = null
                }
                syncWindowSize()
            }
    }

    private fun observeSignals() = scope.launch {
        IslandEventBus.signals.collect { signal ->
            // Skip system events the user disabled for the pill.
            if (signal is CutoutSignal.System && eventEnabled[signal.type] == false) return@collect
            // Skip now-playing media when the music tile is turned off.
            if (signal is CutoutSignal.Music && tileEnabled[DynamicTile.MUSIC] == false) return@collect
            // Skip the current call when the phone tile is turned off.
            if (signal is CutoutSignal.Call && tileEnabled[DynamicTile.PHONE] == false) return@collect
            // Skip the running timer when the timer tile is turned off.
            if (signal is CutoutSignal.Timer && tileEnabled[DynamicTile.TIMER] == false) return@collect
            // Skip assistant responses when the assistant tile is turned off.
            if (signal is CutoutSignal.Assistant && tileEnabled[DynamicTile.ASSISTANT] == false) return@collect
            // Skip anything posted by an app the user muted on the Apps screen.
            if (signal.sourcePackage() in disabledApps) return@collect

            if (signal is CutoutSignal.Assistant && !signal.active) {
                assistantActive = false
                lastAssistantEvent = null
                if (currentEvent.value?.assistant != null) {
                    dismissIsland()
                }
                return@collect
            }

            val isNoExpandLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE &&
                (behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.NORMAL_ONLY ||
                 behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA)

            val rawAutoExpand = when (signal) {
                is CutoutSignal.Notification -> behaviourState.value.notificationsAutoExpand
                is CutoutSignal.Music -> musicSettings.expandOnPlay
                is CutoutSignal.Assistant -> assistantSettings.displayAnswerInCutout
                // The phone tile has no expanded state — it is shown as one bigger normal cutout.
                is CutoutSignal.Call -> false
                is CutoutSignal.Timer -> false
                is CutoutSignal.System -> false
            }
            // "Normal only": this app's pill has no expanded state at all. Suppressing auto-expand
            // here keeps the window from ever being sized for it; the flag carried on the event is
            // what stops a tap toggling it open (and opens the app instead) — see [IslandEvent.normalOnly].
            val normalOnly = signal.sourcePackage() in normalOnlyApps
            val autoExpand = if (isNoExpandLandscape || normalOnly) false else rawAutoExpand

            val resolvedEvent = resolver.resolve(
                signal,
                customIcons,
                musicSettings,
                phoneSettings,
                timerSettings,
                assistantSettings,
                eventDynamicColor,
                eventDynamicColorRole,
                eventDynamicColorOpacity,
                eventAnimatedIcons,
                eventAnimatedIconLoops,
                eventColors,
            ).copy(initiallyExpanded = autoExpand, normalOnly = normalOnly)

            if (overlayHidden) {
                if (behaviourState.value.cutoutEnabled) {
                    savedEventBeforeHide = resolvedEvent
                    when (signal) {
                        is CutoutSignal.Music -> {
                            musicPlaying = true
                            lastMusicEvent = resolvedEvent
                        }
                        is CutoutSignal.Call -> {
                            callActive = true
                            lastCallEvent = resolvedEvent
                        }
                        is CutoutSignal.Timer -> {
                            timerActive = true
                            lastTimerEvent = resolvedEvent
                        }
                        else -> {}
                    }
                }
                return@collect
            }

            if (!behaviourState.value.cutoutEnabled) return@collect

            // Remember the system event (if any) so its auto-dismiss honours its per-event duration.
            currentSystemEventType = (signal as? CutoutSignal.System)?.type
            forcedExpanded.value = if (isNoExpandLandscape) false else null
            expanded = autoExpand
            currentEvent.value = resolvedEvent
            syncWindowSize()
            // A music/call/assistant signal is only emitted while that tile is live, so pin it up rather than
            // starting the auto-dismiss timer — it stays for as long as playback / the call / assistant lasts.
            when (signal) {
                is CutoutSignal.Music -> {
                    musicPlaying = true
                    lastMusicEvent = resolvedEvent
                    dismissJob?.cancel()
                }

                is CutoutSignal.Call -> {
                    callActive = true
                    lastCallEvent = resolvedEvent
                    dismissJob?.cancel()
                }

                is CutoutSignal.Timer -> {
                    timerActive = true
                    lastTimerEvent = resolvedEvent
                    dismissJob?.cancel()
                }

                is CutoutSignal.Assistant -> {
                    assistantActive = true
                    lastAssistantEvent = currentEvent.value
                    dismissJob?.cancel()
                }

                else -> scheduleDismiss()
            }
            // Playback is now tracked (musicPlaying / lastMusicEvent), so if "Visible in player app"
            // is off and the playing app is on screen, hide the music cutout we just showed. It
            // returns via musicPillToReturnTo() once the user leaves that app.
            if (signal is CutoutSignal.Music && shouldHideForPlayerApp()) {
                playerAppHidden = true
                currentEvent.value = null
                removeOverlay()
            }
            if (signal is CutoutSignal.Call && shouldHideForPhoneApp()) {
                phoneAppHidden = true
                forcedExpanded.value = null
                expanded = false
                currentEvent.value = null
                syncWindowSize()
            }
        }
    }

    /** Pause auto-dismiss while expanded; on collapse either hide or return to the normal cutout. */
    private fun onExpandedChanged(isExpanded: Boolean) {
        val isNoExpandLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE &&
            (behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.NORMAL_ONLY ||
             behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA)
        val targetExpanded = if (isNoExpandLandscape) false else isExpanded
        // The resting empty pill's "center" has no event to dismiss — just keep the window and
        // touchable region sized to whatever it's showing (collapsed pill vs. expanded grid).
        if (currentEvent.value == null) {
            expanded = targetExpanded
            // Sync the flashlight state when the center opens so a torch shortcut shows lit/unlit.
            if (targetExpanded) CenterShortcutExecutor.syncTorchState(context)
            syncWindowSize()
            return
        }
        val wasExpanded = expanded
        expanded = targetExpanded
        syncWindowSize()
        when {
            targetExpanded -> dismissJob?.cancel()
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
     * the real notification does. A live tile is the exception: tapping it opens its app (the
     * dialer's in-call screen, the player) but leaves the pill up, since the call/playback is still
     * running and there'd otherwise be no way to bring the tile back. It stays until the call ends
     * or the user swipes it away.
     */
    /**
     * A tap on the resting (empty) pill. Its behaviour is the user's "On click" choice: [OPEN_APP]
     * launches the chosen app; [NONE] (and, for now, the reserved [OPEN_CENTER]) do nothing beyond
     * the press animation the pill already plays.
     */
    private fun onEmptyClick() {
        val behaviour = behaviourState.value
        if (behaviour.showsWhenEmptyClickAction != EmptyClickAction.OPEN_APP) return
        val packageName = behaviour.showsWhenEmptyClickPackage ?: return
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return
        runCatching { context.startActivity(launch) }
    }

    /**
     * Run a shortcut tapped in the expanded center. The composable has already begun collapsing the
     * center as it calls this; for actions that capture or cover the screen (screenshot, power menu)
     * we give that collapse a beat to finish so the overlay isn't in the shot / behind the dialog.
     */
    private fun onCenterShortcut(shortcut: CenterShortcut) {
        val settleFirst = shortcut is CenterShortcut.Global &&
            (shortcut.action == GlobalAction.SCREENSHOT || shortcut.action == GlobalAction.POWER_DIALOG)
        if (settleFirst) {
            scope.launch {
                delay(CENTER_ACTION_SETTLE_MS)
                CenterShortcutExecutor.execute(shortcut, context)
            }
        } else {
            CenterShortcutExecutor.execute(shortcut, context)
        }
    }

    private fun onActivate() {
        val intent = currentEvent.value?.contentIntent
        if (isPinnedLiveTile()) {
            dismissJob?.cancel()
            intent?.let(::sendPendingIntent)
            return
        }
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
            action.intent?.let(::sendPendingIntent)
            return
        }
        dismissIsland()
        action.intent?.let(::sendPendingIntent)
    }

    /**
     * Send a typed reply through the action's intent by packing the text into the [RemoteInput]s
     * the action declared, then dismiss the island (the message is on its way).
     */
    private fun onReply(action: IslandAction, text: String) {
        val reply = action.reply ?: return
        val intent = action.intent ?: return
        dismissIsland()
        val fillIn = Intent()
        val results = Bundle().apply { putCharSequence(reply.resultKey, text) }
        RemoteInput.addResultsToIntent(reply.remoteInputs.toTypedArray(), fillIn, results)
        sendPendingIntent(intent, fillIn)
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
        syncWindowSize()
        if (returnToLive) {
            dismissJob = scope.launch {
                delay(MUSIC_RETURN_DELAY_MS)
                livePillToReturnTo()?.let { pill ->
                    forcedExpanded.value = null
                    expanded = false
                    currentEvent.value = pill
                    syncWindowSize()
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

    /**
     * The app a signal came from, or null for a device-level event (which belongs to no app and is
     * governed by the Events screen instead).
     */
    private fun CutoutSignal.sourcePackage(): String? = when (this) {
        is CutoutSignal.Notification -> packageName
        is CutoutSignal.Music -> packageName
        is CutoutSignal.Call -> packageName
        is CutoutSignal.Timer -> packageName
        is CutoutSignal.Assistant -> packageName
        is CutoutSignal.System -> null
    }

    private fun musicPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!musicPlaying) return null
        if (tileEnabled[DynamicTile.MUSIC] == false) return null
        // Stay hidden while the playing app is in the foreground and "Visible in player app" is off.
        if (shouldHideForPlayerApp()) return null
        return lastMusicEvent?.copy(initiallyExpanded = false)
    }

    private fun callPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!callActive) return null
        if (tileEnabled[DynamicTile.PHONE] == false) return null
        // The dialer stays on the call bus even when muted, so don't bring its pill back.
        if (OnCallBus.state.value?.packageName in disabledApps) return null
        if (shouldHideForPhoneApp()) return null
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
            syncWindowSize()
        }
    }

    private fun computeWindowGravity(): Int {
        if (currentOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            return Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        return when (behaviourState.value.horizontalCutoutMode) {
            HorizontalCutoutMode.STICK_TO_CAMERA -> getLandscapeCameraGravity()
            else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    private fun isRotation270(): Boolean {
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        return display?.rotation == Surface.ROTATION_270
    }

    private fun getLandscapeCameraGravity(): Int {
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        return when (display?.rotation) {
            Surface.ROTATION_90 -> Gravity.LEFT or Gravity.CENTER_VERTICAL
            Surface.ROTATION_270 -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
            else -> Gravity.LEFT or Gravity.CENTER_VERTICAL
        }
    }

    private fun getLandscapeCameraRotation(): Float {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        return when (display?.rotation) {
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_270 -> -90f
            else -> 0f
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        // A fixed band centred at the top, sized to hug the pill (see syncWindowSize) rather than span the
        // whole screen — so the areas either side stay free for the notification-shade pull, in landscape
        // too. Starts non-touchable (nothing showing) and becomes touchable only while the island is
        // visible (so tap-to-expand works).
        return WindowManager.LayoutParams(
            windowWidthPx(IslandLayout.DEFAULT),
            windowHeightPx(IslandLayout.DEFAULT),
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = computeWindowGravity()
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

        // Let the center's collapse begin before a screen-capturing / screen-covering shortcut fires,
        // so the overlay isn't caught in the screenshot or left behind the power dialog.
        const val CENTER_ACTION_SETTLE_MS = 260L
    }
}
