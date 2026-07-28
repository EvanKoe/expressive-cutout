package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import com.ekoehler.expressivecutout.data.AnimationBounce
import com.ekoehler.expressivecutout.data.AnimationSpeed
import com.ekoehler.expressivecutout.data.AnimationStyle
import kotlin.math.roundToInt

/**
 * Builds the animation specs for the island's primary motion — the appear/disappear reveal, the
 * size / position / corner transition and the background fade — from the user's animation settings.
 * Shared by the overlay island and the Animations screen's example pills, so the example previews
 * exactly the motion the real cutout uses.
 *
 * [AnimationStyle.EXPRESSIVE] uses spatial springs based on the Material 3 expressive
 * `MotionScheme` tokens (the `MotionScheme` API itself needs material3 1.4+; see [spatialSpec] for
 * how the values here deviate). [AnimationStyle.EASE_IN_OUT] uses a standard ease-in-out tween
 * scaled by the duration slider.
 */
internal class IslandMotion(
    style: AnimationStyle,
    private val speed: AnimationSpeed,
    private val bounce: AnimationBounce,
    animationDurationMs: Int,
) {
    private val animScale = animationDurationMs / BASE_TRANSITION_MS.toFloat()
    private val expressive = style == AnimationStyle.EXPRESSIVE

    private fun scaled(baseMs: Int) = (baseMs * animScale).roundToInt()

    /** Spatial motion on a 0–1 fraction (the reveal). Springs may overshoot past 1; clamp consumers. */
    fun float(baseMs: Int = BASE_TRANSITION_MS): AnimationSpec<Float> =
        if (expressive) spatialSpec(speed, bounce, visibilityThreshold = 0.001f)
        else tween(durationMillis = scaled(baseMs), easing = EaseInOutEasing)

    /** Spatial motion on sizes, offsets and corner radii. */
    fun dp(): AnimationSpec<Dp> =
        if (expressive) spatialSpec(speed, bounce, visibilityThreshold = Dp.VisibilityThreshold)
        else tween(durationMillis = scaled(BASE_TRANSITION_MS), easing = EaseInOutEasing)

    /** Alpha / colour motion: critically damped (no overshoot), so fades never over-brighten. */
    fun fade(): AnimationSpec<Float> =
        if (expressive) effectsSpec(speed)
        else tween(durationMillis = scaled(BASE_TRANSITION_MS), easing = EaseInOutEasing)

    companion object {
        // The tuned baseline for the island's primary expand/collapse transition. Every tween-based
        // animation is expressed relative to this, so the duration slider scales them in proportion.
        const val BASE_TRANSITION_MS = 220

        // Standard ease-in-out — cubic-bezier(0.42, 0.0, 0.58, 1.0) — for AnimationStyle.EASE_IN_OUT.
        private val EaseInOutEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

        /**
         * A spatial spring based on the Material 3 expressive MotionScheme tokens: [speed] sets the
         * stiffness (the MotionScheme slow / default / fast values) and [bounce] the damping.
         * NORMAL damping (0.6, ~9% overshoot) is lower than the stock tokens' 0.8, whose ~1.5%
         * overshoot is imperceptible at cutout scale; SMALL restores that stock feel and BIG
         * (0.45, ~20% overshoot) is unmistakably springy. [visibilityThreshold] lets the spring
         * settle without a long tail.
         */
        private fun <T> spatialSpec(
            speed: AnimationSpeed,
            bounce: AnimationBounce,
            visibilityThreshold: T? = null,
        ): SpringSpec<T> {
            val stiffness = when (speed) {
                AnimationSpeed.SLOW -> 170f
                AnimationSpeed.DEFAULT -> 380f
                AnimationSpeed.FAST -> 800f
            }
            val dampingRatio = when (bounce) {
                AnimationBounce.BIG -> 0.45f
                AnimationBounce.NORMAL -> 0.6f
                AnimationBounce.SMALL -> 0.8f
            }
            return spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
        }

        /** The matching MotionScheme "effects" spring: critically damped, for alpha / colour. */
        private fun effectsSpec(speed: AnimationSpeed): SpringSpec<Float> = when (speed) {
            AnimationSpeed.SLOW -> spring(dampingRatio = 1f, stiffness = 300f)
            AnimationSpeed.DEFAULT -> spring(dampingRatio = 1f, stiffness = 700f)
            AnimationSpeed.FAST -> spring(dampingRatio = 1f, stiffness = 1600f)
        }
    }
}
