package com.ekoehler.expressivecutout.data

/**
 * User-tunable geometry of the island. [widthDp]/[heightDp] set the pill's size directly
 * and independently; [offsetXDp]/[offsetYDp] shift it from its top-centre anchor (positive
 * X moves right, positive Y moves down). Every value is clamped to the ranges below so the
 * pill can never collapse or be pushed fully off-screen.
 */
data class IslandLayout(
    val widthDp: Int = DEFAULT_WIDTH_DP,
    val heightDp: Int = DEFAULT_HEIGHT_DP,
    val offsetXDp: Int = DEFAULT_OFFSET_X_DP,
    val offsetYDp: Int = DEFAULT_OFFSET_Y_DP,
) {
    companion object {
        const val DEFAULT_WIDTH_DP = 132
        const val DEFAULT_HEIGHT_DP = 34
        const val DEFAULT_OFFSET_X_DP = 0
        const val DEFAULT_OFFSET_Y_DP = 6

        const val MIN_WIDTH_DP = 60
        const val MAX_WIDTH_DP = 340
        const val MIN_HEIGHT_DP = 22
        const val MAX_HEIGHT_DP = 96
        const val MIN_OFFSET_X_DP = -180
        const val MAX_OFFSET_X_DP = 180
        const val MIN_OFFSET_Y_DP = 0
        const val MAX_OFFSET_Y_DP = 160

        val DEFAULT = IslandLayout()

        fun of(widthDp: Int, heightDp: Int, offsetXDp: Int, offsetYDp: Int) = IslandLayout(
            widthDp = widthDp.coerceIn(MIN_WIDTH_DP, MAX_WIDTH_DP),
            heightDp = heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
            offsetXDp = offsetXDp.coerceIn(MIN_OFFSET_X_DP, MAX_OFFSET_X_DP),
            offsetYDp = offsetYDp.coerceIn(MIN_OFFSET_Y_DP, MAX_OFFSET_Y_DP),
        )
    }
}
