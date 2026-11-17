package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * How much of the row a selected pill claims relative to an unselected one when the pills share the
 * full width. The row still measures to exactly the space available — the others simply give up
 * what the selected one takes.
 */
private const val PILL_WEIGHT = 1f
private const val PILL_WEIGHT_SELECTED = 1.4f

/**
 * A Material 3 "expressive" single-choice selector built from individually sized pills that scroll
 * horizontally when they overflow. Unlike [ExpressiveSegmentedRow] — which splits the width evenly
 * and slides one indicator — every option keeps its own filled container, so long labels stay
 * readable instead of ellipsising. Use it for option sets whose labels vary in length. Set
 * [fillWidth] for a short, fixed set of options that should span the row instead: the pills share
 * exactly the available width — the selected one taking a little more than the rest — and the row
 * no longer scrolls. Purely presentational.
 */
@Composable
fun ExpressivePillRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
    disabledIndices: Set<Int> = emptySet(),
    fillWidth: Boolean = false,
) {
    if (fillWidth) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(PILL_GAP),
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val weight by animateFloatAsState(
                    targetValue = if (selected) PILL_WEIGHT_SELECTED else PILL_WEIGHT,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "pillWeight",
                )
                Pill(
                    label = label,
                    shape = pillShape(index = index, lastIndex = options.lastIndex),
                    selected = selected,
                    disabled = index in disabledIndices,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(weight),
                    growWhenSelected = false,
                )
            }
        }
        return
    }

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
            Pill(
                label = label,
                shape = pillShape(index = index, lastIndex = options.lastIndex),
                selected = index == selectedIndex,
                disabled = index in disabledIndices,
                onClick = { onSelect(index) },
            )
        }
    }
}

/**
 * One pill. Sizes itself to its label unless the caller constrains it — the fill-width row passes a
 * weight, the scrolling row passes nothing. Clear [growWhenSelected] when the caller already widens
 * the selected pill, so the padding doesn't squeeze the label inside a width it no longer sets.
 */
@Composable
private fun Pill(
    label: String,
    shape: Shape,
    selected: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    growWhenSelected: Boolean = true,
) {
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected && growWhenSelected) PILL_PADDING_SELECTED else PILL_PADDING,
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
        modifier = modifier
            .height(PILL_HEIGHT)
            .defaultMinSize(minWidth = PILL_MIN_WIDTH)
            .selectable(
                selected = selected,
                enabled = !disabled,
                onClick = onClick,
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
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Rounds only the row's outer edges, so the pills read as one connected group. */
private fun pillShape(index: Int, lastIndex: Int) = RoundedCornerShape(
    topStart = if (index == 0) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
    bottomStart = if (index == 0) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
    topEnd = if (index == lastIndex) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
    bottomEnd = if (index == lastIndex) PILL_OUTER_RADIUS else PILL_INNER_RADIUS,
)
