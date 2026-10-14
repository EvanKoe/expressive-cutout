package com.ekoehler.expressivecutout.data

import kotlin.math.roundToInt

/**
 * Geometry for one island state. [widthPercent] is the width as a percentage of the screen
 * width (so it scales to any device and can span the whole screen); [heightDp] is the height;
 * [offsetXDp]/[offsetYDp] shift it from its top-centre anchor (positive X right, positive Y
 * down); and the four corner radii round each corner independently. All values are clamped to
 * the ranges below.
 *
 * [isNotch] turns the state into a notch instead of a floating pill: it hangs from the very top of
 * the screen and its top corners flare *outwards* into the screen edge by [notchFlareDp], the
 * inverted (concave) counterpart of a corner radius. A notch is flush by definition, so [of] forces
 * [offsetYDp] to zero whenever it is set — the fillets would curve into empty space otherwise, and
 * the overlay window and its touchable region both size themselves off that offset. The two top
 * radii are kept as the user left them but go unused, since a flare replaces the corner it is on.
 * The ears sit *outside* [widthPercent], so widening the flare never eats into the body — see
 * [notchOverhangDp], which every width measurement adds back.
 *
 * [bottomEars] is the notch-like expanded design: the bottom corners become concave ears hanging
 * *below* the body, sized by [cornerBottomLeftDp] / [cornerBottomRightDp]. It is only ever set by
 * [asNotchLikeExpanded], never stored; see [bottomEarDp] for the height the ears add.
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
    val isNotch: Boolean = false,
    val notchFlareDp: Int = DEFAULT_NOTCH_FLARE_DP,
    val bottomEars: Boolean = false,
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
        const val MIN_NOTCH_FLARE_DP = 0
        const val MAX_NOTCH_FLARE_DP = 48
        const val DEFAULT_NOTCH_FLARE_DP = 20

        /**
         * The only way geometry is built from untrusted numbers — sliders, an imported settings
         * file, the derived call and tiny cutouts — so it is also where a notch's "flush with the
         * top of the screen" invariant is enforced, once, for every caller.
         */
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
            isNotch: Boolean = false,
            notchFlareDp: Int = DEFAULT_NOTCH_FLARE_DP,
            bottomEars: Boolean = false,
        ) = IslandDimensions(
            widthPercent = widthPercent.coerceIn(MIN_WIDTH_PERCENT, MAX_WIDTH_PERCENT),
            heightDp = heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
            offsetXDp = offsetXDp.coerceIn(MIN_OFFSET_X_DP, MAX_OFFSET_X_DP),
            offsetYDp = if (isNotch) 0 else offsetYDp.coerceIn(MIN_OFFSET_Y_DP, MAX_OFFSET_Y_DP),
            cornerTopLeftDp = cornerTopLeftDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerTopRightDp = cornerTopRightDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerBottomLeftDp = cornerBottomLeftDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            cornerBottomRightDp = cornerBottomRightDp.coerceIn(MIN_CORNER_DP, MAX_CORNER_DP),
            topMarginDp = topMarginDp.coerceIn(MIN_TOP_MARGIN_DP, MAX_TOP_MARGIN_DP),
            isNotch = isNotch,
            notchFlareDp = notchFlareDp.coerceIn(MIN_NOTCH_FLARE_DP, MAX_NOTCH_FLARE_DP),
            bottomEars = bottomEars,
        )
    }
}

/**
 * How far a notch's ears reach past each side of the configured width, so the body always measures
 * exactly what the width slider says. Zero for every other state, which lets each width measurement
 * add it unconditionally.
 */
val IslandDimensions.notchOverhangDp: Int get() = if (isNotch) notchFlareDp else 0

/**
 * How far the bottom ears of the notch-like design hang below the body — the larger of the two lower
 * corner radii. Zero for every other state, so each height measurement can add it unconditionally.
 */
val IslandDimensions.bottomEarDp: Int
    get() = if (bottomEars) maxOf(cornerBottomLeftDp, cornerBottomRightDp) else 0

/**
 * The expanded state as the notch-like design draws it: the full screen width, flush with the top
 * edge and centred. The two lower corner radii become the ears' radii and the top-edge notch is off,
 * since this design replaces it.
 */
