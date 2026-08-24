package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.AppColorFallback
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.screen.ColorPickerDialog
import com.ekoehler.expressivecutout.ui.screen.ColorSwatch
import com.ekoehler.expressivecutout.ui.screen.CustomColorSwatch
import com.ekoehler.expressivecutout.ui.screen.dynamicDescription

/**
 * The predefined swatches [ColorPickerCard] shows by default: black, white, dark/light grey, then
 * blue, red and green. Any screen can override the set by passing its own list to [ColorPickerCard].
 */
val DefaultPresetColors: List<Long> = listOf(
    0xFF0A0A0A, // black
    0xFFFFFFFF, // white
    0xFF444444, // dark grey
    0xFFBBBBBB, // light grey
    0xFF3B82F6, // blue
    0xFFEF4444, // red
    0xFF22C55E, // green
)

/** The Material You dynamic roles [ColorPickerCard] offers by default, in display order. */
private val DefaultDynamicRoles = listOf(DynamicRole.PRIMARY, DynamicRole.SECONDARY, DynamicRole.TERTIARY)

/**
 * Reusable segmented row for choosing fallback behavior when app icon color is selected
 * but no active notification provides an app icon.
 */
@Composable
fun AppColorFallbackRow(
    fallback: AppColorFallback,
    onSelect: (AppColorFallback) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackOptions = listOf(
        AppColorFallback.ADAPTIVE to R.string.app_color_fallback_adaptive,
        AppColorFallback.DYNAMIC_THEME to R.string.app_color_fallback_dynamic,
        AppColorFallback.OLED_BLACK to R.string.app_color_fallback_oled,
    )

    Column(
        modifier = modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.app_color_fallback_title),
            style = MaterialTheme.typography.titleSmall,
        )
        ExpressiveSegmentedRow(
            options = fallbackOptions.map { stringResource(it.second) },
            selectedIndex = fallbackOptions.indexOfFirst { it.first == fallback }.coerceAtLeast(0),
            onSelect = { index -> onSelect(fallbackOptions[index].first) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Contextual description card showing user what the active color swatch selection does.
 */
@Composable
fun ColorSelectionTooltip(
    text: String?,
    modifier: Modifier = Modifier,
) {
    if (text == null) return
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().padding(top = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun ColorPickerCard(
    label: String? = null,
    selected: CutoutColor?,
    onSelect: (CutoutColor?) -> Unit,
    defaultLabel: String? = null,
    defaultColor: Color? = null,
    presetColors: List<Long> = DefaultPresetColors,
    dynamicRoles: List<DynamicRole> = DefaultDynamicRoles,
    roundedCorners: Dp = 24.dp,
    allowAppIcon: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val customArgb = (selected as? CutoutColor.Solid)?.argb
        ?.takeIf { argb -> presetColors.none { it == argb } }
    val currentColor = selected?.resolve() ?: defaultColor ?: Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(roundedCorners),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (label != null) {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (defaultLabel != null) {
                    ColorSwatch(
                        color = defaultColor ?: MaterialTheme.colorScheme.primary,
                        selected = selected == null,
                        badge = Icons.Rounded.RestartAlt,
                        badgeDescription = defaultLabel,
                        onClick = { onSelect(null) },
                    )
                }

                if (allowAppIcon) {
                    ColorSwatch(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        selected = selected is CutoutColor.AppIcon,
                        badgePainter = painterResource(R.drawable.ic_play_store),
                        badgeDescription = stringResource(R.string.cd_color_app_icon),
                        onClick = {
                            val currentFallback = (selected as? CutoutColor.AppIcon)?.fallback ?: AppColorFallback.ADAPTIVE
                            onSelect(CutoutColor.AppIcon(currentFallback))
                        },
                    )
                }

                dynamicRoles.forEach { role ->
                    ColorSwatch(
                        color = CutoutColor.Dynamic(role).resolve(),
                        selected = (selected as? CutoutColor.Dynamic)?.role == role,
                        badge = Icons.Rounded.AutoAwesome,
                        badgeDescription = role.dynamicDescription(),
                        onClick = { onSelect(CutoutColor.Dynamic(role)) },
                    )
                }
                CustomColorSwatch(
                    selectedColor = customArgb?.let { Color(it) },
                    onClick = { showPicker = true },
                )
                presetColors.forEach { argb ->
                    ColorSwatch(
                        color = Color(argb),
                        selected = selected == CutoutColor.Solid(argb),
                        onClick = { onSelect(CutoutColor.Solid(argb)) },
                    )
                }
            }

            AnimatedVisibility(visible = allowAppIcon && selected is CutoutColor.AppIcon) {
                val fallback = (selected as? CutoutColor.AppIcon)?.fallback ?: AppColorFallback.ADAPTIVE
                AppColorFallbackRow(
                    fallback = fallback,
                    onSelect = { onSelect(CutoutColor.AppIcon(it)) },
                )
            }

            val tooltipText = when {
                selected == null -> stringResource(R.string.tooltip_default_reset)
                selected is CutoutColor.AppIcon -> stringResource(R.string.tooltip_app_icon)
                selected is CutoutColor.Dynamic && selected.role == DynamicRole.PRIMARY -> stringResource(R.string.tooltip_dynamic_primary)
                selected is CutoutColor.Dynamic && selected.role == DynamicRole.SECONDARY -> stringResource(R.string.tooltip_dynamic_secondary)
                selected is CutoutColor.Dynamic && selected.role == DynamicRole.TERTIARY -> stringResource(R.string.tooltip_dynamic_tertiary)
                selected is CutoutColor.Solid && (selected.argb and 0xFFFFFFL == 0x000000L) -> stringResource(R.string.tooltip_oled_black)
                selected is CutoutColor.Solid && customArgb != null -> stringResource(R.string.tooltip_custom_color)
                selected is CutoutColor.Solid -> stringResource(R.string.tooltip_preset_color)
                else -> null
            }
            ColorSelectionTooltip(text = tooltipText)
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = currentColor,
            onConfirm = { picked ->
                showPicker = false
                onSelect(CutoutColor.Solid(picked.toArgb().toLong() and 0xFFFFFFFFL))
            },
            onDismiss = { showPicker = false },
        )
    }
}