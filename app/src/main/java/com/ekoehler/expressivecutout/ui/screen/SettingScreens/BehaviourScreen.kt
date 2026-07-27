package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.SwipeDismissDirection
import com.ekoehler.expressivecutout.data.SwipeDismissTarget
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlin.math.roundToInt

/** Grouped-list item shape: large outer corners at the group ends, small between items. */
private fun groupedShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 32.dp else 4.dp,
    topEnd = if (isFirst) 32.dp else 4.dp,
    bottomStart = if (isLast) 32.dp else 4.dp,
    bottomEnd = if (isLast) 32.dp else 4.dp,
)

@Composable
internal fun BehaviourScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    var animationMs by remember(behaviour.animationDurationMs) {
        mutableStateOf(behaviour.animationDurationMs.toFloat())
    }
    var normalSeconds by remember(behaviour.normalDurationSeconds) {
        mutableStateOf(behaviour.normalDurationSeconds.toFloat())
    }
    var seconds by remember(behaviour.expandedCollapseSeconds) {
        mutableStateOf(behaviour.expandedCollapseSeconds.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Grouped list: the first item's top corners and the last item's bottom corners round.
        SettingsToggleCard(
            shape = groupedShape(isFirst = true, isLast = false),
            title = stringResource(R.string.behaviour_hide_lockscreen),
            description = stringResource(R.string.behaviour_hide_lockscreen_desc),
            checked = behaviour.hideOnLockscreen,
            onCheckedChange = viewModel::setHideOnLockscreen,
        )
        BehaviourSliderRow(
            shape = groupedShape(isFirst = false, isLast = false),
            label = stringResource(R.string.behaviour_animation_duration),
            valueText = "${animationMs.roundToInt()} ms",
            value = animationMs,
            valueRange = BehaviourSettings.MIN_ANIMATION_DURATION_MS.toFloat()..
                BehaviourSettings.MAX_ANIMATION_DURATION_MS.toFloat(),
            step = 20f,
            onValueChange = { animationMs = it },
            onCommit = { viewModel.setAnimationDurationMs(animationMs.roundToInt()) },
        )
        BehaviourSliderRow(
            shape = groupedShape(isFirst = false, isLast = false),
            label = stringResource(R.string.behaviour_normal_duration),
            valueText = "${normalSeconds.roundToInt()} s",
            value = normalSeconds,
            valueRange = BehaviourSettings.MIN_NORMAL_SECONDS.toFloat()..
                BehaviourSettings.MAX_NORMAL_SECONDS.toFloat(),
            onValueChange = { normalSeconds = it },
            onCommit = { viewModel.setNormalDurationSeconds(normalSeconds.roundToInt()) },
        )
        SettingsToggleCard(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_auto_collapse),
            description = stringResource(R.string.behaviour_auto_collapse_desc),
            checked = behaviour.expandedAutoCollapse,
            onCheckedChange = viewModel::setExpandedAutoCollapse,
        )
        AnimatedVisibility(visible = behaviour.expandedAutoCollapse) {
            BehaviourSliderRow(
                shape = groupedShape(isFirst = false, isLast = false),
                label = stringResource(R.string.behaviour_collapse_delay),
                valueText = "${seconds.roundToInt()} s",
                value = seconds,
                valueRange = BehaviourSettings.MIN_COLLAPSE_SECONDS.toFloat()..
                    BehaviourSettings.MAX_COLLAPSE_SECONDS.toFloat(),
                onValueChange = { seconds = it },
                onCommit = { viewModel.setExpandedCollapseSeconds(seconds.roundToInt()) },
            )
        }
        SettingsToggleCard(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_disappear),
            description = stringResource(R.string.behaviour_disappear_desc),
            checked = behaviour.expandedDisappearOnShrink,
            onCheckedChange = viewModel::setExpandedDisappearOnShrink,
        )
        SettingsToggleCard(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_notif_auto_expand),
            description = stringResource(R.string.behaviour_notif_auto_expand_desc),
            checked = behaviour.notificationsAutoExpand,
            onCheckedChange = viewModel::setNotificationsAutoExpand,
        )
        SettingsToggleCard(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_action_buttons),
            description = stringResource(R.string.behaviour_action_buttons_desc),
            checked = behaviour.showActionButtons,
            onCheckedChange = viewModel::setShowActionButtons,
        )
        SettingsToggleCard(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_shrink_swipe_up),
            description = stringResource(R.string.behaviour_shrink_swipe_up_desc),
            checked = behaviour.shrinkOnSwipeUp,
            onCheckedChange = viewModel::setShrinkOnSwipeUp,
        )
        SettingsToggleCard(
            shape = groupedShape(isFirst = false, isLast = !behaviour.swipeToDismiss),
            title = stringResource(R.string.behaviour_swipe_dismiss),
            description = stringResource(R.string.behaviour_swipe_dismiss_desc),
            checked = behaviour.swipeToDismiss,
            onCheckedChange = viewModel::setSwipeToDismiss,
        )
        AnimatedVisibility(visible = behaviour.swipeToDismiss) {
            Column (verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BehaviourSegmentedRow(
                    shape = groupedShape(isFirst = false, isLast = false),
                    label = stringResource(R.string.behaviour_swipe_direction),
                    options = listOf(
                        stringResource(R.string.swipe_dir_left),
                        stringResource(R.string.swipe_dir_right),
                        stringResource(R.string.swipe_dir_both),
                    ),
                    selectedIndex = behaviour.swipeDismissDirection.ordinal,
                    onSelect = { viewModel.setSwipeDismissDirection(SwipeDismissDirection.entries[it]) },
                )
                BehaviourSegmentedRow(
                    shape = groupedShape(isFirst = false, isLast = true),
                    label = stringResource(R.string.behaviour_swipe_target),
                    options = listOf(
                        stringResource(R.string.swipe_target_expanded),
                        stringResource(R.string.swipe_target_both),
                        stringResource(R.string.swipe_target_normal),
                    ),
                    selectedIndex = behaviour.swipeDismissTarget.ordinal,
                    onSelect = { viewModel.setSwipeDismissTarget(SwipeDismissTarget.entries[it]) },
                )
            }
        }
    }
}

@Composable
private fun BehaviourSegmentedRow(
    shape: Shape,
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
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
            ExpressiveSegmentedRow(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BehaviourSliderRow(
    shape: Shape,
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            AdjustableSlider(
                label = label,
                valueText = valueText,
                value = value,
                valueRange = valueRange,
                step = step,
                onValueChange = onValueChange,
                onCommit = onCommit,
            )
        }
    }
}