fun IslandDimensions.asNotchLikeExpanded(): IslandDimensions = IslandDimensions.of(
    widthPercent = IslandDimensions.MAX_WIDTH_PERCENT,
    heightDp = heightDp,
    offsetXDp = 0,
    offsetYDp = 0,
    cornerTopLeftDp = cornerTopLeftDp,
    cornerTopRightDp = cornerTopRightDp,
    cornerBottomLeftDp = cornerBottomLeftDp,
    cornerBottomRightDp = cornerBottomRightDp,
    topMarginDp = topMarginDp,
    bottomEars = true,
)

/**
 * The phone tile is shown as a single, bigger "normal" cutout (it has no expanded state): wider than
 * the usual collapsed pill and a bit taller, so the caller's photo and name sit on the left and the
 * hang-up button on the right. Derived from the user's [collapsed] state so its vertical offset
 * carries over; the height and (stadium) corners are fixed to this fuller shape. [widthPercent] is
 * the screen-width fraction to span — [CALL_MIN_WIDTH_PERCENT] by default, widened to fit a long
 * caller name (see the overlay's width measurement). Used by both the overlay's rendering and its
 * touch sizing so they always agree on the call cutout's geometry. A notched normal cutout stays a
 * notch here, so the shape doesn't change character when a call comes in.
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
        isNotch = isNotch,
        notchFlareDp = notchFlareDp,
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
        isNotch = isNotch,
        notchFlareDp = notchFlareDp,
    )
}

/**
 * The split connected call's pill: the user's own collapsed geometry — height, corners, vertical
 * offset — but sized and placed around the physical camera like [asTinyCutout], since its content
 * has to stay readable. [contentWidthDp] of badge and clock sits on the leading side, ending where
 * the camera hole begins, and the pill runs on past the hole, clearing it by [TINY_CAMERA_GAP_DP],
 * with nothing drawn over it.
 *
 * @param displayWidthDp the screen width, since [IslandDimensions] stores width as a percentage of it.
 * @param cameraRightEdgeDp the camera cutout's right edge measured from the screen's horizontal
 *   centre, or null when the device won't report one — then [DEFAULT_CAMERA_RADIUS_DP] stands in.
 */
fun IslandDimensions.asSplitCallCutout(
    displayWidthDp: Int,
    contentWidthDp: Float,
    cameraRightEdgeDp: Float? = null,
): IslandDimensions {
    val cameraRight = cameraRightEdgeDp ?: DEFAULT_CAMERA_RADIUS_DP
    val widthDp = contentWidthDp + cameraRight * 2f + TINY_CAMERA_GAP_DP
    val percent = if (displayWidthDp > 0) (widthDp * 100f / displayWidthDp).roundToInt() else widthPercent
    return IslandDimensions.of(
        widthPercent = percent,
        heightDp = heightDp,
        // Centre of the span from the content's leading edge (one camera radius left of the hole,
        // less the content) to the trailing edge that clears the hole by a gap.
        offsetXDp = ((TINY_CAMERA_GAP_DP - contentWidthDp) / 2f).roundToInt(),
        offsetYDp = offsetYDp,
        cornerTopLeftDp = cornerTopLeftDp,
        cornerTopRightDp = cornerTopRightDp,
        cornerBottomLeftDp = cornerBottomLeftDp,
        cornerBottomRightDp = cornerBottomRightDp,
        topMarginDp = topMarginDp,
        isNotch = isNotch,
        notchFlareDp = notchFlareDp,
    )
}

/** How much wider than it is tall the tiny cutout is — just enough for its glyph. */
private const val TINY_WIDTH_RATIO = 1.8f

/** How far the tiny cutout's trailing edge clears the camera hole's own edge. */
private const val TINY_CAMERA_GAP_DP = 4f

/** Half a typical punch-hole, standing in when the device won't report its cutout. */
internal const val DEFAULT_CAMERA_RADIUS_DP = 16f

/**
 * The two independently configurable island states, plus the [expandedDesign] the expanded one is
 * drawn in. The stored [expanded] geometry is what the user edits; [rendered] is what is drawn.
 */
data class IslandLayout(
    val collapsed: IslandDimensions = DEFAULT_COLLAPSED,
    val expanded: IslandDimensions = DEFAULT_EXPANDED,
    val expandedDesign: ExpandedDesign = ExpandedDesign.ISLAND,
) {
    /**
     * This layout with the expanded geometry resolved for [expandedDesign], so the overlay's drawing,
     * window and touch sizing all read one consistent set of numbers.
     */
    fun rendered(): IslandLayout =
        if (expandedDesign == ExpandedDesign.NOTCH_LIKE) copy(expanded = expanded.asNotchLikeExpanded()) else this

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
