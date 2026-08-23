package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandPreview

// Shared building blocks used by more than one settings sub-screen. Kept `internal` so each
// screen file (same package, split across the SettingScreens/ folder) can reach them.

/** A labelled slider flanked by step buttons; commits on release or on each step tap. */
@Composable
internal fun AdjustableSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SliderRow(
            value = value,
            valueRange = valueRange,
            step = step,
            onValueChange = onValueChange,
            onCommit = onCommit,
        )
    }
}

/** The slider itself flanked by the -/+ step buttons, without any label. */
@Composable
private fun SliderRow(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = {
                onValueChange((value - step).coerceIn(valueRange))
                onCommit()
            },
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.cd_decrease))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onCommit,
            valueRange = valueRange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        )
        FilledTonalIconButton(
            onClick = {
                onValueChange((value + step).coerceIn(valueRange))
                onCommit()
            },
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_increase))
        }
    }
}

/**
 * A surface card laid out like [SettingsToggleCard] — title, short description, and a trailing
 * value in place of the switch — with the slider underneath.
 */
@Composable
internal fun SettingsSliderCard(
    shape: Shape,
    title: String,
    description: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            SliderRow(
                value = value,
                valueRange = valueRange,
                step = step,
                onValueChange = onValueChange,
                onCommit = onCommit,
            )
        }
    }
}

/**
 * A surface card wrapping a title/description and a trailing [Switch]. Pass [enabled] = false for a
 * setting that exists but can't be changed yet — the row dims and the switch stops responding,
 * which keeps the feature discoverable instead of hiding it.
 */
@Composable
internal fun SettingsToggleCard(
    shape: Shape,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

/**
 * Like [SettingsToggleCard] but the title/description area is clickable — tapping it runs [onClick]
 * (typically to open a detail screen), while the trailing [Switch] still toggles independently.
 * A chevron marks the row as navigable and a thin divider separates it from the switch, matching the
 * events / dynamic-tiles list rows.
 */
@Composable
internal fun SettingsToggleNavCard(
    shape: Shape,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Thin divider between the (tappable) row and the switch, matching the events list.
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/** A miniature "screen" showing the real device cutout and the island as the overlay would place it. */
@Composable
internal fun IslandPreviewPanel(
    background: Color,
    cutout: TopCutout?,
    widthPercent: Int,
    heightDp: Int,
    cornerTopLeftDp: Int,
    cornerTopRightDp: Int,
    cornerBottomLeftDp: Int,
    cornerBottomRightDp: Int,
    offsetXDp: Int,
    offsetYDp: Int,
    topMarginDp: Int = IslandDimensions.DEFAULT_TOP_MARGIN_DP,
    expanded: Boolean,
    event: IslandEvent,
    appearance: AppearanceSettings = AppearanceSettings(),
    showActions: Boolean = true,
    collapsedHeightDp: Int = IslandLayout.DEFAULT_COLLAPSED.heightDp,
) {
    val cutoutOutline = Color.White.copy(alpha = 0.28f)
    // Grow the panel so the island (at its offset) always fits without clipping.
    val panelHeight = (offsetYDp + heightDp + 32).coerceIn(150, 340).dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
    ) {
        val density = LocalDensity.current
        val panelWidth = maxWidth
        // The panel represents the screen, so the island width is that percentage of it.
        val islandWidth = panelWidth * (widthPercent / 100f)

        // A punch-hole's bounding rect is often taller than the hole (its top edge sits near
        // the screen edge), so use the smaller dimension as the diameter to draw a true circle.
        val diameter = cutout?.let {
            with(density) { minOf(it.widthPx, it.heightPx).toDp() }
        } ?: 28.dp
        val centerFraction = cutout?.centerXFraction ?: 0.5f

        // Real cutout: black hole with a faint outline so it reads on either background.
        Box(
            modifier = Modifier
                .offset(x = panelWidth * centerFraction - diameter / 2f, y = 8.dp)
                .size(diameter)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, cutoutOutline, CircleShape),
        )

        // The island, positioned exactly as the overlay would place it (top-centre + offset).
        Box(
            modifier = Modifier.offset(
                x = panelWidth / 2f + offsetXDp.dp - islandWidth / 2f,
                y = offsetYDp.dp,
            ),
        ) {
            IslandPreview(
                event = event,
                width = islandWidth,
                heightDp = heightDp,
                cornerTopLeftDp = cornerTopLeftDp,
                cornerTopRightDp = cornerTopRightDp,
                cornerBottomLeftDp = cornerBottomLeftDp,
                cornerBottomRightDp = cornerBottomRightDp,
                topMarginDp = topMarginDp,
                offsetXDp = offsetXDp,
                expanded = expanded,
                appearance = appearance,
                showActions = showActions,
                collapsedHeightDp = collapsedHeightDp,
            )
        }
    }
}

/**
 * The measured camera cutout at the top of the screen, shared by the settings previews so they can
 * draw the island against the device's real hole rather than a guess.
 */
internal data class TopCutout(
    val widthPx: Int,
    val heightPx: Int,
    val centerXFraction: Float,
)

/** Reads the device's top display cutout once, or null if there isn't one to represent. */
@Composable
internal fun rememberTopCutout(): TopCutout? {
    val view = LocalView.current
    return remember(view) {
        val displayCutout = view.rootWindowInsets?.displayCutout
        val rect = displayCutout?.boundingRectTop
        if (rect != null && rect.width() > 0 && rect.height() > 0) {
            val screenWidth = view.resources.displayMetrics.widthPixels.coerceAtLeast(1)
            TopCutout(
                widthPx = rect.width(),
                heightPx = rect.height(),
                centerXFraction = (rect.exactCenterX() / screenWidth).coerceIn(0f, 1f),
            )
        } else {
            null
        }
    }
}
