package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.ActionButtonAnimation
import com.ekoehler.expressivecutout.data.AnimationBounce
import com.ekoehler.expressivecutout.data.AnimationSpeed
import com.ekoehler.expressivecutout.data.AnimationStyle
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.PageTransitionStyle
import com.ekoehler.expressivecutout.overlay.IslandMotion
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Grouped-list item shape: large outer corners at the group ends, small between items. */
private fun groupedShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 32.dp else 4.dp,
    topEnd = if (isFirst) 32.dp else 4.dp,
    bottomStart = if (isLast) 32.dp else 4.dp,
    bottomEnd = if (isLast) 32.dp else 4.dp,
)

/**
 * The Animations screen: picks between the Material 3 expressive spring motion (with a slow /
 * default / fast speed) and a standard ease-in-out tween (with a millisecond duration slider).
 * Only the controls that apply to the chosen style are shown.
 */
@Composable
internal fun AnimationScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    var animationMs by remember(behaviour.animationDurationMs) {
        mutableStateOf(behaviour.animationDurationMs.toFloat())
    }
    val expressive = behaviour.animationStyle == AnimationStyle.EXPRESSIVE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AnimationExampleCard(
            shape = groupedShape(isFirst = true, isLast = true),
            speed = behaviour.animationSpeed,
            bounce = behaviour.animationBounce,
            durationMs = behaviour.animationDurationMs,
        )

        Column(
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimationSegmentedRow(
                shape = groupedShape(isFirst = true, isLast = false),
                label = stringResource(R.string.animation_style),
                options = listOf(
                    stringResource(R.string.animation_style_expressive),
                    stringResource(R.string.animation_style_ease),
                ),
                selectedIndex = behaviour.animationStyle.ordinal,
                onSelect = { viewModel.setAnimationStyle(AnimationStyle.entries[it]) },
            )

            // Expressive springs have no fixed duration: their pace is the slow / default / fast speed,
            // and their overshoot the bounce.
            AnimatedVisibility(visible = expressive) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimationSegmentedRow(
                        shape = groupedShape(isFirst = false, isLast = false),
                        label = stringResource(R.string.animation_speed),
                        options = listOf(
                            stringResource(R.string.animation_speed_slow),
                            stringResource(R.string.animation_speed_default),
                            stringResource(R.string.animation_speed_fast),
                        ),
                        selectedIndex = behaviour.animationSpeed.ordinal,
                        onSelect = { viewModel.setAnimationSpeed(AnimationSpeed.entries[it]) },
                    )
                    AnimationSegmentedRow(
                        shape = groupedShape(isFirst = false, isLast = true),
                        label = stringResource(R.string.animation_bounce),
                        options = listOf(
                            stringResource(R.string.animation_bounce_big),
                            stringResource(R.string.animation_bounce_normal),
                            stringResource(R.string.animation_bounce_small),
                        ),
                        selectedIndex = behaviour.animationBounce.ordinal,
                        onSelect = { viewModel.setAnimationBounce(AnimationBounce.entries[it]) },
                    )
                }
            }
            // The ease-in-out tween is the one timed in milliseconds, so the slider only applies to it.
            AnimatedVisibility(visible = !expressive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = groupedShape(isFirst = false, isLast = true),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        AdjustableSlider(
                            label = stringResource(R.string.behaviour_animation_duration),
                            valueText = "${animationMs.roundToInt()} ms",
                            value = animationMs,
                            valueRange = BehaviourSettings.MIN_ANIMATION_DURATION_MS.toFloat()..
                                    BehaviourSettings.MAX_ANIMATION_DURATION_MS.toFloat(),
                            step = 20f,
                            onValueChange = { animationMs = it },
                            onCommit = { viewModel.setAnimationDurationMs(animationMs.roundToInt()) },
                        )
                    }
                }
            }
        }

        // The press reaction of the action / reply buttons is independent of the primary motion
        // style above, so it lives in its own group.
        AnimationSegmentedRow(
            shape = groupedShape(isFirst = true, isLast = true),
            label = stringResource(R.string.animation_button),
            options = listOf(
                stringResource(R.string.animation_button_scale),
                stringResource(R.string.animation_button_expand),
            ),
            selectedIndex = behaviour.actionButtonAnimation.ordinal,
            onSelect = { viewModel.setActionButtonAnimation(ActionButtonAnimation.entries[it]) },
        )

        // In-app navigation motion. Unlike everything above it does not touch the island, so it
        // sits last, in its own group.
        AnimationSegmentedRow(
            shape = groupedShape(isFirst = true, isLast = true),
            label = stringResource(R.string.appearance_page_transition_title),
            description = stringResource(R.string.appearance_page_transition_desc),
            options = listOf(
                stringResource(R.string.appearance_page_transition_fade),
                stringResource(R.string.appearance_page_transition_slide),
            ),
            selectedIndex = PageTransitionStyle.entries.indexOf(appearance.pageTransitionStyle),
            onSelect = { viewModel.setPageTransitionStyle(PageTransitionStyle.entries[it]) },
        )
    }
}

