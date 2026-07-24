package com.ekoehler.expressivecutout.ui.screen

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
import com.ekoehler.expressivecutout.ui.AppViewModel
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
        BehaviourSliderRow(
            shape = groupedShape(isFirst = true, isLast = false),
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
        if (behaviour.expandedAutoCollapse) {
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
            shape = groupedShape(isFirst = false, isLast = true),
            title = stringResource(R.string.behaviour_shrink_swipe_up),
            description = stringResource(R.string.behaviour_shrink_swipe_up_desc),
            checked = behaviour.shrinkOnSwipeUp,
            onCheckedChange = viewModel::setShrinkOnSwipeUp,
        )
    }
}

@Composable
private fun BehaviourSliderRow(
    shape: Shape,
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
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
                step = 1f,
                onValueChange = onValueChange,
                onCommit = onCommit,
            )
        }
    }
}
