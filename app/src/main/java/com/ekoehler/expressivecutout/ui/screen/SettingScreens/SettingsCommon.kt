package com.ekoehler.expressivecutout.ui.screen

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandPreview
import com.ekoehler.expressivecutout.overlay.MaterialIconCatalog
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.components.groupedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * which keeps the feature discoverable instead of hiding it. [content] draws inside the same card,
 * under the switch row, for a setting whose own options belong with it rather than in a card of
 * their own. It keeps the card's horizontal padding but none below it, so content that animates
 * itself away leaves no gap behind; content that stays adds its own bottom padding.
 */
@Composable
internal fun SettingsToggleCard(
    shape: Shape = groupedShape(),
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    /** Ink for the title; the description takes it at 80% opacity. Null keeps the neutral pair. */
    contentColor: Color? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
                    color = (contentColor ?: MaterialTheme.colorScheme.onSurface).copy(alpha = contentAlpha),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = (contentColor?.copy(alpha = 0.8f) ?: MaterialTheme.colorScheme.onSurfaceVariant)
                        .copy(alpha = contentAlpha),
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
        if (content != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                content = content,
            )
        }
    }
}

/**
 * Like [SettingsToggleCard] but the whole card is clickable — tapping it runs [onClick] (typically
 * to open a detail screen) and ripples across the full card, while the trailing [Switch] consumes
 * its own taps and still toggles independently.
 * A chevron marks the row as navigable and a thin divider separates it from the switch, matching the
 * events / dynamic-tiles list rows. [leading] optionally draws content ahead of the title, such as
 * an event's icon thumbnail.
 */
@Composable
internal fun SettingsToggleNavCard(
    shape: Shape,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(14.dp))
                }
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
            // Thin divider between the title area and the switch, matching the events list.
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
    var dynamicHeightDp by remember(expanded, event.id, topMarginDp, appearance.actionButtonHeightDp, showActions) {
        mutableStateOf(heightDp)
    }
    val effectiveIslandHeightDp = if (expanded) maxOf(heightDp, dynamicHeightDp) else heightDp
    val cutoutOutline = Color.White.copy(alpha = 0.28f)
    // Grow the panel so the island (at its offset) always fits without clipping.
    val panelHeight = (offsetYDp + effectiveIslandHeightDp + 32).coerceIn(150, 420).dp

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
                heightDp = effectiveIslandHeightDp,
                cornerTopLeftDp = cornerTopLeftDp,
                cornerTopRightDp = cornerTopRightDp,
                cornerBottomLeftDp = cornerBottomLeftDp,
                cornerBottomRightDp = cornerBottomRightDp,
                topMarginDp = topMarginDp,
                expanded = expanded,
                appearance = appearance,
                showActions = showActions,
                collapsedHeightDp = collapsedHeightDp,
                onHeightMeasured = { dynamicHeightDp = it },
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

@Composable
internal fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    bgColor: Color = MaterialTheme.colorScheme.surface,
    fgColor: Color? = null,
    hapticsOnClick: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (hapticsOnClick) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                onClick()
            }),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A round badge previewing a chosen icon: a [containerColor] disc (a faint neutral one when unset)
 * behind the glyph — a picked image, a Material vector, or [placeholder] when no icon has been
 * chosen yet. [glyphColor] overrides the ink the vector is drawn in, which otherwise contrasts
 * with the disc. Mirrors how the overlay draws the icon on the island.
 */
@Composable
internal fun EmptyIconThumbnail(
    source: IconSource?,
    containerColor: CutoutColor?,
    size: Dp = 48.dp,
    glyphColor: CutoutColor? = null,
    placeholder: ImageVector = Icons.Rounded.Edit,
) {
    val context = LocalContext.current
    val disc = containerColor?.resolve() ?: MaterialTheme.colorScheme.surfaceVariant
    // Ink that reads on the disc: dark on a light fill, light on a dark one.
    val glyph = glyphColor?.resolve()
        ?: if (disc.luminance() > 0.5f) Color.Black.copy(alpha = 0.75f) else Color.White

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = when (val current = source) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                Uri.parse(current.uri).loadImageBitmapOrNull(context)
            }
            is IconSource.Material, null -> null
        }
    }
    val materialIcon = (source as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(disc),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        when {
            loaded != null -> Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.size(size * 0.66f).clip(CircleShape),
            )

            materialIcon != null -> Icon(
                imageVector = materialIcon,
                contentDescription = null,
                tint = glyph,
                modifier = Modifier.size(size * 0.5f),
            )

            else -> Icon(
                imageVector = placeholder,
                contentDescription = null,
                tint = glyph.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}
