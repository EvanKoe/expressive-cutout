package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.GestureAction
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressivePillRow
import com.ekoehler.expressivecutout.ui.components.OptionSelectionCard
import com.ekoehler.expressivecutout.ui.components.PageTitle
import com.ekoehler.expressivecutout.ui.components.SelectableOption

/** Label shown for each gesture action in the options cards. */
private val GestureAction.titleRes: Int
    get() = when (this) {
        GestureAction.NONE -> R.string.gesture_action_none
        GestureAction.EXPAND -> R.string.gesture_action_expand
        GestureAction.SHRINK -> R.string.gesture_action_shrink
        GestureAction.OPEN_APP -> R.string.gesture_action_open_app
        GestureAction.DISMISS -> R.string.gesture_action_dismiss
    }

/** One-line explanation shown under each gesture action. */
private val GestureAction.descriptionRes: Int
    get() = when (this) {
        GestureAction.NONE -> R.string.gesture_action_none_desc
        GestureAction.EXPAND -> R.string.gesture_action_expand_desc
        GestureAction.SHRINK -> R.string.gesture_action_shrink_desc
        GestureAction.OPEN_APP -> R.string.gesture_action_open_app_desc
        GestureAction.DISMISS -> R.string.gesture_action_dismiss_desc
    }

/**
 * Settings page choosing what a tap, a swipe up, a swipe left and a swipe right do on the normal
 * cutout, and the same plus a swipe down on the expanded one. The normal cutout has no swipe down:
 * Android hands any fast downward swipe that starts in the status bar to the shade.
 */
@Composable
internal fun GestureScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PageTitle(text = stringResource(R.string.gestures_title))

        ExpressivePillRow(
            options = listOf(
                stringResource(R.string.gesture_tab_normal),
                stringResource(R.string.gesture_tab_expanded),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(bottom = 8.dp),
            fillWidth = true,
        )

        if (tab == 0) {
            GestureCard(
                titleRes = R.string.gesture_swipe_up,
                descriptionRes = R.string.gesture_swipe_up_desc,
                actions = listOf(GestureAction.NONE, GestureAction.DISMISS, GestureAction.OPEN_APP),
                selected = behaviour.normalSwipeUp,
                onSelect = viewModel::setNormalSwipeUp,
                isFirst = true,
            )
            GestureCard(
                titleRes = R.string.gesture_swipe_left,
                descriptionRes = R.string.gesture_swipe_left_desc,
                actions = listOf(GestureAction.NONE, GestureAction.DISMISS),
                selected = behaviour.normalSwipeLeft,
                onSelect = viewModel::setNormalSwipeLeft,
            )
            GestureCard(
                titleRes = R.string.gesture_swipe_right,
                descriptionRes = R.string.gesture_swipe_right_desc,
                actions = listOf(GestureAction.NONE, GestureAction.DISMISS),
                selected = behaviour.normalSwipeRight,
                onSelect = viewModel::setNormalSwipeRight,
            )
            GestureCard(
                titleRes = R.string.gesture_tap,
                descriptionRes = R.string.gesture_tap_desc,
                actions = listOf(GestureAction.NONE, GestureAction.EXPAND, GestureAction.OPEN_APP),
                selected = behaviour.normalTap,
                onSelect = viewModel::setNormalTap,
                isLast = true,
            )
        } else {
            GestureCard(
                titleRes = R.string.gesture_swipe_up,
                descriptionRes = R.string.gesture_swipe_up_desc,
                actions = listOf(
                    GestureAction.NONE,
                    GestureAction.SHRINK,
                    GestureAction.DISMISS,
                    GestureAction.OPEN_APP,
                ),
                selected = behaviour.expandedSwipeUp,
                onSelect = viewModel::setExpandedSwipeUp,
                isFirst = true,
            )
            GestureCard(
                titleRes = R.string.gesture_swipe_down,
                descriptionRes = R.string.gesture_swipe_down_desc,
                actions = listOf(GestureAction.NONE, GestureAction.OPEN_APP, GestureAction.DISMISS),
                selected = behaviour.expandedSwipeDown,
                onSelect = viewModel::setExpandedSwipeDown,
            )
            GestureCard(
                titleRes = R.string.gesture_swipe_left,
                descriptionRes = R.string.gesture_swipe_left_desc,
                actions = listOf(GestureAction.NONE, GestureAction.DISMISS),
                selected = behaviour.expandedSwipeLeft,
                onSelect = viewModel::setExpandedSwipeLeft,
            )
            GestureCard(
                titleRes = R.string.gesture_swipe_right,
                descriptionRes = R.string.gesture_swipe_right_desc,
                actions = listOf(GestureAction.NONE, GestureAction.DISMISS),
                selected = behaviour.expandedSwipeRight,
                onSelect = viewModel::setExpandedSwipeRight,
            )
            GestureCard(
                titleRes = R.string.gesture_tap,
                descriptionRes = R.string.gesture_tap_desc,
                actions = listOf(GestureAction.NONE, GestureAction.OPEN_APP, GestureAction.SHRINK),
                selected = behaviour.expandedTap,
                onSelect = viewModel::setExpandedTap,
                isLast = true,
            )
        }
    }
}

/** One gesture's card: a single-choice list of the [actions] it may trigger, chained into a group. */
@Composable
private fun GestureCard(
    titleRes: Int,
    descriptionRes: Int,
    actions: List<GestureAction>,
    selected: GestureAction,
    onSelect: (GestureAction) -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    OptionSelectionCard(
        modifier = if (isLast) Modifier.padding(bottom = 8.dp) else Modifier,
        title = stringResource(titleRes),
        description = stringResource(descriptionRes),
        options = actions.map { action ->
            SelectableOption(
                value = action,
                title = stringResource(action.titleRes),
                description = stringResource(action.descriptionRes),
            )
        },
        selectedValue = selected,
        onSelectionChange = onSelect,
        isFirst = isFirst,
        isLast = isLast,
    )
}
