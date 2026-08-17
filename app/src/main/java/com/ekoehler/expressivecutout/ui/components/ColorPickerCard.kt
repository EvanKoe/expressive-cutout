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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.RecentColorPreferences
import com.ekoehler.expressivecutout.overlay.resolve
import kotlinx.coroutines.launch
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

@Composable
fun ColorPickerCard (
    label: String? = null,
    selected: CutoutColor?,
    onSelect: (CutoutColor?) -> Unit,
    defaultLabel: String? = null,
    defaultColor: Color? = null,
    presetColors: List<Long> = DefaultPresetColors,
    dynamicRoles: List<DynamicRole> = DefaultDynamicRoles,
    roundedCorners: Dp = 24.dp,
    proposeOledBlack: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    // A Solid colour that isn't one of the presets is the user's own custom pick.
    val customArgb = (selected as? CutoutColor.Solid)?.argb
        ?.takeIf { argb -> presetColors.none { it == argb } }
    // Seed the picker with whatever colour is active right now.
    val currentColor = selected?.resolve() ?: defaultColor ?: Color.White

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recentColorPreferences = remember(context) { RecentColorPreferences(context) }
    val storedRecents by recentColorPreferences.recentColors
        .collectAsStateWithLifecycle(initialValue = emptyList())
    // A recent pick that this screen also lists as a preset would otherwise show up twice.
    val recentColors = storedRecents.filterNot { argb -> presetColors.any { it == argb } }

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
                    // Breathing room so the selected swatch's enlarged ring isn't clipped at the edges.
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Optional "use the default" swatch (null selection), then the Material You dynamic
                // roles, then the custom picker, then the user's recent picks, then the predefined
                // swatches.
                if (defaultLabel != null) {
                    ColorSwatch(
                        color = defaultColor ?: MaterialTheme.colorScheme.primary,
                        selected = selected == null,
                        badge = Icons.Rounded.RestartAlt,
                        badgeDescription = defaultLabel,
                        onClick = { onSelect(null) },
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
                recentColors.forEach { argb ->
                    ColorSwatch(
                        color = Color(argb),
                        selected = selected == CutoutColor.Solid(argb),
                        onClick = { onSelect(CutoutColor.Solid(argb)) },
                    )
                }
                presetColors.forEach { argb ->
                    ColorSwatch(
                        color = Color(argb),
                        selected = selected == CutoutColor.Solid(argb),
                        onClick = { onSelect(CutoutColor.Solid(argb)) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = currentColor,
            onConfirm = { picked ->
                showPicker = false
                val argb = picked.toArgb().toLong() and 0xFFFFFFFFL
                onSelect(CutoutColor.Solid(argb))
                scope.launch { recentColorPreferences.record(argb) }
            },
            onDismiss = { showPicker = false },
        )
    }
}