package com.ekoehler.expressivecutout.data

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

        fun of(
            widthPercent: Int,
            heightDp: Int,
            offsetXDp: Int,
            offsetYDp: Int,
            cornerTopLeftDp: Int,
            cornerTopRightDp: Int,
            cornerBottomLeftDp: Int,
            cornerBottomRightDp: Int,
        ) = IslandDimensions(
            widthPercent = widthPercent.coerceIn(MIN_WIDTH_PERCENT, MAX_WIDTH_PERCENT),
            heightDp = heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
            offsetXDp = offsetXDp.coerceIn(MIN_OFFSET_X_DP, MAX_OFFSET_X_DP),
            offsetYDp = offsetYDp.coerceIn(MIN_OFFSET_Y_DP, MAX_OFFSET_Y_DP),
            cornerTopLeftDp = cornerTopLeftDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerTopRightDp = cornerTopRightDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerBottomLeftDp = cornerBottomLeftDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerBottomRightDp = cornerBottomRightDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
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
fun IslandDimensions.asCallCutout(
    widthPercent: Int = CALL_MIN_WIDTH_PERCENT,
    incoming: Boolean = false,
): IslandDimensions =
    IslandDimensions.of(
        widthPercent = widthPercent,
        // An incoming call uses a taller, two-row cutout: caller (below the camera) over a row of
        // Take / Hang up buttons. A connected call keeps the shorter single-row shape.
        heightDp = if (incoming) CALL_INCOMING_HEIGHT_DP else CALL_HEIGHT_DP,
        offsetXDp = offsetXDp,
        offsetYDp = offsetYDp,
        cornerTopLeftDp = CALL_CORNER_DP,
        cornerTopRightDp = CALL_CORNER_DP,
        cornerBottomLeftDp = CALL_CORNER_DP,
        cornerBottomRightDp = CALL_CORNER_DP,
    )

/** The call cutout's default width, and the most it will grow to when a caller name is long. */
const val CALL_MIN_WIDTH_PERCENT = 60
const val CALL_MAX_WIDTH_PERCENT = 80

private const val CALL_HEIGHT_DP = 60
/** Taller shape for the incoming-call tile so its caller row and Take / Hang up button row both fit. */
private const val CALL_INCOMING_HEIGHT_DP = 138
private const val CALL_CORNER_DP = 30

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
        )

        val DEFAULT = IslandLayout()
    }
}
