package com.ekoehler.expressivecutout.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.ekoehler.expressivecutout.events.MediaPlaybackMonitor
import com.ekoehler.expressivecutout.events.SystemEventMonitor
import com.ekoehler.expressivecutout.overlay.IslandOverlayController

/**
 * The always-on host of the island. Its sole purpose is to provide a context that can add
 * a TYPE_ACCESSIBILITY_OVERLAY window (no SYSTEM_ALERT_WINDOW required) and to keep the
 * overlay controller and system-event monitor alive for the lifetime of the binding. It
 * deliberately ignores accessibility events and never inspects window content.
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: we host an overlay, we do not react to accessibility events.
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
        mediaPlayback?.stop()
        mediaPlayback = null
        systemEvents?.stop()
        systemEvents = null
        overlay?.stop()
        overlay = null
    }
}
