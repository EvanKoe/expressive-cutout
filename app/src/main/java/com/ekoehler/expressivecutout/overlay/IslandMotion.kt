package com.ekoehler.expressivecutout.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
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
import kotlin.math.sqrt

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

    /**
     * Like [dp] but critically damped (no overshoot), for a size that grows incrementally rather than
     * in one jump — the assistant cutout's height as its answer streams in. The bouncy spatial spring
     * re-overshoots every time the target nudges up (once per token), making the cutout visibly bob;
     * a critically damped spring chases the growing height smoothly instead. Ignores the bounce knob
     * by design, but still honours the speed knob and the duration slider.
     */
    fun dpSmooth(): AnimationSpec<Dp> =
        if (expressive) spring(dampingRatio = 1f, stiffness = spatialStiffness(speed), visibilityThreshold = Dp.VisibilityThreshold)
        else tween(durationMillis = scaled(BASE_TRANSITION_MS), easing = EaseInOutEasing)

    /**
     * The normal cutout's tap "boop" — the scale dip under a finger and the settle back on release.
     * A spring rather than a tween because the two halves overlap: a quick tap lifts the finger
     * before the dip has landed, and a spring carries the current velocity into the release instead
     * of restarting an easing curve from a standstill, so the whole tap reads as one motion.
     * Critically damped and stiff, so it stays immediate without springing past the resting scale.
     */
    fun boop(): AnimationSpec<Float> =
        if (expressive) spring(dampingRatio = 1f, stiffness = boopStiffness(speed), visibilityThreshold = 0.0005f)
        else tween(durationMillis = scaled(140), easing = EaseInOutEasing)

    /**
     * Runs the expanded island's tap "pop" on [scale]: a swell out to [peak] and back to rest.
     *
     * Driven as one continuous motion rather than an out-then-back pair. Chaining two animations
     * stalls at the apex — the outward leg has to decelerate to zero before the return can start,
     * and that dead frame is what reads as a stutter. Instead the spring is aimed straight at the
     * resting scale and handed enough outward velocity to overshoot to [peak] on the way there, so
     * the swell and the settle are a single arc.
     *
     * The overshoot is set by the launch velocity, not by the damping, so [AnimationBounce] is
     * deliberately not consulted: the pop is always exactly [peak] tall and the damping only decides
     * how it settles. [AnimationStyle.EASE_IN_OUT] has no velocity to carry, so it keeps a two-part
     * tween, weighted so the return is the slower half.
     */
    suspend fun pop(scale: Animatable<Float, AnimationVector1D>, peak: Float) {
        if (expressive) {
            // A spring launched from its own target with velocity v peaks at v / omega * POP_PEAK_RATIO.
            // Inverting that keeps the swell at [peak] whatever stiffness the speed knob picks.
            val stiffness = spatialStiffness(speed)
            val velocity = (peak - REST_SCALE) * sqrt(stiffness) / POP_PEAK_RATIO
            scale.animateTo(
                targetValue = REST_SCALE,
                animationSpec = spring(dampingRatio = POP_DAMPING, stiffness = stiffness, visibilityThreshold = 0.0005f),
                initialVelocity = velocity,
            )
        } else {
            scale.animateTo(peak, tween(durationMillis = scaled(80), easing = EaseInOutEasing))
            scale.animateTo(REST_SCALE, tween(durationMillis = scaled(160), easing = EaseInOutEasing))
        }
    }

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

        // The island's resting scale, i.e. the one the boop and the pop depart from and return to.
        private const val REST_SCALE = 1f

        // Damping for [pop]'s single-arc swell. Loose enough that the spring actually overshoots,
        // tight enough that it settles in one swing rather than wobbling.
        private const val POP_DAMPING = 0.55f

        // How far a spring launched from its target overshoots, as a fraction of velocity / omega:
        // exp(-z * atan(sqrt(1 - z^2) / z) / sqrt(1 - z^2)), evaluated at z = POP_DAMPING.
        // Keep in step with POP_DAMPING — it is only correct for that value.
        private const val POP_PEAK_RATIO = 0.5216f

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
            val stiffness = spatialStiffness(speed)
            val dampingRatio = when (bounce) {
                AnimationBounce.BIG -> 0.45f
                AnimationBounce.NORMAL -> 0.6f
                AnimationBounce.SMALL -> 0.8f
            }
            return spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
        }

        /** The MotionScheme slow / default / fast stiffness tokens, shared by the spatial springs. */
        private fun spatialStiffness(speed: AnimationSpeed): Float = when (speed) {
            AnimationSpeed.SLOW -> 170f
            AnimationSpeed.DEFAULT -> 380f
            AnimationSpeed.FAST -> 800f
        }

        /**
         * Stiffness for the tap boop. Higher than [spatialStiffness] across the board: the dip is a
         * tiny 4% travel that has to track the finger, so it needs to land in well under the time
         * the island itself takes to move.
         */
        private fun boopStiffness(speed: AnimationSpeed): Float = when (speed) {
            AnimationSpeed.SLOW -> 400f
            AnimationSpeed.DEFAULT -> 900f
            AnimationSpeed.FAST -> 1800f
        }

        /** The matching MotionScheme "effects" spring: critically damped, for alpha / colour. */
        private fun effectsSpec(speed: AnimationSpeed): SpringSpec<Float> = when (speed) {
            AnimationSpeed.SLOW -> spring(dampingRatio = 1f, stiffness = 300f)
            AnimationSpeed.DEFAULT -> spring(dampingRatio = 1f, stiffness = 700f)
            AnimationSpeed.FAST -> spring(dampingRatio = 1f, stiffness = 1600f)
        }
    }
}
