package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pill metrics: a fixed height, a hairline gap so the row reads as one connected group, and the
 * horizontal padding that gives each pill its width — the selected one grows to the wider value.
 */
private val PILL_HEIGHT = 48.dp
private val PILL_GAP = 4.dp
private val PILL_MIN_WIDTH = 64.dp
private val PILL_OUTER_RADIUS = 24.dp
private val PILL_INNER_RADIUS = 8.dp
private val PILL_PADDING = 20.dp
private val PILL_PADDING_SELECTED = 32.dp

/**
 * A Material 3 "expressive" single-choice selector built from individually sized pills that scroll
 * horizontally when they overflow. Unlike [ExpressiveSegmentedRow] — which splits the width evenly
 * and slides one indicator — every option keeps its own filled container, so long labels stay
 * readable instead of ellipsising. Use it for option sets whose labels vary in length. Purely
 * presentational.
 */
@Composable
fun ExpressivePillRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
    disabledIndices: Set<Int> = emptySet(),
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, options.size) {
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        val fullyVisible = item != null &&
            item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(PILL_GAP),
    ) {
        itemsIndexed(options) { index, label ->
            val selected = index == selectedIndex
            val disabled = index in disabledIndices
            val shape = RoundedCornerShape(
                topStart = if (index == 0) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
                bottomStart = if (index == 0) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
                topEnd = if (index == options.lastIndex) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
                bottomEnd = if (index == options.lastIndex) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
            )
            val horizontalPadding by animateDpAsState(
                targetValue = if (selected) PILL_PADDING_SELECTED else PILL_PADDING,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "pillWidth",
            )
            val containerColor by animateColorAsState(
                targetValue = when {
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "pillContainer",
            )
            val contentColor by animateColorAsState(
                targetValue = when {
                    disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    selected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "pillContent",
            )
            Surface(
                shape = shape,
                color = containerColor,
                modifier = Modifier
                    .height(PILL_HEIGHT)
                    .defaultMinSize(minWidth = PILL_MIN_WIDTH)
                    .selectable(
                        selected = selected,
                        enabled = !disabled,
                        onClick = { onSelect(index) },
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = contentColor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
