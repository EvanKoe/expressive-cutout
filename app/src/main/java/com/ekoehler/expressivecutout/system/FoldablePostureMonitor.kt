package com.ekoehler.expressivecutout.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.data.LayoutPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps the closed-device profile synchronized with the hinge when window layout updates are
 * unavailable, including while the island's accessibility service runs without the app activity.
 */
class FoldablePostureMonitor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService<SensorManager>()
    private val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    private val layoutPreferences = LayoutPreferences(context)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var currentPosture: Boolean? = null
    private var registered = false

    /**
     * Starts observing the device hinge when its angle sensor is available.
     */
    fun start() {
        val manager = sensorManager ?: return
        val sensor = hingeSensor ?: return
        scope.launch {
            currentPosture = layoutPreferences.deviceClosed.first()
            registered = manager.registerListener(this@FoldablePostureMonitor, sensor, SensorManager.SENSOR_DELAY_UI)
            if (!registered) Log.w(TAG, "Failed to register the hinge-angle sensor listener")
        }
    }

    /**
     * Stops sensor updates and releases the monitor's coroutine scope.
     */
    fun stop() {
        if (registered) sensorManager?.unregisterListener(this)
        registered = false
        scope.cancel()
    }

    /**
     * Updates posture only near a fully closed or open hinge position, keeping flex and tent
     * positions on their current profile while the active-screen layout reports a more precise state.
     */
    override fun onSensorChanged(event: SensorEvent) {
        val angleDegrees = event.values.firstOrNull() ?: return
        val closed = when {
            angleDegrees <= CLOSED_ANGLE_DEGREES -> true
            angleDegrees >= OPEN_ANGLE_DEGREES -> false
            else -> return
        }
        if (closed == currentPosture) return
        currentPosture = closed
        scope.launch { layoutPreferences.setDeviceClosed(closed) }
    }

    /**
     * Hinge sensor accuracy does not affect the posture threshold.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TAG = "FoldablePostureMonitor"
        const val CLOSED_ANGLE_DEGREES = 10f
        const val OPEN_ANGLE_DEGREES = 20f
    }
}
