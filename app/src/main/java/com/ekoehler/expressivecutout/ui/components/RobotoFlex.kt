@file:OptIn(ExperimentalTextApi::class)

package com.ekoehler.expressivecutout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ekoehler.expressivecutout.R

/** The `wdth` axis range Roboto Flex exposes; 100 is the normal, undistorted width. */
const val ROBOTO_FLEX_MIN_WIDTH = 25f
const val ROBOTO_FLEX_MAX_WIDTH = 151f
const val ROBOTO_FLEX_DEFAULT_WIDTH = 100f

/** Roboto Flex's `XTRA` (counter width) axis, ridden alongside `wdth` — see [xtraFor]. */
private const val XTRA_MIN = 323f
private const val XTRA_DEFAULT = 468f
private const val XTRA_MAX = 603f

/**
 * The `XTRA` value that goes with a given `wdth`. On its own the `wdth` axis only spans about ±11%
 * of a word's width, which barely reads at label sizes; pulling the counters in (or opening them
 * out) by the same fraction of their own range takes that to roughly ±30% without touching the
 * apparent weight. The two halves are mapped separately so [ROBOTO_FLEX_DEFAULT_WIDTH] still lands
 * on the font's own [XTRA_DEFAULT].
 */
private fun xtraFor(width: Float): Float = if (width <= ROBOTO_FLEX_DEFAULT_WIDTH) {
    val t = (width - ROBOTO_FLEX_MIN_WIDTH) / (ROBOTO_FLEX_DEFAULT_WIDTH - ROBOTO_FLEX_MIN_WIDTH)
    XTRA_MIN + t * (XTRA_DEFAULT - XTRA_MIN)
} else {
    val t = (width - ROBOTO_FLEX_DEFAULT_WIDTH) / (ROBOTO_FLEX_MAX_WIDTH - ROBOTO_FLEX_DEFAULT_WIDTH)
    XTRA_DEFAULT + t * (XTRA_MAX - XTRA_DEFAULT)
}

/**
 * Roboto Flex condensed or expanded to [width] (clamped to the `wdth` axis range) at the given
 * `wght` [weight], for labels whose width the user can dial in.
 */
@Composable
fun rememberRobotoFlexFamily(
    width: Float,
    weight: Int = 700,
): FontFamily = remember(width, weight) {
    val clamped = width.coerceIn(ROBOTO_FLEX_MIN_WIDTH, ROBOTO_FLEX_MAX_WIDTH)
    FontFamily(
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(clamped),
                FontVariation.Setting("XTRA", xtraFor(clamped)),
            ),
        )
    )
}
