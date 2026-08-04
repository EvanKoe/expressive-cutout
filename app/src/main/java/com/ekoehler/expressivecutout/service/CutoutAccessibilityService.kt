package com.ekoehler.expressivecutout.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.ForegroundAppBus
import com.ekoehler.expressivecutout.core.IslandEventBus
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
 * It tracks which app is in the foreground, and inspects assistant windows for live response text.
 */
class CutoutAccessibilityService : AccessibilityService() {

    private var overlay: IslandOverlayController? = null
    private var systemEvents: SystemEventMonitor? = null
    private var mediaPlayback: MediaPlaybackMonitor? = null
    private var lastAssistantKey: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = IslandOverlayController(this).also { it.start() }
        systemEvents = SystemEventMonitor(this).also { it.start() }
        mediaPlayback = MediaPlaybackMonitor(this).also { it.start() }
        instance = this
        _bound.value = true
    }

    private fun isAssistantPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg == "com.google.android.googlequicksearchbox" ||
            pkg == "com.google.android.apps.googleassistant" ||
            pkg == "com.google.android.apps.bard" ||
            pkg == "com.google.android.apps.gemini" ||
            pkg == "com.samsung.android.bixby.agent" ||
            pkg == "com.samsung.android.bixby.service" ||
            pkg == "com.amazon.dee.app" ||
            pkg == "com.openai.chatgpt" ||
            pkg == "com.microsoft.copilot" ||
            pkg.contains("assistant") ||
            pkg.contains("bixby") ||
            pkg.contains("gemini")
    }

    private val DISCLAIMER_PATTERNS = listOf(
        "can make mistakes",
        "gemini is ai",
        "gemini is an ai",
        "display inaccurate info",
        "check responses",
        "type, talk, or share",
        "ask gemini",
        "gemini advanced",
        "share screen with live"
    )

    private fun isDisclaimer(text: String): Boolean {
        val lower = text.lowercase()
        return DISCLAIMER_PATTERNS.any { lower.contains(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkg = ev.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return

        if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ForegroundAppBus.update(pkg)
        }

        if (isAssistantPackage(pkg)) {
            inspectAssistantWindow(pkg, ev)
        } else if (lastAssistantKey != null && ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // User navigated away from assistant app/overlay — dismiss assistant cutout
            lastAssistantKey = null
            IslandEventBus.emit(CutoutSignal.Assistant(packageName = pkg, active = false))
        }
    }

    private fun inspectAssistantWindow(pkg: String, event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: event.source
        if (rootNode == null) {
            if (lastAssistantKey != null) {
                lastAssistantKey = null
                IslandEventBus.emit(CutoutSignal.Assistant(packageName = pkg, active = false))
            }
            return
        }

        val textList = mutableListOf<String>()
        collectTextNodes(rootNode, textList)

        if (textList.isEmpty()) {
            if (lastAssistantKey != null) {
                lastAssistantKey = null
                IslandEventBus.emit(CutoutSignal.Assistant(packageName = pkg, active = false))
            }
            return
        }

        val title = textList.firstOrNull { it.isNotBlank() }
        val responseText = textList.filter { it.isNotBlank() && it != title }.joinToString("\n").ifBlank { title }

        val lastKey = "$pkg|$title|$responseText"
        if (lastKey != lastAssistantKey) {
            lastAssistantKey = lastKey
            IslandEventBus.emit(
                CutoutSignal.Assistant(
                    packageName = pkg,
                    title = title,
                    text = responseText,
                    contentIntent = null,
                    active = true,
                ),
            )
        }
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() && text.length > 1 && !isDisclaimer(text)) {
            list.add(text)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextNodes(child, list)
        }
    }

    /**
     * Forward device rotations to the overlay so it can rebuild its top-of-screen window for the new
     * geometry — otherwise the touchable-region carve-out that lets the notification shade through
     * beside the pill goes stale in landscape and the band swallows the shade pull.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onOrientationChanged(newConfig.orientation)
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
        instance = null
        mediaPlayback?.stop()
        mediaPlayback = null
        systemEvents?.stop()
        systemEvents = null
        overlay?.stop()
        overlay = null
    }

    companion object {
        /**
         * The live service instance while bound, used by [performGlobal] to fire system-wide actions
         * for the expanded "center" shortcuts. Held statically (the service has no android:process, so
         * it's this same process) and cleared in [teardown] so it never outlives the binding.
         */
        private var instance: CutoutAccessibilityService? = null

        /**
         * Perform a system-wide [AccessibilityService] global action (e.g. lock screen, screenshot,
         * quick settings) if the service is bound. Best-effort: returns false when nothing is bound
         * or the action is rejected, so callers can fall back or ignore it.
         */
        fun performGlobal(action: Int): Boolean =
            runCatching { instance?.performGlobalAction(action) }.getOrNull() ?: false

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
