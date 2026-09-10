package com.ekoehler.expressivecutout.data

import kotlin.math.roundToInt

/**
 * Geometry for one island state. [widthPercent] is the width as a percentage of the screen
 * width (so it scales to any device and can span the whole screen); [heightDp] is the height;
 * [offsetXDp]/[offsetYDp] shift it from its top-centre anchor (positive X right, positive Y
 * down); and the four corner radii round each corner independently. All values are clamped to
 * the ranges below.
 */
data class IslandDimensions(
    val widthPercent: Int,
    val heightDp: Int,
    val offsetXDp: Int,
    val offsetYDp: Int,
    val cornerTopLeftDp: Int,
    val cornerTopRightDp: Int,
    val cornerBottomLeftDp: Int,
    val cornerBottomRightDp: Int,
    val topMarginDp: Int = DEFAULT_TOP_MARGIN_DP,
) {
    companion object {
        const val MIN_WIDTH_PERCENT = 10
        const val MAX_WIDTH_PERCENT = 100
        const val MIN_HEIGHT_DP = 22
        const val MAX_HEIGHT_DP = 220
        const val MIN_OFFSET_X_DP = -200
        const val MAX_OFFSET_X_DP = 200
        const val MIN_OFFSET_Y_DP = 0
        const val MAX_OFFSET_Y_DP = 400
        const val MIN_CORNER_DP = 0
        const val MAX_CORNER_DP = 110
        const val MIN_TOP_MARGIN_DP = 0
        const val MAX_TOP_MARGIN_DP = 100
        const val DEFAULT_TOP_MARGIN_DP = 48

        fun of(
            widthPercent: Int,
            heightDp: Int,
            offsetXDp: Int,
            offsetYDp: Int,
            cornerTopLeftDp: Int,
            cornerTopRightDp: Int,
            cornerBottomLeftDp: Int,
            cornerBottomRightDp: Int,
            topMarginDp: Int = DEFAULT_TOP_MARGIN_DP,
        ) = IslandDimensions(
            widthPercent = widthPercent.coerceIn(MIN_WIDTH_PERCENT, MAX_WIDTH_PERCENT),
            heightDp = heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
            offsetXDp = offsetXDp.coerceIn(MIN_OFFSET_X_DP, MAX_OFFSET_X_DP),
            offsetYDp = offsetYDp.coerceIn(MIN_OFFSET_Y_DP, MAX_OFFSET_Y_DP),
            cornerTopLeftDp = cornerTopLeftDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerTopRightDp = cornerTopRightDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerBottomLeftDp = cornerBottomLeftDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerBottomRightDp = cornerBottomRightDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            topMarginDp = topMarginDp.coerceIn(MIN_TOP_MARGIN_DP, MAX_TOP_MARGIN_DP),
        )
    }
}

/**
 * The phone tile is shown as a single, bigger "normal" cutout (it has no expanded state): wider than
 * the usual collapsed pill and a bit taller, so the caller's photo and name sit on the left and the
 * hang-up button on the right. Derived from the user's [collapsed] state so its vertical offset
 * carries over; the height and (stadium) corners are fixed to this fuller shape. [widthPercent] is
 * the screen-width fraction to span — [CALL_MIN_WIDTH_PERCENT] by default, widened to fit a long
 * caller name (see the overlay's width measurement). Used by both the overlay's rendering and its
 * touch sizing so they always agree on the call cutout's geometry.
 */
fun IslandDimensions.asCallCutout(widthPercent: Int = CALL_MIN_WIDTH_PERCENT): IslandDimensions =
    IslandDimensions.of(
        widthPercent = widthPercent,
        heightDp = CALL_HEIGHT_DP,
        offsetXDp = offsetXDp,
        offsetYDp = offsetYDp,
        cornerTopLeftDp = CALL_CORNER_DP,
        cornerTopRightDp = CALL_CORNER_DP,
        cornerBottomLeftDp = CALL_CORNER_DP,
        cornerBottomRightDp = CALL_CORNER_DP,
        topMarginDp = topMarginDp,
    )

