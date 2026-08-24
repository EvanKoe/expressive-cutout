package com.ekoehler.expressivecutout.core

import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.view.RoundedCorner
import android.view.View

/**
 * Reads the physical screen geometry the island cares about — the front-camera cutout and the
 * display's rounded corners — from a live [View]'s window insets. All values are in the view's
 * current-orientation pixel coordinates, so the same call works in portrait and landscape.
 *
 * Everything degrades gracefully: the precise APIs ([android.view.DisplayCutout.getCutoutPath],
 * [android.view.RoundedCorner]) only exist on API 31+, so older devices fall back to the cutout's
 * bounding rectangles or simply report null, and callers keep their own defaults.
 */
object CutoutMetrics {

    /**
     * The centre of the device's camera cutout in [view]'s pixel coordinates, or null when the
     * device has no cutout. Prefers the exact [android.view.DisplayCutout.getCutoutPath] outline
     * (API 31+) and falls back to the largest of the cutout's bounding rectangles.
     */
    fun cutoutCenterPx(view: View): PointF? {
        val cutout = view.rootWindowInsets?.displayCutout ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val path = cutout.cutoutPath
            if (path != null) {
                val bounds = RectF()
                path.computeBounds(bounds, true)
                if (!bounds.isEmpty) return PointF(bounds.centerX(), bounds.centerY())
            }
        }
        val rect = cutout.boundingRects.maxByOrNull { it.width() * it.height() } ?: return null
        return PointF(rect.exactCenterX(), rect.exactCenterY())
    }

    /**
     * The display's rounded-corner radius in pixels (the largest of the four corners), or null when
     * the device reports no rounded corners or predates the API (31+). Used as the natural default
     * for the expanded island so its corners echo the phone's own.
     */
    fun screenCornerRadiusPx(view: View): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val insets = view.rootWindowInsets ?: return null
        return listOf(
            RoundedCorner.POSITION_TOP_LEFT,
            RoundedCorner.POSITION_TOP_RIGHT,
            RoundedCorner.POSITION_BOTTOM_LEFT,
            RoundedCorner.POSITION_BOTTOM_RIGHT,
        ).mapNotNull { insets.getRoundedCorner(it)?.radius }
            .maxOrNull()
            ?.takeIf { it > 0 }
    }
}
