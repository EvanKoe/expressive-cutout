package com.ekoehler.expressivecutout.core

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.data.CenterShortcut
import com.ekoehler.expressivecutout.service.CutoutAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Performs a [CenterShortcut]. Global actions go through the live accessibility service
 * ([CutoutAccessibilityService.performGlobal]); torch, settings panels and app launches use plain
 * platform APIs. Every call is best-effort and swallows failures, so a dead service, a device without
 * a flash, or a missing app never crashes the overlay — it just returns false.
 */
object CenterShortcutExecutor {

    // Hosts the brief torch-state read only; no work — and no camera listener — lives here at rest.
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // How long to wait for the camera service to report the current torch state before giving up
    // (e.g. the flash is busy in another app). Kept tight — the state callback is near-immediate.
    private const val TORCH_STATE_TIMEOUT_MS = 500L

    /** Runs the shortcut. Returns true if it could be dispatched; false means it isn't available here. */
    fun execute(shortcut: CenterShortcut, context: Context): Boolean = when (shortcut) {
        is CenterShortcut.Global -> performGlobal(shortcut)
        CenterShortcut.Torch -> toggleTorch(context)
        is CenterShortcut.Panel -> startIntent(Intent(shortcut.panel.action), context)
        is CenterShortcut.LaunchApp -> launchApp(shortcut.packageName, context)
    }

    private fun performGlobal(shortcut: CenterShortcut.Global): Boolean {
        if (Build.VERSION.SDK_INT < shortcut.action.minSdk) return false
        return CutoutAccessibilityService.performGlobal(shortcut.action.action)
    }

    private fun launchApp(packageName: String, context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return startIntent(launch, context)
    }

    private fun startIntent(intent: Intent, context: Context): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    /**
     * Publish the current torch state into [TorchStateBus] without changing it — called when the
     * center opens so the torch shortcut renders lit/unlit correctly from the first frame.
     */
    fun syncTorchState(context: Context) {
        val cameraManager = context.getSystemService<CameraManager>() ?: return
        scope.launch {
            val (_, isOn) = readTorchState(context, cameraManager) ?: return@launch
            TorchStateBus.update(isOn)
        }
    }

    /**
     * Toggle the camera flash. Rather than keep a resident [CameraManager.TorchCallback] alive
     * between taps, we register one only long enough to read the authoritative current state — the
     * camera service reports it near-immediately on registration — unregister it, then flip. A
     * toggle is therefore always correct even if another app changed the torch, with no listeners at
     * rest. Returns true optimistically (the flip runs asynchronously once the state is read).
     */
    private fun toggleTorch(context: Context): Boolean {
        val cameraManager = context.getSystemService<CameraManager>() ?: return false
        scope.launch {
            val (cameraId, isOn) = readTorchState(context, cameraManager) ?: return@launch
            val next = !isOn
            if (runCatching { cameraManager.setTorchMode(cameraId, next) }.isSuccess) {
                TorchStateBus.update(next)
            }
        }
        return true
    }

    /**
     * The current (cameraId, enabled) of the first flash-capable camera, read via a one-shot
     * [CameraManager.TorchCallback] that is unregistered the instant it reports — and on timeout or
     * cancellation via [kotlinx.coroutines.CancellableContinuation.invokeOnCancellation], so it never
     * outlives the read. Null when no flash reports a state in time (e.g. none present, or in use).
     */
    private suspend fun readTorchState(
        context: Context,
        cameraManager: CameraManager,
    ): Pair<String, Boolean>? = withTimeoutOrNull(TORCH_STATE_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    runCatching { cameraManager.unregisterTorchCallback(this) }
                    if (continuation.isActive) continuation.resume(cameraId to enabled)
                }
            }
            cameraManager.registerTorchCallback(context.mainExecutor, callback)
            continuation.invokeOnCancellation {
                runCatching { cameraManager.unregisterTorchCallback(callback) }
            }
        }
    }
}
