package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * One entry of an [OptionSelectionCard]. [value] is what gets handed back on selection, so callers
 * can pass an enum entry, an id, or any other key and stay type-safe.
 */
data class SelectableOption<T>(
    val value: T,
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
)

/**
 * A titled card holding a single-choice list of options, each with its own title and optional
 * description. The selected row fills with the theme's secondary container so it reads as chosen in
 * both light and dark dynamic-colour schemes; [onSelectionChange] fires with the tapped option's
 * value. Rows already tapped don't re-emit, and the whole group is exposed as one radio group to
 * accessibility services. Pass [shape] to slot the card into a grouped settings list.
 */
@Composable
fun <T> OptionSelectionCard(
    title: String,
    options: List<SelectableOption<T>>,
    selectedValue: T?,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 16.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEachIndexed { index, option ->
                    OptionRow(
                        option = option,
                        shape = optionShape(
                            isFirst = index == 0,
                            isLast = index == options.lastIndex,
                        ),
                        selected = option.value == selectedValue,
                        enabled = enabled && option.enabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectionChange(option.value)
                        },
                    )
                }
            }
        }
    }
}

/** Grouped-list row shape: the group's outer corners round, the ones between rows stay tight. */
private fun optionShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 24.dp else 4.dp,
    topEnd = if (isFirst) 24.dp else 4.dp,
    bottomStart = if (isLast) 24.dp else 4.dp,
    bottomEnd = if (isLast) 24.dp else 4.dp,
)

/**
 * A single selectable row of [OptionSelectionCard]. Instead of the flat bounded ripple, pressing a
 * row squashes it on a bouncy spring, then it settles into the selected container colour — the
 * motion feedback Material 3 Expressive uses in place of a plain ripple wash. A soft
 * secondary-tinted ripple rides along underneath so the touch point still reads.
 */
@Composable
private fun <T> OptionRow(
    option: SelectableOption<T>,
    shape: Shape,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "optionContainerColor",
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "optionTitleColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = bouncy,
        label = "optionScale",
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(shape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.secondary),
                enabled = enabled && !selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor.copy(alpha = contentAlpha),
                )
                option.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}
