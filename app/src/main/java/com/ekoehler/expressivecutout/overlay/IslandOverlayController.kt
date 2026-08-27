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
import android.view.MotionEvent
import android.view.Surface
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
import com.ekoehler.expressivecutout.core.CenterShortcutExecutor
import com.ekoehler.expressivecutout.core.CutoutMetrics
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.PermissionDotPreviewBus
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
import com.ekoehler.expressivecutout.data.SatellitePosition
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
import com.ekoehler.expressivecutout.data.PermissionDotColors
import com.ekoehler.expressivecutout.data.PermissionDotPosition
import com.ekoehler.expressivecutout.data.PermissionDotPreferences
import com.ekoehler.expressivecutout.data.PhoneTilePreferences
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import com.ekoehler.expressivecutout.data.TimerTilePreferences
import com.ekoehler.expressivecutout.data.TimerTileSettings
import com.ekoehler.expressivecutout.service.CutoutNotificationListenerService
import com.ekoehler.expressivecutout.system.PermissionUsageMonitor
import com.ekoehler.expressivecutout.ui.theme.ExpressiveCutoutTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.reflect.Proxy
import kotlin.math.roundToInt

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
    private val permissionDotPreferences = PermissionDotPreferences(context)
    private val density = context.resources.displayMetrics.density

    /**
     * Full display width, used by the island to size itself as a percentage of the screen. Read
     * from the *current* window metrics so it follows the device between portrait and landscape —
     * recomputed on rotation by [onOrientationChanged]. (maximumWindowMetrics would stay pinned to
     * the natural orientation, leaving the landscape pill and its touchable-region carve-out
     * mis-sized.) The px value is read live by the touchable region; the dp value is a flow so the
     * pill re-sizes on rotation without recreating the ComposeView.
     */
    private var displayWidthPx: Int = computeDisplayWidthPx()
    private val displayWidthDp = MutableStateFlow((displayWidthPx / density).toInt())

    /**
     * The orientation the live window geometry was built for, so [onOrientationChanged] only reacts
     * to an actual portrait <-> landscape flip. Also the single source of truth for the current
     * orientation, ready for orientation-specific layout settings later.
     */
    private var currentOrientation: Int = context.resources.configuration.orientation
    private val orientationState = MutableStateFlow(currentOrientation)

    /**
     * The live display rotation (Surface.ROTATION_*). Tracked separately from [currentOrientation]
     * because a 90° <-> 270° flip keeps the orientation LANDSCAPE yet moves the camera to the
     * opposite edge, so stick-to-camera must react to it too. The flow drives the pill's rotated
     * rendering so it re-orients on that flip without recreating the ComposeView.
     */
    private var currentRotation: Int = currentDisplayRotation()
    private val rotationState = MutableStateFlow(currentRotation)

    /**
     * Set while a rotation cross-fade is applying the new geometry with the island hidden, so the
     * pill snaps to its new shape/placement instead of sliding there — the movement stays invisible
     * and only the fade is seen.
     */
    private val rotationSnapState = MutableStateFlow(false)

    private val displayHeightPx: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            context.resources.displayMetrics.heightPixels
        }
    private val displayHeightDp: Int = (displayHeightPx / density).toInt()

    private val currentEvent = MutableStateFlow<IslandEvent?>(null)

    /**
     * The event parked in the satellite bubble beside the pill, or null when the island is whole.
     * Holds whatever [currentEvent] displaced, so a pinned live tile stays visible instead of
     * vanishing until [livePillToReturnTo] brings it back. Capacity is exactly one.
     */
    private val satelliteEvent = MutableStateFlow<IslandEvent?>(null)
    private var satelliteDismissJob: Job? = null

    /**
     * When the pill's / bubble's auto-dismiss is due, or null while that slot is pinned (a live
     * tile). Carried across a demotion so a notification pushed into the bubble expires on its
     * original schedule rather than getting a fresh lease.
     */
    private var currentDeadlineMs: Long? = null
    private var satelliteDeadlineMs: Long? = null

    /**
     * Set while the pill is showing an event tapped out of the bubble. The two slots are swapped
     * for the duration so the expanded cutout acts on a real [currentEvent], and swapped back when
     * it collapses — the bubble is hidden while expanded, so the user never sees them move.
     */
    private var restoreSlotsOnCollapse = false
    private val layoutState = MutableStateFlow(IslandLayout.DEFAULT)
    private val forcedExpanded = MutableStateFlow<Boolean?>(null)
    private val behaviourState = MutableStateFlow(BehaviourSettings())
    private val appearanceState = MutableStateFlow(AppearanceSettings())

    /**
     * Whether the permission dots are switched on, and which end of the pill they sit on. *What* to
     * draw comes from [PermissionUsageMonitor] instead; the enabled flag is mirrored separately so
     * the island can keep the (empty) dot row mounted and let each dot animate itself.
     */
    private val permissionDotEnabledState = MutableStateFlow(false)
    private val permissionDotPositionState = MutableStateFlow(PermissionDotPosition.RIGHT)
    private val permissionDotColorsState = MutableStateFlow(PermissionDotColors())
    private val permissionDotVerticalState = MutableStateFlow(false)
    private var customIcons: Map<SystemEventType, IconSource> = emptyMap()
    private var eventEnabled: Map<SystemEventType, Boolean> = emptyMap()
    private var eventDurations: Map<SystemEventType, Int> = emptyMap()
    private var eventAnimatedIcons: Map<SystemEventType, Boolean> = emptyMap()
    private var eventAnimatedIconLoops: Map<SystemEventType, Boolean> = emptyMap()
    private var eventColors: Map<SystemEventType, CutoutColor> = emptyMap()
    /**
     * The system event currently on the pill, so its auto-dismiss uses that event's own duration
     * override (null while a notification or live tile is showing → the global normal duration).
     */
    private var currentSystemEventType: SystemEventType? = null

    /**
     * The notification key the pill last mirrored, so the listener can be told the moment that pill
     * goes away and a notification it was holding back is due in the panel. See
     * [observeMirroredKey].
     */
    private var mirroredKey: String? = null
    private var eventDynamicColor: Boolean = false
    private var eventDynamicColorRole: DynamicRole = DynamicRole.PRIMARY
    private var eventDynamicColorOpacity: Float = 1f
    private var tileEnabled: Map<DynamicTile, Boolean> = emptyMap()
    /** Packages the user muted on the Apps screen: nothing they post reaches the cutout. */
    private var disabledApps: Set<String> = emptySet()
    /** Packages allowed on the cutout but never allowed to expand it on their own. */
    private var normalOnlyApps: Set<String> = emptySet()
    private var musicSettings: MusicTileSettings = MusicTileSettings()
    private var phoneSettings: PhoneTileSettings = PhoneTileSettings()
    private var timerSettings: TimerTileSettings = TimerTileSettings()
    private var assistantSettings: AssistantTileSettings = AssistantTileSettings()
    private var previewPinned = false
    private var previewExpanded = false
    private var expanded = false
    /**
     * True while a media session is actively playing; keeps the music cutout pinned up (no
     * auto-dismiss) for as long as playback lasts.
     */
    private var musicPlaying = false
    /**
     * The last resolved music event, so the pill can return after a notification/system event that
     * briefly took over the cutout while playback carried on.
     */
    private var lastMusicEvent: IslandEvent? = null
    /**
     * The package of the app currently in the foreground, from the accessibility service. Drives
     * the "Visible in player app" option: when off, the music cutout hides while this matches the
     * player.
     */
    private var foregroundPackage: String? = null
    /**
     * True while the music cutout is being held hidden because the playing app is in the foreground
     * and "Visible in player app" is off, so it can be brought back when the user leaves that app.
     */
    private var playerAppHidden = false
    /**
     * True while the phone cutout is being held hidden because the phone app is full screen in the
     * foreground, so it can be brought back when the user leaves the phone app.
     */
    private var phoneAppHidden = false
    /**
     * True while a phone call is present; keeps the call cutout pinned up (no auto-dismiss) for the
     * whole call, and — like [lastMusicEvent] — lets the pill return after an interruption.
     */
    private var callActive = false
    private var lastCallEvent: IslandEvent? = null
    /**
     * True while a countdown timer is running; keeps the timer cutout pinned up (no auto-dismiss)
     * for the whole countdown, and — like [lastCallEvent] — lets the pill return after an
     * interruption.
     */
    private var timerActive = false
    private var lastTimerEvent: IslandEvent? = null
    private var assistantActive = false
    private var lastAssistantEvent: IslandEvent? = null
    // True while the device is locked (screen off or keyguard active); keeps the locked cutout pinned
    // up (no auto-dismiss) until the screen is unlocked.
    private var isDeviceLocked = false
    private var lastLockEvent: IslandEvent? = null

    /** A neutral sample shown while the settings screen pins the island open. */
    private val previewEvent by lazy {
        IslandEvent(
            id = -1L,
            icon = IslandIcon.Vector(Icons.Rounded.Tune),
            label = context.getString(R.string.preview_label),
            detail = context.getString(R.string.preview_detail),
            accent = Color(0xFF60A5FA),
            appColor = Color(0xFF60A5FA),
        )
    }

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var dismissJob: Job? = null
    private var windowResizeJob: Job? = null
    private val collapseTrigger = MutableStateFlow(0L)

    /**
     * The installed OnComputeInternalInsetsListener (a reflection Proxy), kept so it can be
     * removed.
     */
    private var insetsListener: Any? = null

    /**
     * True while the window has been torn down because "hide on lockscreen" or "hide in landscape"
     * is active. Guards signal handling and drives whether the window currently exists.
     */
    private var overlayHidden = false
    private var savedEventBeforeHide: IslandEvent? = null

    /**
     * Re-evaluate lock visibility whenever the screen or lock state changes. All are protected
     * system broadcasts.
     */
    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = applyLockVisibility()
    }

    /**
     * Brings the island up: the fake lifecycle, the overlay window, the lock receiver, and one
     * collector per preference group. Mirrored by [stop].
     */
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
        observePermissionDotSettings()
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
        observeMirroredKey()
    }

    /**
     * Takes the island down and undoes everything [start] set up, releasing anything the rest of
     * the system would otherwise keep holding on the island's behalf.
     */
    fun stop() {
        satelliteDismissJob?.cancel()
        // The island is going away with a pill still up, so nothing is left to mirror the
        // notification it was standing in for — hand it back to the panel before the collector that
        // would normally notice dies with the scope.
        mirroredKey?.let { CutoutNotificationListenerService.release(it) }
        dismissJob?.cancel()
        windowResizeJob?.cancel()
        runCatching { context.unregisterReceiver(lockReceiver) }
        removeOverlay()
        lifecycleOwner.onDestroy()
        scope.cancel()
    }

    /**
     * Watches for the screen locking and unlocking, which the "hide on lockscreen" setting acts on.
     */
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
        val isKeyguardLocked = keyguardManager?.isKeyguardLocked == true
        val isDeviceLockedActual = keyguardManager?.isDeviceLocked == true
        isDeviceLocked = isDeviceLockedActual
        val shouldHideLock = behaviourState.value.hideOnLockscreen && isKeyguardLocked
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

            !shouldHide && !overlayHidden && isDeviceLockedActual && currentEvent.value == null -> {
                showLockedEvent()
            }

            !shouldHide && !overlayHidden && !isDeviceLockedActual && currentEvent.value?.id == lastLockEvent?.id && lastLockEvent != null -> {
                lastLockEvent = null
                IslandEventBus.emit(CutoutSignal.System(SystemEventType.DEVICE_UNLOCKED))
            }
        }
    }

    private fun showLockedEvent() {
        if (!behaviourState.value.cutoutEnabled || eventEnabled[SystemEventType.DEVICE_LOCKED] == false) return
        val lockedSignal = CutoutSignal.System(SystemEventType.DEVICE_LOCKED)
        val resolved = resolver.resolve(
            lockedSignal,
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
        ).copy(initiallyExpanded = false, normalOnly = false)
        isDeviceLocked = true
        lastLockEvent = resolved
        currentEvent.value = resolved
        dismissJob?.cancel()
        syncWindowSize()
    }

    /**
     * Puts back whatever should be on screen after the island was hidden, in priority order: a
     * pinned preview, then a call, then music, then a timer, then whatever pill was interrupted.
     * Only the last of those is given a fresh dismiss timer, since the live tiles stay until their
     * state ends.
     */
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
            isDeviceLocked && lastLockEvent != null -> {
                dismissJob?.cancel()
                expanded = false
                currentEvent.value = lastLockEvent
            }
            isDeviceLocked -> {
                showLockedEvent()
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
        val rotation = currentDisplayRotation()
        if (orientation == currentOrientation && rotation == currentRotation) return
        // The split is portrait-only, so a bubble must not survive the rotation: landscape gives it no
        // reserved width and no touchable region, which would leave it drawn but dead.
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) clearSatellite()
        val view = composeView
        // With nothing on screen there is nothing to cross-fade — apply the new geometry at once.
        if (view == null || overlayHidden) {
            applyRotation(orientation, rotation)
            return
        }
        // Cross-fade across the rotation: fade the island out at its old placement, snap to the new
        // geometry while it is invisible (so it never slides from the top band to the side edge),
        // then fade it back in.
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .setDuration(ROTATION_FADE_MS)
            .withEndAction {
                rotationSnapState.value = true
                applyRotation(orientation, rotation)
                val revealed = composeView
                if (revealed == null || overlayHidden) {
                    rotationSnapState.value = false
                    return@withEndAction
                }
                revealed.animate()
                    .alpha(1f)
                    .setDuration(ROTATION_FADE_MS)
                    .withEndAction { rotationSnapState.value = false }
                    .start()
            }
            .start()
    }

    /** Adopt a new orientation/rotation: resize to the live width and re-place the window. */
    private fun applyRotation(orientation: Int, rotation: Int) {
        currentOrientation = orientation
        currentRotation = rotation
        orientationState.value = orientation
        rotationState.value = rotation
        displayWidthPx = computeDisplayWidthPx()
        displayWidthDp.value = (displayWidthPx / density).toInt()
        applyLockVisibility()
        if (overlayHidden) return
        // Resize straight to the final geometry in one step (not the usual grow-then-shrink), while the
        // island is still invisible mid cross-fade. The delayed shrink would otherwise fire after the
        // fade-in and visibly slide the pill back to its place.
        windowResizeJob?.cancel()
        resizeWindow(
            windowWidthPx(layoutState.value),
            windowHeightPx(layoutState.value, expanded),
        )
    }

    /**
     * Creates the Compose-hosting overlay window and attaches it to the window manager. This is the
     * one place the island touches the framework's view system.
     */
    private fun addOverlay() {
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE || event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    onOutsideTouch()
                }
                false
            }
            setContent {
                val collapse by collapseTrigger.collectAsStateWithLifecycle()
                val event by currentEvent.collectAsStateWithLifecycle()
                val satellite by satelliteEvent.collectAsStateWithLifecycle()
                val layout by layoutState.collectAsStateWithLifecycle()
                val forced by forcedExpanded.collectAsStateWithLifecycle()
                val behaviour by behaviourState.collectAsStateWithLifecycle()
                val appearance by appearanceState.collectAsStateWithLifecycle()
                val widthDp by displayWidthDp.collectAsStateWithLifecycle()
                val orientation by orientationState.collectAsStateWithLifecycle()
                val rotation by rotationState.collectAsStateWithLifecycle()
                val snapGeometry by rotationSnapState.collectAsStateWithLifecycle()
                val permissionUsage by PermissionUsageMonitor.usage.collectAsStateWithLifecycle()
                val permissionDotsEnabled by permissionDotEnabledState.collectAsStateWithLifecycle()
                val permissionDotPosition by permissionDotPositionState.collectAsStateWithLifecycle()
                val permissionDotColors by permissionDotColorsState.collectAsStateWithLifecycle()
                val permissionDotVertical by permissionDotVerticalState.collectAsStateWithLifecycle()
                // The dot settings screen shows every enabled dot here, switch on or not.
                val permissionDotPreview by PermissionDotPreviewBus.active.collectAsStateWithLifecycle()
                val isNoExpandLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    (behaviour.horizontalCutoutMode == HorizontalCutoutMode.NORMAL_ONLY ||
                     behaviour.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA)
                val effectiveForced = if (isNoExpandLandscape) false else forced
                val isStickToCamera = orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    behaviour.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
                val rot270 = rotation == Surface.ROTATION_270

                ExpressiveCutoutTheme {
                    DynamicIsland(
                        event = event,
                        collapsed = layout.collapsed,
                        expanded = layout.expanded,
                        displayWidthDp = widthDp,
                        forcedExpanded = effectiveForced,
                        collapseTrigger = collapse,
                        isStickToCamera = isStickToCamera,
                        isRotation270 = rot270,
                        snapGeometry = snapGeometry,
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
                        vibrateOnTap = behaviour.vibrateOnTap,
                        hapticsOnPop = behaviour.hapticsOnPop,
                        permissionDotsEnabled = permissionDotsEnabled || permissionDotPreview,
                        permissionUsage = permissionUsage,
                        permissionDotPosition = permissionDotPosition,
                        permissionDotColors = permissionDotColors,
                        permissionDotsVertical = permissionDotVertical,
                        satellite = satellite,
                        satellitePosition = behaviour.satellitePosition,
                        onSatelliteClick = ::onSatellitePromote,
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

    /**
     * Detaches the overlay window immediately, rather than posting the removal, so a teardown can't
     * leave a view behind on a service that is already going away.
     */
    private fun removeOverlay() {
        composeView?.let { windowManager.removeViewImmediate(it) }
        composeView = null
        insetsListener = null
    }

    /** Mirrors the user's per-event icon overrides into [customIcons]. */
    private fun observeIconPreferences() = scope.launch {
        iconPreferences.customIcons.collect { customIcons = it }
    }

    /**
     * Mirrors the behaviour settings, applying lock visibility as they arrive so a toggle takes
     * effect at once rather than at the next lock.
     */
    private fun observeBehaviour() = scope.launch {
        behaviourPreferences.settings.collect {
            behaviourState.value = it
            // Toggling "hide on lockscreen" (or first load while already locked) must take effect now.
            applyLockVisibility()
        }
    }

    /**
     * Mirrors the appearance settings, re-syncing the window because the action-button height feeds
     * the expanded window's size.
     */
    private fun observeAppearance() = scope.launch {
        appearancePreferences.settings.collect {
            appearanceState.value = it
            // The action-button height feeds the expanded window's extra room; keep them in step.
            syncWindowSize()
        }
    }

    /** Mirrors the per-event enabled switches into [eventEnabled]. */
    private fun observeEventPreferences() = scope.launch {
        eventPreferences.enabled.collect { eventEnabled = it }
    }

    /** Mirrors the per-event duration overrides into [eventDurations]. */
    private fun observeEventDurations() = scope.launch {
        eventPreferences.durations.collect { eventDurations = it }
    }

    /**
     * Mirrors the animated-icon switches and their loop flags, which are stored apart but always
     * read together.
     */
    private fun observeEventAnimatedIcons() {
        scope.launch { eventPreferences.animatedIcons.collect { eventAnimatedIcons = it } }
        scope.launch { eventPreferences.animatedIconLoops.collect { eventAnimatedIconLoops = it } }
    }

    /** Mirrors the per-event colour overrides into [eventColors]. */
    private fun observeEventColors() = scope.launch {
        eventPreferences.colors.collect { eventColors = it }
    }

    /** Mirrors whether events take their colour from Material You into [eventDynamicColor]. */
    private fun observeEventDynamicColor() = scope.launch {
        eventPreferences.dynamicColor.collect { eventDynamicColor = it }
    }

    /** Mirrors which Material You role events use into [eventDynamicColorRole]. */
    private fun observeEventDynamicColorRole() = scope.launch {
        eventPreferences.dynamicColorRole.collect { eventDynamicColorRole = it }
    }

    /** Mirrors the Material You colour opacity into [eventDynamicColorOpacity]. */
    private fun observeEventDynamicColorOpacity() = scope.launch {
        eventPreferences.dynamicColorOpacity.collect { eventDynamicColorOpacity = it }
    }

    /** Mirrors which dynamic tiles are enabled into [tileEnabled]. */
    private fun observeTilePreferences() = scope.launch {
        dynamicTilePreferences.enabled.collect { tileEnabled = it }
    }

    /**
     * Mirrors both per-app sets: the apps the island is off for, and the ones limited to the
     * collapsed cutout.
     */
    private fun observeAppPreferences() {
        scope.launch { appPreferences.disabledPackages.collect { disabledApps = it } }
        scope.launch { appPreferences.normalOnlyPackages.collect { normalOnlyApps = it } }
    }

    /** Mirrors the permission-dot switch, its placement and each dot's colour into their states. */
    private fun observePermissionDotSettings() {
        scope.launch { permissionDotPreferences.enabled.collect { permissionDotEnabledState.value = it } }
        scope.launch { permissionDotPreferences.position.collect { permissionDotPositionState.value = it } }
        scope.launch { permissionDotPreferences.colors.collect { permissionDotColorsState.value = it } }
        scope.launch { permissionDotPreferences.vertical.collect { permissionDotVerticalState.value = it } }
        // A dot lighting up or going out changes how wide the pill is drawn, so the window has to
        // follow — the geometry flows above do the same by way of their own observers.
        scope.launch {
            PermissionUsageMonitor.usage
                .map { it.count }
                .distinctUntilChanged()
                .collect { syncWindowSize() }
        }
    }

    /**
     * Mirrors the music tile settings, re-applying player-app visibility so that toggle takes
     * effect mid-playback.
     */
    private fun observeMusicSettings() = scope.launch {
        musicTilePreferences.settings.collect {
            musicSettings = it
            // Toggling "Visible in player app" should take effect immediately, even mid-playback.
            applyPlayerAppVisibility()
        }
    }

    /** Mirrors the phone tile settings into [phoneSettings]. */
    private fun observePhoneSettings() = scope.launch {
        phoneTilePreferences.settings.collect { phoneSettings = it }
    }

    /** Mirrors the timer tile settings into [timerSettings]. */
    private fun observeTimerSettings() = scope.launch {
        timerTilePreferences.settings.collect { timerSettings = it }
    }

    /** Mirrors the assistant tile settings into [assistantSettings]. */
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
            pruneSatellite()
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
            pruneSatellite()
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
            pruneSatellite()
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

    /** The lock cutout should stay pinned up (no auto-dismiss) while the device is locked. */
    private fun isPinnedLock(): Boolean = isDeviceLocked && currentEvent.value?.id == lastLockEvent?.id

    /** Any live tile (music, a call, a running timer, assistant, or lock status) is currently pinned up. */
    private fun isPinnedLiveTile(): Boolean = isPinnedMusic() || isPinnedCall() || isPinnedTimer() || isPinnedAssistant() || isPinnedLock()

    /**
     * Mirrors the island geometry, re-sizing the window as it changes so a slider drag in settings
     * is visible live.
     */
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
            setTouchable(
                event != null || satelliteEvent.value != null ||
                    (behaviour.showsWhenEmpty && behaviour.cutoutEnabled),
            )
        }
    }

    /**
     * Hand a mirrored notification back to the panel as soon as its pill leaves the island, however
     * it leaves: the auto-dismiss timer ran out, an expanded pill shrank away, another event took the
     * island over, the overlay was torn down for the lockscreen. Watching the shown event rather than
     * hooking each of those paths is what keeps the two in step — nothing can make the pill vanish
     * without passing through here.
     *
     * The user-driven endings (a swipe, a tap, an action) reach the listener first and by their own
     * route, having already told it to throw the notification away instead; this then finds nothing
     * left to release.
     */
    private fun observeMirroredKey() = scope.launch {
        currentEvent.collect { event ->
            val key = event?.notificationKey
            val previous = mirroredKey
            if (previous != null && previous != key) CutoutNotificationListenerService.release(previous)
            mirroredKey = key
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

    /**
     * Resizes the window to fit, growing at once but shrinking only after [WINDOW_SHRINK_DELAY_MS].
     * The window has to be big enough before the island animates into it, so the grow leads and the
     * shrink waits for the animation to finish.
     */
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

    /**
     * Applies a window size, gravity and offset in one pass, skipping the call entirely when
     * nothing changed so an unchanged layout costs no framework work.
     */
    private fun resizeWindow(targetWidthPx: Int, targetHeightPx: Int) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val targetGravity = computeWindowGravity()
        val targetY = computeWindowOffsetY()
        if (params.width != targetWidthPx || params.height != targetHeightPx ||
            params.gravity != targetGravity || params.y != targetY
        ) {
            params.width = targetWidthPx
            params.height = targetHeightPx
            params.gravity = targetGravity
            params.y = targetY
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    /**
     * Lets touches through or catches them, so the island only steals input while there is
     * something on screen to press.
     */
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
                            val region = touchableRegionField.get(info) as Region
                            region.setEmpty()
                            for (rect in touchRects(view.width, view.height)) {
                                region.op(rect, Region.Op.UNION)
                            }
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
     * Every rectangle that should accept touches: the pill, plus the satellite bubble when one is up.
     * A union rather than one bounding box, so the gap between them keeps falling through to the app
     * underneath instead of swallowing a shade pull.
     */
    private fun touchRects(viewWidth: Int, viewHeight: Int): List<Rect> {
        val pill = pillTouchRect(viewWidth, viewHeight)
        val satellite = satelliteTouchRect(viewWidth, viewHeight)
        return if (satellite == null) listOf(pill) else listOf(pill, satellite)
    }

    /**
     * The satellite bubble's rectangle in the window's own coordinates, or null when no bubble is up.
     * Mirrors the offsets [DynamicIsland] places it at - the pill's centre, out past half the pill's
     * width plus the gap - so what is tappable is exactly what is drawn.
     */
    private fun satelliteTouchRect(viewWidth: Int, viewHeight: Int): Rect? {
        if (satelliteEvent.value == null) return null
        if (expanded) return null
        val isStickToCamera = orientationState.value == Configuration.ORIENTATION_LANDSCAPE &&
            behaviourState.value.horizontalCutoutMode == HorizontalCutoutMode.STICK_TO_CAMERA
        if (isStickToCamera) return null
        val collapsed = layoutState.value.collapsed
        val dims = effectiveDims(layoutState.value, expanded = false)
        val diameterPx = (collapsed.heightDp * density).toInt()
        val gapPx = (SATELLITE_GAP_DP * density).toInt()
        val dotBonusPx = (permissionDotWidthBonusDp(false) * density).toInt()
        val splitPx = (satelliteSplitDp() * density).toInt()
        val pillWidthPx = displayWidthPx * dims.widthPercent / 100 + dotBonusPx - splitPx
        val margin = (TOUCH_MARGIN_DP * density).toInt()
        val centerX = viewWidth / 2 + (dims.offsetXDp * density).toInt() + dotBonusPx / 2 +
            satelliteShiftPx(splitPx)
        val step = pillWidthPx / 2 + gapPx + diameterPx / 2
        val satelliteCenterX = if (behaviourState.value.satellitePosition == SatellitePosition.LEFT) {
            centerX - step
        } else {
            centerX + step
        }
        val topPx = (collapsed.offsetYDp * density).toInt()
        val bottomPx = topPx + diameterPx
        return Rect(
            (satelliteCenterX - diameterPx / 2 - margin).coerceAtLeast(0),
            (topPx - margin).coerceAtLeast(0),
            (satelliteCenterX + diameterPx / 2 + margin).coerceAtMost(viewWidth),
            (bottomPx + margin).coerceAtMost(if (viewHeight > 0) viewHeight else bottomPx + margin),
        )
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
        val dotBonusPx = (permissionDotWidthBonusDp(expanded) * density).toInt()
        val splitPx = (satelliteSplitDp() * density).toInt()
        val pillWidthPx = displayWidthPx * dims.widthPercent / 100 + dotBonusPx - splitPx
        val margin = (TOUCH_MARGIN_DP * density).toInt()
        // The pill grows to the right for the dots, so its centre moves with it; a bubble beside it
        // pushes the centre the other way by half of the width it gave up.
        val centerX = viewWidth / 2 + (dims.offsetXDp * density).toInt() + dotBonusPx / 2 +
            satelliteShiftPx(splitPx)
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
        val islandWidthDp = displayWidthDp.value * (dims.widthPercent / 100f) +
            permissionDotWidthBonusDp(expanded)
        return ((islandWidthDp + WINDOW_MARGIN_DP * 2) * density).toInt()
    }

    /**
     * How much wider the collapsed pill is drawn to fit the permission dots on its trailing edge, so
     * the window and the touchable region cover the grown pill rather than clipping it. Mirrors the
     * conditions [DynamicIsland] draws the dots under.
     */
    private fun permissionDotWidthBonusDp(expanded: Boolean): Int {
        if (expanded) return 0
        if (!permissionDotEnabledState.value && !PermissionDotPreviewBus.active.value) return 0
        if (permissionDotPositionState.value != PermissionDotPosition.RIGHT) return 0
        val event = currentEvent.value
        if (event?.call != null) return 0
        // Only grown for a tile that writes on the trailing edge; elsewhere the dots fit as they are.
        if (event == null || (event.timer == null && event.progressData == null)) return 0
        return permissionDotTrailingInsetDp(
            PermissionUsageMonitor.usage.value,
            layoutState.value.collapsed.heightDp,
            permissionDotVerticalState.value,
        )
    }

    /**
     * How much width the collapsed pill gives up to the bubble beside it — the bubble's diameter (the
     * pill's own height, so it reads as a circle of the same scale) plus the gap. The pair keeps the
     * normal cutout's total width rather than growing past it, so the overlay window never has to
     * change size for a split: nothing to re-centre, and the pill can't slide off the camera.
     *
     * Mirrors what [DynamicIsland] draws, so the touchable region tracks the shrunken pill.
     */
    private fun satelliteSplitDp(): Int {
        if (satelliteEvent.value == null) return 0
        if (expanded) return 0
        if (currentEvent.value?.call != null) return 0
        if (isLandscapeSplitSuppressed()) return 0
        return layoutState.value.collapsed.heightDp + SATELLITE_GAP_DP
    }

    /** Which way the pill's centre steps to make room: away from the side the bubble takes. */
    private fun satelliteShiftPx(splitPx: Int): Int =
        if (behaviourState.value.satellitePosition == SatellitePosition.LEFT) splitPx / 2 else -splitPx / 2

    /** The split is portrait-only for now: landscape has its own camera-anchored geometry. */
    private fun isLandscapeSplitSuppressed(): Boolean =
        orientationState.value == Configuration.ORIENTATION_LANDSCAPE

    /**
     * Whether the pill is wide enough to give a bubble's worth of width away and still be a pill.
     * Derived from the real geometry rather than a hard-coded width percentage, so a user on a narrow
     * cutout loses the bubble exactly when the split would start to look degenerate. Two floors: what
     * it gives up (or the remainder stops reaching across the camera hole) and twice its own height
     * (or the "pill" collapses towards a circle of its own).
     */
    private fun satelliteFitsWidth(): Boolean {
        val collapsed = layoutState.value.collapsed
        val splitDp = collapsed.heightDp + SATELLITE_GAP_DP
        val pillDp = displayWidthDp.value * (collapsed.widthPercent / 100f) - splitDp
        return pillDp >= splitDp && pillDp >= collapsed.heightDp * 2
    }

    /**
     * Whether [displaced] may be parked in the bubble as [incoming] takes the pill. Every
     * suppression lives here so the rule set is in one place: a call owns the whole cutout and must
     * never be demoted, the assistant tile's height is a fraction of the screen, and the same
     * notification must never occupy both slots.
     */
    private fun satelliteAllowed(displaced: IslandEvent, incoming: IslandEvent): Boolean {
        if (!behaviourState.value.splitIslandEnabled) return false
        if (isLandscapeSplitSuppressed()) return false
        if (displaced.call != null || incoming.call != null) return false
        if (displaced.assistant != null || incoming.assistant != null) return false
        if (isTwoRowCall()) return false
        val key = displaced.notificationKey
        if (key != null && key == incoming.notificationKey) return false
        if (displaced.id == incoming.id) return false
        if (!satelliteFitsWidth()) return false
        return true
    }

    /**
     * Parks [displaced] in the bubble as [incoming] takes the pill, or clears the bubble when the
     * hand-off isn't allowed. A pinned live tile arrives with a null [deadlineMs] and so persists for
     * as long as its bus says it is live; a transient keeps the deadline it already had.
     */
    private fun demoteToSatellite(displaced: IslandEvent?, incoming: IslandEvent, deadlineMs: Long?) {
        if (displaced == null || !satelliteAllowed(displaced, incoming)) {
            clearSatellite()
            return
        }
        parkInSatellite(displaced, deadlineMs)
        syncWindowSize()
    }

    /**
     * Puts [event] in the bubble with [deadlineMs] as its own expiry, arming the job that empties the
     * bubble when it comes due. A null deadline is a pinned live tile: no timer, it leaves when its
     * bus says so.
     */
    private fun parkInSatellite(event: IslandEvent, deadlineMs: Long?) {
        satelliteDismissJob?.cancel()
        satelliteEvent.value = event
        satelliteDeadlineMs = deadlineMs
        if (deadlineMs != null) {
            satelliteDismissJob = scope.launch {
                val remaining = deadlineMs - System.currentTimeMillis()
                if (remaining > 0) delay(remaining)
                if (satelliteEvent.value?.id == event.id) clearSatellite()
            }
        }
    }

    /** Arms the pill's auto-dismiss for an already-known [deadlineMs], or pins it when that is null. */
    private fun armPillDismiss(deadlineMs: Long?) {
        dismissJob?.cancel()
        currentDeadlineMs = deadlineMs
        if (deadlineMs == null) return
        dismissJob = scope.launch {
            val remaining = deadlineMs - System.currentTimeMillis()
            if (remaining > 0) delay(remaining)
            dismissIsland()
        }
    }

    /** Empties the bubble and stops whatever was going to empty it. */
    private fun clearSatellite() {
        satelliteDismissJob?.cancel()
        satelliteDismissJob = null
        satelliteDeadlineMs = null
        if (satelliteEvent.value != null) {
            satelliteEvent.value = null
            syncWindowSize()
        }
    }

    /**
     * Drops a parked live tile from the bubble once its own bus stops reporting it live - the bubble
     * has no timer of its own in that case, so without this the tile would sit there for good.
     */
    private fun pruneSatellite() {
        val bubble = satelliteEvent.value ?: return
        val stale = when {
            bubble.media != null -> !musicPlaying
            bubble.call != null -> !callActive
            bubble.timer != null -> !timerActive
            bubble.assistant != null -> !assistantActive
            else -> false
        }
        if (stale) clearSatellite()
    }

    /**
     * Moves whatever is in the bubble into the pill, always collapsed, and reports whether it did.
     *
     * The pill being replaced may have been expanded — the user swiping an open notification away is
     * the common case — and the promoted event must not inherit that. It is a normal cutout: a
     * notification or a dynamic tile, either way collapsed. Left to the new event's id alone the
     * composable can carry the open state over, so the collapse is asked for explicitly.
     *
     * A transient carries its own remaining time across rather than getting a fresh lease, and one
     * whose time already ran out while it sat in the bubble is dropped instead of being promoted for
     * a frame. A live tile has no deadline and stays pinned.
     */
    private fun promoteSatelliteCollapsed(): Boolean {
        restoreSlotsOnCollapse = false
        val bubble = satelliteEvent.value ?: return false
        val deadline = satelliteDeadlineMs
        val expired = deadline != null && deadline <= System.currentTimeMillis()
        clearSatellite()
        if (expired) return false
        forcedExpanded.value = null
        expanded = false
        currentSystemEventType = null
        currentEvent.value = bubble.copy(initiallyExpanded = false)
        currentDeadlineMs = deadline
        collapseTrigger.value = System.currentTimeMillis()
        if (deadline != null) {
            dismissJob = scope.launch {
                val remaining = deadline - System.currentTimeMillis()
                if (remaining > 0) delay(remaining)
                dismissIsland()
            }
        }
        syncWindowSize()
        return true
    }

    /**
     * Opens the bubble's event on tap, since the tap is a request to read the thing rather than to
     * reorder the island.
     *
     * The two slots are swapped for the duration rather than the tapped event merely being drawn
     * expanded, so every action, reply and dismiss inside the expanded cutout acts on a real
     * [currentEvent] and there is no second notion of "which event is open". The bubble is hidden
     * while expanded, so the swap is never seen, and [restoreSlots] puts both back on collapse —
     * from the user's side neither one ever moved, and neither is lost.
     */
    private fun onSatellitePromote() {
        val bubble = satelliteEvent.value ?: return
        // An app the user set to "Normal only" has no expanded state to open, so a tap opens the app
        // instead, exactly as it would on the pill, and the island is left alone.
        if (bubble.normalOnly) {
            bubble.contentIntent?.let { sendPendingIntent(it) }
            return
        }
        val bubbleDeadline = satelliteDeadlineMs
        val pill = currentEvent.value
        val pillDeadline = currentDeadlineMs
        satelliteDismissJob?.cancel()
        satelliteEvent.value = null
        satelliteDeadlineMs = null
        forcedExpanded.value = null
        currentSystemEventType = null
        expanded = true
        currentEvent.value = bubble.copy(initiallyExpanded = true)
        armPillDismiss(bubbleDeadline)
        if (pill != null && satelliteAllowed(pill, bubble)) {
            parkInSatellite(pill.copy(initiallyExpanded = false), pillDeadline)
            restoreSlotsOnCollapse = true
        }
        syncWindowSize()
    }

    /**
     * Undoes the swap [onSatellitePromote] made: the event that was expanded goes back to the bubble,
     * the one it displaced back to the pill, each keeping the deadline it still had.
     */
    private fun restoreSlots() {
        val opened = currentEvent.value
        val parked = satelliteEvent.value
        if (opened == null || parked == null) return
        val openedDeadline = currentDeadlineMs
        val parkedDeadline = satelliteDeadlineMs
        satelliteDismissJob?.cancel()
        satelliteEvent.value = null
        satelliteDeadlineMs = null
        forcedExpanded.value = null
        expanded = false
        currentSystemEventType = null
        currentEvent.value = parked.copy(initiallyExpanded = false)
        armPillDismiss(parkedDeadline)
        parkInSatellite(opened.copy(initiallyExpanded = false), openedDeadline)
        collapseTrigger.value = System.currentTimeMillis()
        syncWindowSize()
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

    /**
     * The extra height the expanded island claims for its bottom rows, mirroring the composable: the
     * control row, plus the music progress bar when that's shown as a third row of its own.
     */
    private fun expandedActionsBonusDp(): Int {
        val event = currentEvent.value
        val hasActions = behaviourState.value.showActionButtons && event?.actions?.isNotEmpty() == true
        val hasMediaControls = event?.media?.showControls == true
        val hasCallActions = event?.call?.showActions == true && event.actions.isNotEmpty()
        val hasTimerActions = event?.timer?.showActions == true && event.actions.isNotEmpty()
        val hasAssistantActions = behaviourState.value.showActionButtons && event?.assistant != null
        val controlsExtra =
            if (hasActions || hasMediaControls || hasCallActions || hasTimerActions || hasAssistantActions) {
                expandedActionsExtraDp(appearanceState.value.actionButtonHeightDp)
            } else {
                0
            }
        val progressExtra = if (event?.media?.showProgress == true) expandedMediaProgressExtraDp() else 0
        return controlsExtra + progressExtra
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

    /**
     * The single consumer of [IslandEventBus]: turns each signal into a pill or a live tile,
     * honouring the per-event and per-tile switches the user set.
     */
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
            // Skip silent notifications when configured to ignore them.
            if (signal is CutoutSignal.Notification && behaviourState.value.ignoreSilentNotifications && signal.isSilent) {
                return@collect
            }

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
                        is CutoutSignal.System -> {
                            if (signal.type == SystemEventType.DEVICE_LOCKED) {
                                isDeviceLocked = true
                                lastLockEvent = resolvedEvent
                            } else if (signal.type == SystemEventType.DEVICE_UNLOCKED) {
                                isDeviceLocked = false
                                lastLockEvent = null
                            }
                        }
                        else -> {}
                    }
                }
                return@collect
            }

            if (!behaviourState.value.cutoutEnabled) return@collect

            val existing = currentEvent.value
            if (signal is CutoutSignal.Notification && signal.key != null &&
                existing != null && existing.notificationKey == signal.key
            ) {
                currentEvent.value = resolvedEvent.copy(id = existing.id)
                syncWindowSize()
                scheduleDismiss()
                return@collect
            }

            // Park whatever the pill is losing in the bubble, carrying its own deadline over, so a
            // pinned live tile stays visible rather than disappearing until livePillToReturnTo
            // brings it back. Cleared straight after: scheduleDismiss re-arms it below for a
            // transient, and the pinned branches leave it null.
            restoreSlotsOnCollapse = false
            demoteToSatellite(existing, resolvedEvent, currentDeadlineMs)
            currentDeadlineMs = null
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

                is CutoutSignal.System -> {
                    if (signal.type == SystemEventType.DEVICE_LOCKED) {
                        isDeviceLocked = true
                        lastLockEvent = resolvedEvent
                        dismissJob?.cancel()
                    } else if (signal.type == SystemEventType.DEVICE_UNLOCKED) {
                        isDeviceLocked = false
                        lastLockEvent = null
                        scheduleDismiss()
                    } else {
                        scheduleDismiss()
                    }
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
        // An event tapped out of the bubble borrowed the pill for as long as it was open; give it back.
        if (!targetExpanded && restoreSlotsOnCollapse) {
            restoreSlotsOnCollapse = false
            if (behaviourState.value.expandedDisappearOnShrink) {
                // The user asked for an expanded cutout to vanish on shrink rather than settle back,
                // so hand the island to what it displaced instead of returning it to the bubble.
                dismissJob?.cancel()
                promoteSatelliteCollapsed()
            } else {
                restoreSlots()
            }
            return
        }
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

    /**
     * Tap on the pill: open what it points at. Reading the event before [dismissIsland] clears it,
     * since settling the notification needs its key.
     */
    private fun onActivate() {
        val event = currentEvent.value
        val intent = event?.contentIntent
        if (isPinnedLiveTile()) {
            dismissJob?.cancel()
            intent?.let(::sendPendingIntent)
            return
        }
        event?.notificationKey?.let { CutoutNotificationListenerService.settle(it) }
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
        currentEvent.value?.notificationKey?.let { CutoutNotificationListenerService.settle(it) }
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
        currentEvent.value?.notificationKey?.let { CutoutNotificationListenerService.settle(it) }
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

    /**
     * When the user clicks or taps outside of the expanded island, minimize it in a shrink animation.
     */
    private fun onOutsideTouch() {
        if (shouldCollapseOnOutsideTouch(expanded, previewPinned)) {
            collapseTrigger.value = System.currentTimeMillis()
        }
    }

    /**
     * Takes or releases window focus. Only held while a reply field is open, since a focusable
     * overlay would otherwise swallow the keyboard from the app underneath.
     */
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
        restoreSlotsOnCollapse = false
        forcedExpanded.value = null
        expanded = false
        // Whatever is parked in the bubble is already visible, so slide it into the pill rather than
        // clearing the island and waiting out the usual return delay, which would read as a stutter.
        if (promoteSatelliteCollapsed()) return
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
     * clear the island. A live call takes precedence over music, which takes precedence over lock state.
     * Each returns its pill only when the hidden event wasn't a live pill itself, that tile is still live,
     * and the tile is enabled.
     */
    private fun livePillToReturnTo(): IslandEvent? =
        callPillToReturnTo() ?: musicPillToReturnTo() ?: timerPillToReturnTo() ?: lockPillToReturnTo()

    /** True while a live pill occupies either slot (so we never "return" on top of one). */
    private fun showingLiveTile(): Boolean =
        isLiveTileEvent(currentEvent.value) || isLiveTileEvent(satelliteEvent.value)

    /** Whether [event] is one of the live tiles, in whichever slot it happens to sit. */
    private fun isLiveTileEvent(event: IslandEvent?): Boolean = event?.let {
        it.media != null || it.call != null || it.timer != null || (isDeviceLocked && it.id == lastLockEvent?.id)
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

    /**
     * The music pill to fall back to once a transient pill is done, or null when music shouldn't be
     * showing.
     */
    private fun musicPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!musicPlaying) return null
        if (tileEnabled[DynamicTile.MUSIC] == false) return null
        // Stay hidden while the playing app is in the foreground and "Visible in player app" is off.
        if (shouldHideForPlayerApp()) return null
        return lastMusicEvent?.copy(initiallyExpanded = false)
    }

    /**
     * The call pill to fall back to once a transient pill is done, or null when no call should be
     * showing.
     */
    private fun callPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!callActive) return null
        if (tileEnabled[DynamicTile.PHONE] == false) return null
        // The dialer stays on the call bus even when muted, so don't bring its pill back.
        if (OnCallBus.state.value?.packageName in disabledApps) return null
        if (shouldHideForPhoneApp()) return null
        return lastCallEvent?.copy(initiallyExpanded = false)
    }

    /**
     * The timer pill to fall back to once a transient pill is done, or null when no timer should be
     * showing.
     */
    private fun timerPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!timerActive) return null
        if (tileEnabled[DynamicTile.TIMER] == false) return null
        return lastTimerEvent?.copy(initiallyExpanded = false)
    }

    private fun lockPillToReturnTo(): IslandEvent? {
        if (showingLiveTile()) return null
        if (!isDeviceLocked) return null
        if (eventEnabled[SystemEventType.DEVICE_LOCKED] == false) return null
        if (!behaviourState.value.cutoutEnabled) return null
        return lastLockEvent?.copy(initiallyExpanded = false)
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

    /**
     * Arms the timer that dismisses the current pill, preferring a per-event duration override over
     * the global one. A live tile is never given a timer: it stays until its state ends.
     */
    private fun scheduleDismiss() {
        dismissJob?.cancel()
        currentDeadlineMs = null
        // Never time out a live cutout while it's active — it stays until playback / the call stops.
        if (isPinnedLiveTile()) return
        // A system event with its own duration override wins; everything else uses the global normal.
        val seconds = currentSystemEventType?.let { eventDurations[it] }
            ?: behaviourState.value.normalDurationSeconds
        currentDeadlineMs = System.currentTimeMillis() + seconds * 1_000L
        dismissJob = scope.launch {
            delay(seconds * 1_000L)
            // Return to the pinned preview if settings is still open.
            if (previewPinned) {
                expanded = previewExpanded
                forcedExpanded.value = previewExpanded
                currentEvent.value = previewEvent
                syncWindowSize()
                return@launch
            }
            // Whatever is parked in the bubble is already on screen, so promote that copy rather
            // than letting livePillToReturnTo resolve a second one.
            if (promoteSatelliteCollapsed()) return@launch
            val livePill = livePillToReturnTo()
            when {
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

    /**
     * Where the island sits in its window. Centred at the top everywhere except landscape with
     * stick-to-camera on, where it follows the physical camera instead.
     */
    private fun computeWindowGravity(): Int {
        if (currentOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            return Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        return when (behaviourState.value.horizontalCutoutMode) {
            HorizontalCutoutMode.STICK_TO_CAMERA -> getLandscapeCameraGravity()
            else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    /** The display's current rotation, read through the window manager rather than the context. */
    private fun currentDisplayRotation(): Int {
        // Read from the WindowManager's display, not context.display: the accessibility service's
        // context is not a visual/display context, so context.display throws on API R+.
        @Suppress("DEPRECATION")
        return windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
    }

    /** The current display size in px in the live orientation (width, height). */
    private fun currentScreenSizePx(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val metrics = context.resources.displayMetrics
            metrics.widthPixels to metrics.heightPixels
        }

    /**
     * Which edge the (rotated) landscape pill hugs. Derived from the camera cutout's real position
     * (left/right half of the landscape screen) so it tracks the actual hole, falling back to the
     * display rotation when the cutout can't be measured.
     */
    private fun getLandscapeCameraGravity(): Int {
        val center = composeView?.let { CutoutMetrics.cutoutCenterPx(it) }
        if (center != null) {
            val (widthPx, _) = currentScreenSizePx()
            return if (center.x < widthPx / 2f) {
                Gravity.LEFT or Gravity.CENTER_VERTICAL
            } else {
                Gravity.RIGHT or Gravity.CENTER_VERTICAL
            }
        }
        return when (currentRotation) {
            Surface.ROTATION_270 -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
            else -> Gravity.LEFT or Gravity.CENTER_VERTICAL
        }
    }

    /**
     * The window's vertical offset from its gravity anchor. For landscape stick-to-camera the window
     * hugs a side edge and is centred vertically, so shift it by the cutout's distance from the
     * screen's vertical centre — that plants the pill's midline directly over the physical camera.
     * Zero in every other case (portrait, or non-stick modes, position via gravity alone).
     */
    private fun computeWindowOffsetY(): Int {
        if (currentOrientation != Configuration.ORIENTATION_LANDSCAPE) return 0
        if (behaviourState.value.horizontalCutoutMode != HorizontalCutoutMode.STICK_TO_CAMERA) return 0
        val center = composeView?.let { CutoutMetrics.cutoutCenterPx(it) } ?: return 0
        val (_, heightPx) = currentScreenSizePx()
        return (center.y - heightPx / 2f).roundToInt()
    }

    /** How far the island has to be rotated to sit level with the physical camera in landscape. */
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

    /**
     * Builds the overlay window's parameters. Uses `TYPE_ACCESSIBILITY_OVERLAY` rather than
     * `SYSTEM_ALERT_WINDOW`, which is what lets the island draw with no draw-over-apps permission
     * at all.
     */
    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        // A fixed band centred at the top, sized to hug the pill (see syncWindowSize) rather than span the
        // whole screen — so the areas either side stay free for the notification-shade pull, in landscape
        // too. Starts non-touchable (nothing showing) and becomes touchable only while the island is
        // visible (so tap-to-expand works). WATCH_OUTSIDE_TOUCH delivers ACTION_OUTSIDE when the user taps
        // outside the expanded island to trigger the shrink animation.
        return WindowManager.LayoutParams(
            windowWidthPx(IslandLayout.DEFAULT),
            windowHeightPx(IslandLayout.DEFAULT),
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = computeWindowGravity()
            y = computeWindowOffsetY()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    internal companion object {
        fun shouldCollapseOnOutsideTouch(isExpanded: Boolean, previewPinned: Boolean): Boolean =
            isExpanded && !previewPinned
        const val TAG = "IslandOverlay"
        const val WINDOW_MARGIN_DP = 24

        /** Half-length of the rotation cross-fade: island fades out, snaps, then fades back in. */
        const val ROTATION_FADE_MS = 150L

        /**
         * Slack around the pill's touchable rectangle so its rounded edges, shadow and tap "boop"
         * scale stay tappable — kept small so the shade-pull area beside the pill stays free.
         */
        const val TOUCH_MARGIN_DP = 12


        /**
         * Hold the (larger) expanded window size until the pill has finished its ~220ms collapse
         * animation, then shrink — so the collapse never clips and the freed area becomes tappable.
         */
        const val WINDOW_SHRINK_DELAY_MS = 300L

        /**
         * Beat between a dismissed interruption fading out and the music pill easing back in, so
         * the hand-off doesn't feel like an instant, janky swap.
         */
        const val MUSIC_RETURN_DELAY_MS = 350L

        /**
         * Let the center's collapse begin before a screen-capturing / screen-covering shortcut
         * fires, so the overlay isn't caught in the screenshot or left behind the power dialog.
         */
        const val CENTER_ACTION_SETTLE_MS = 260L
    }
}