/** The call cutout's default width, and the most it will grow to when a caller name is long. */
const val CALL_MIN_WIDTH_PERCENT = 60
const val CALL_MAX_WIDTH_PERCENT = 80

/**
 * Height of the call cutout, fixed so an in-call island is recognisable whatever the user's normal
 * geometry is.
 */
private const val CALL_HEIGHT_DP = 60
/** Corner radius of the call cutout, half of [CALL_HEIGHT_DP] so its ends are fully round. */
private const val CALL_CORNER_DP = 30

/**
 * The music tile's "Mini player": a tiny cutout holding only the note glyph, drawn in place of the
 * normal cutout while music plays. It is not centred like every other state — it swallows the
 * physical camera hole and extends past it to the left, so it reads as the camera grown a little
 * wider to make room for the glyph.
 *
 * Derived from the user's [collapsed] state so its height and vertical offset carry over; the
 * corners are made fully round, the width is fixed at [TINY_WIDTH_RATIO] times the height, and the
 * horizontal offset is computed rather than inherited.
 *
 * @param displayWidthDp the screen width, since [IslandDimensions] stores width as a percentage of it.
 * @param cameraRightEdgeDp the camera cutout's right edge measured from the screen's horizontal
 *   centre, or null on a device whose cutout can't be measured — then [DEFAULT_CAMERA_RADIUS_DP]
 *   stands in for a centred hole.
 */
fun IslandDimensions.asTinyCutout(
    displayWidthDp: Int,
    cameraRightEdgeDp: Float? = null,
): IslandDimensions {
    val widthDp = heightDp * TINY_WIDTH_RATIO
    val percent = if (displayWidthDp > 0) (widthDp * 100f / displayWidthDp).roundToInt() else heightDp
    val cameraRight = cameraRightEdgeDp ?: DEFAULT_CAMERA_RADIUS_DP
    return IslandDimensions.of(
        widthPercent = percent,
        heightDp = heightDp,
        // Its trailing edge clears the camera by one gap, so all of the width it has over the hole
        // falls on the leading side — exactly the room the glyph needs.
        offsetXDp = (cameraRight + TINY_CAMERA_GAP_DP - widthDp / 2f).roundToInt(),
        offsetYDp = offsetYDp,
        cornerTopLeftDp = heightDp / 2,
        cornerTopRightDp = heightDp / 2,
        cornerBottomLeftDp = heightDp / 2,
        cornerBottomRightDp = heightDp / 2,
        topMarginDp = topMarginDp,
    )
}

/** How much wider than it is tall the tiny cutout is — just enough for its glyph. */
private const val TINY_WIDTH_RATIO = 1.8f

/** How far the tiny cutout's trailing edge clears the camera hole's own edge. */
private const val TINY_CAMERA_GAP_DP = 4f

/** Half a typical punch-hole, standing in when the device won't report its cutout. */
private const val DEFAULT_CAMERA_RADIUS_DP = 16f

/** The two independently configurable island states. */
data class IslandLayout(
    val collapsed: IslandDimensions = DEFAULT_COLLAPSED,
    val expanded: IslandDimensions = DEFAULT_EXPANDED,
) {
    companion object {
        val DEFAULT_COLLAPSED = IslandDimensions(
            widthPercent = 38,
            heightDp = 34,
            offsetXDp = 0,
            offsetYDp = 6,
            cornerTopLeftDp = 17,
            cornerTopRightDp = 17,
            cornerBottomLeftDp = 17,
            cornerBottomRightDp = 17,
            topMarginDp = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
        )
        val DEFAULT_EXPANDED = IslandDimensions(
            widthPercent = 90,
            heightDp = 108,
            offsetXDp = 0,
            offsetYDp = 6,
            cornerTopLeftDp = 30,
            cornerTopRightDp = 30,
            cornerBottomLeftDp = 30,
            cornerBottomRightDp = 30,
            topMarginDp = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
        )

        val DEFAULT = IslandLayout()
    }
}
