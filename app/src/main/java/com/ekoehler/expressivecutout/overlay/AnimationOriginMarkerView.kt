package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.View

/**
 * Draws a target at the island window's actual screen-relative animation origin.
 */
class AnimationOriginMarkerView(context: Context) : View(context) {

    /** The target position in screen pixels, matching the island window's coordinate system. */
    var targetScreenPosition: Point? = null
        set(value) {
            field = value
            invalidate()
        }

    /** Converts the saved dp offsets and marker dimensions to display pixels. */
    private val density = resources.displayMetrics.density

    /** Soft outer highlight that keeps the target visible against varied wallpaper and app colors. */
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HALO_COLOR
        style = Paint.Style.FILL
    }

    /** Filled centre of the highlighted target. */
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MARKER_COLOR
        style = Paint.Style.FILL
    }

    /** Bright outline around the target centre. */
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OUTLINE_COLOR
        style = Paint.Style.STROKE
        strokeWidth = OUTLINE_WIDTH_DP * density
    }

    /**
     * Draws a bright, outlined circle at the configured display point so it remains visible over
     * both light and dark app content.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val target = targetScreenPosition ?: return
        val markerWindowPosition = IntArray(2)
        getLocationOnScreen(markerWindowPosition)
        val centerX = (target.x - markerWindowPosition[0]).toFloat()
        val centerY = (target.y - markerWindowPosition[1]).toFloat()
        canvas.drawCircle(centerX, centerY, HALO_RADIUS_DP * density, haloPaint)
        canvas.drawCircle(centerX, centerY, MARKER_RADIUS_DP * density, markerPaint)
        canvas.drawCircle(centerX, centerY, MARKER_RADIUS_DP * density, outlinePaint)
    }

    private companion object {
        /** Marker palette and dimensions, grouped for the view's drawing style. */
        const val HALO_COLOR = 0x8064D8F0.toInt()
        const val MARKER_COLOR = 0xFF35C9E8.toInt()
        const val OUTLINE_COLOR = 0xFFFFFFFF.toInt()
        const val HALO_RADIUS_DP = 18f
        const val MARKER_RADIUS_DP = 8f
        const val OUTLINE_WIDTH_DP = 2f
    }
}
