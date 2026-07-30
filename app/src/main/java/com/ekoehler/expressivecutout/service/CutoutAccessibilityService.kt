package com.ekoehler.expressivecutout.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.ekoehler.expressivecutout.core.ForegroundAppBus
import com.ekoehler.expressivecutout.events.MediaPlaybackMonitor
import com.ekoehler.expressivecutout.events.SystemEventMonitor
import com.ekoehler.expressivecutout.overlay.IslandOverlayController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The always-on host of the island. Its main purpose is to provide a context that can add
 * a TYPE_ACCESSIBILITY_OVERLAY window (no SYSTEM_ALERT_WINDOW required) and to keep the
 * overlay controller and system-event monitor alive for the lifetime of the binding.
 *
 * It also tracks which app is in the foreground — read from the package name on
 * window-state-changed events only, never from window content (canRetrieveWindowContent stays
 * false) — so the music tile can hide itself while the playing app is open.
 */
class CutoutAccessibilityService : AccessibilityService() {

    private var overlay: IslandOverlayController? = null
    private var systemEvents: SystemEventMonitor? = null
    private var mediaPlayback: MediaPlaybackMonitor? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = IslandOverlayController(this).also { it.start() }
        systemEvents = SystemEventMonitor(this).also { it.start() }
        mediaPlayback = MediaPlaybackMonitor(this).also { it.start() }
        _bound.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // The only event we react to: note which app moved to the foreground so the overlay can
        // hide the music tile while the playing app is open. Only the package name is read.
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        ForegroundAppBus.update(pkg)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        _bound.value = false
        mediaPlayback?.stop()
        mediaPlayback = null
        systemEvents?.stop()
        systemEvents = null
        overlay?.stop()
        overlay = null
    }

    companion object {
        private val _bound = MutableStateFlow(false)

        /**
         * True only while Android actually has this service bound — i.e. while the island is
         * really running. Deliberately separate from
         * [com.ekoehler.expressivecutout.permissions.Permissions.isAccessibilityGranted], which
         * reads the user's *consent* out of Settings.Secure: that stays "enabled" across a
         * reinstall or an app update while the binding is dead, so the app would otherwise report
         * itself healthy while nothing at all is listening. Lives in the companion object rather
         * than on the instance so the settings UI (same process — no android:process on the
         * service) can observe it without a binder of its own.
         */
        val bound: StateFlow<Boolean> = _bound.asStateFlow()
    }
}