/**
 * The example pills' collapsed / expanded sizes, loosely matching the real cutout's proportions.
 */
private val EXAMPLE_COLLAPSED_SIZE = DpSize(96.dp, 30.dp)
private val EXAMPLE_EXPANDED_SIZE = DpSize(230.dp, 56.dp)

/**
 * A live side-by-side example of the two animation styles: both pills expand and collapse on a
 * shared clock, each driven by [IslandMotion] — the very same spec builder the overlay island
 * uses — so the difference shown here is exactly the difference on the real cutout. The expressive
 * pill follows the selected speed and the ease-in-out pill the duration slider, so tweaking either
 * control is previewed immediately.
 */
@Composable
private fun AnimationExampleCard(
    shape: Shape,
    speed: AnimationSpeed,
    bounce: AnimationBounce,
    durationMs: Int,
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_300)
            expanded = !expanded
        }
    }
    val expressiveMotion = remember(speed, bounce) {
        IslandMotion(AnimationStyle.EXPRESSIVE, speed, bounce, BehaviourSettings.DEFAULT_ANIMATION_DURATION_MS)
    }
    val easeMotion = remember(durationMs) {
        IslandMotion(AnimationStyle.EASE_IN_OUT, AnimationSpeed.DEFAULT, AnimationBounce.NORMAL, durationMs)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExamplePill(
                label = stringResource(R.string.animation_style_expressive),
                motion = expressiveMotion,
                expanded = expanded,
            )
            ExamplePill(
                label = stringResource(R.string.animation_style_ease),
                motion = easeMotion,
                expanded = expanded,
            )
        }
    }
}

/** One example pill: its width, height and corners animate with [motion]'s dp spec — the same
 *  properties (and spec) the real island animates between its normal and expanded shapes. */
@Composable
private fun ExamplePill(label: String, motion: IslandMotion, expanded: Boolean) {
    val target = if (expanded) EXAMPLE_EXPANDED_SIZE else EXAMPLE_COLLAPSED_SIZE
    val width by animateDpAsState(target.width, motion.dp(), label = "exampleWidth")
    val height by animateDpAsState(target.height, motion.dp(), label = "exampleHeight")
    val corner by animateDpAsState(
        if (expanded) 24.dp else EXAMPLE_COLLAPSED_SIZE.height / 2,
        motion.dp(),
        label = "exampleCorner",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Reserve the expanded height so the row below doesn't jump while the pill grows.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EXAMPLE_EXPANDED_SIZE.height),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(width, height)
                    .clip(RoundedCornerShape(corner))
                    .background(MaterialTheme.colorScheme.inverseSurface),
            )
        }
    }
}

@Composable
private fun AnimationSegmentedRow(
    shape: Shape,
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    description: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExpressiveSegmentedRow(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
