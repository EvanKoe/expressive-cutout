package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.ColorSpec
import com.ekoehler.expressivecutout.data.CutoutFill
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.GradientDirection
import com.ekoehler.expressivecutout.data.RecentColorPreferences
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.overlay.resolveBrush
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Accent used by the preview event, matching the sibling settings screens. */
private val PREVIEW_ACCENT = Color(0xFF60A5FA)

/** Neutral swatches offered first in every picker, with their content descriptions. */
private val NEUTRAL_COLORS = listOf(
    0xFF0A0A0AL to R.string.cd_color_black,
    0xFF444444L to R.string.cd_color_dark_grey,
    0xFFBBBBBBL to R.string.cd_color_light_grey,
    0xFFFFFFFFL to R.string.cd_color_white,
)

/** Accent swatches shared with the other colour cards. */
private val ACCENT_COLORS = listOf(
    0xFFEF4444L, 0xFFF59E0BL, 0xFF22C55EL, 0xFF3B82F6L, 0xFF8B5CF6L, 0xFFEC4899L,
)

/**
 * Every colour offered as a preset, flattened into one list so a recently-picked colour can be
 * tested against it and not shown twice.
 */
private val PRESET_ARGBS = NEUTRAL_COLORS.map { it.first } + ACCENT_COLORS

/**
 * "Background" screen (reached from the Appearance screen). The collapsed ("normal") and expanded
 * cutout each get their own fill — a solid colour (Material You dynamic role, custom pick, or a
 * preset) or a two-colour gradient. When the two states differ the overlay cross-fades between
 * them as it expands/shrinks; a live preview at the top shows the state of the selected tab.
 */
@Composable
internal fun BackgroundScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    var previewDark by remember { mutableStateOf(systemInDark) }
    // 0 = normal (collapsed), 1 = expanded.
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val expandedTab = tabIndex == 1

    val previewLabel = stringResource(R.string.preview_label)
    val previewDetail = stringResource(R.string.preview_detail)
    val previewEvent = remember(previewLabel, previewDetail) {
        IslandEvent(
            id = 0L,
            icon = IslandIcon.Vector(Icons.Rounded.Notifications),
            label = previewLabel,
            detail = previewDetail,
            accent = PREVIEW_ACCENT,
        )
    }
    val cutout = rememberTopCutout()
    val dims = if (expandedTab) layout.expanded else layout.collapsed
    val currentFill = if (expandedTab) appearance.backgroundExpanded else appearance.backgroundNormal
    val onSelect: (CutoutFill) -> Unit = if (expandedTab) viewModel::setBackgroundExpanded else viewModel::setBackgroundNormal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.appearance_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalIconButton(onClick = { previewDark = !previewDark }) {
                Icon(
                    imageVector = if (previewDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = stringResource(R.string.cd_toggle_preview_theme),
                )
            }
        }

        IslandPreviewPanel(
            background = if (previewDark) Color(0xFF0B0B0C) else Color(0xFFEDEFF3),
            cutout = cutout,
            widthPercent = dims.widthPercent,
            heightDp = dims.heightDp,
            cornerTopLeftDp = dims.cornerTopLeftDp,
            cornerTopRightDp = dims.cornerTopRightDp,
            cornerBottomLeftDp = dims.cornerBottomLeftDp,
            cornerBottomRightDp = dims.cornerBottomRightDp,
            offsetXDp = dims.offsetXDp,
            offsetYDp = dims.offsetYDp,
            expanded = expandedTab,
            event = previewEvent,
            appearance = appearance,
        )

        // Which state is being edited.
        ExpressiveSegmentedRow(
            options = listOf(
                stringResource(R.string.tab_normal),
                stringResource(R.string.tab_expanded),
            ),
            selectedIndex = tabIndex,
            onSelect = { tabIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )

        FillPickerCard(selected = currentFill, onSelect = onSelect)
    }
}

/** The fill editor for one state: a Solid/Gradient switch and the matching controls. */
@Composable
private fun FillPickerCard(
    selected: CutoutFill,
    onSelect: (CutoutFill) -> Unit,
) {
    val isGradient = selected is CutoutFill.Gradient

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExpressiveSegmentedRow(
                options = listOf(
                    stringResource(R.string.label_solid),
                    stringResource(R.string.label_gradient),
                ),
                selectedIndex = if (isGradient) 1 else 0,
                onSelect = { index ->
                    when {
                        index == 1 && selected !is CutoutFill.Gradient -> onSelect(
                            CutoutFill.Gradient(
                                start = (selected as? CutoutFill.Solid)?.color ?: ColorSpec.Fixed(0xFF0A0A0AL),
                                end = ColorSpec.Fixed(0xFF3B82F6L),
                                direction = GradientDirection.VERTICAL,
                            ),
                        )
                        // Leaving gradient mode: keep the start colour as the solid fill.
                        index == 0 && selected is CutoutFill.Gradient ->
                            onSelect(CutoutFill.Solid(selected.start))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (selected is CutoutFill.Gradient) {
                GradientControls(gradient = selected, onSelect = onSelect)
            } else if (selected is CutoutFill.Solid) {
                ColorSpecPicker(
                    spec = selected.color,
                    onChange = { onSelect(CutoutFill.Solid(it)) },
                )
            }
        }
    }
}

/** A gradient preview strip, start/end colour pickers and a direction selector. */
@Composable
private fun GradientControls(
    gradient: CutoutFill.Gradient,
    onSelect: (CutoutFill) -> Unit,
) {
    // Live preview of the gradient
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient.resolveBrush()),
    )

    // Start colour
    Text(text = stringResource(R.string.gradient_start), style = MaterialTheme.typography.titleSmall)
    ColorSpecPicker(
        spec = gradient.start,
        onChange = { onSelect(gradient.copy(start = it)) },
    )

    // End colour
    Text(text = stringResource(R.string.gradient_end), style = MaterialTheme.typography.titleSmall)
    ColorSpecPicker(
        spec = gradient.end,
        onChange = { onSelect(gradient.copy(end = it)) },
    )

    // Direction
    Text(
        text = stringResource(R.string.gradient_direction),
        style = MaterialTheme.typography.titleSmall,
    )
    ExpressiveSegmentedRow(
        options = listOf(
            stringResource(R.string.gradient_vertical),
            stringResource(R.string.gradient_diagonal),
            stringResource(R.string.gradient_horizontal),
        ),
        selectedIndex = gradient.direction.ordinal,
        onSelect = { onSelect(gradient.copy(direction = GradientDirection.entries[it])) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Editor for a single [ColorSpec]: a swatch row (three Material You dynamic roles, a custom wheel
 * pick, then the neutral and accent presets) plus an opacity slider. Selecting a swatch keeps the
 * current opacity, so colour and transparency can be set independently.
 */
@Composable
private fun ColorSpecPicker(
    spec: ColorSpec,
    onChange: (ColorSpec) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val opacity = spec.opacity
    val fixedRgb = (spec as? ColorSpec.Fixed)?.argb?.and(0xFFFFFFL)
    val customRgb = fixedRgb?.takeIf { rgb -> PRESET_ARGBS.none { it and 0xFFFFFFL == rgb } }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recentColorPreferences = remember(context) { RecentColorPreferences(context) }
    val storedRecents by recentColorPreferences.recentColors
        .collectAsStateWithLifecycle(initialValue = emptyList())
    // Compared on RGB only, like the presets below: opacity lives on the spec, not the swatch, so a
    // recent pick and a preset of the same hue are the same swatch here.
    val recentRgbs = storedRecents
        .map { it and 0xFFFFFFL }
        .filterNot { rgb -> PRESET_ARGBS.any { it and 0xFFFFFFL == rgb } }
    // Derived from the fill itself (OLED black == a fully-black fixed colour), not held as separate
    // local state: the normal and expanded tabs share this composable slot and only swap the [spec]
    // passed in, so a remembered flag would leak the toggle across both. Deriving keeps them
    // independent — each tab reflects its own fill.
    val isOledBlack = fixedRgb == 0x000000L

    fun pickFixed(argb: Long) = onChange(ColorSpec.Fixed(argb).withOpacity(opacity))
    fun pickDynamic(role: DynamicRole) = onChange(ColorSpec.Dynamic(role, opacity))

    fun toggleOledBlack(enabled: Boolean) {
        if (enabled) pickFixed(0xFF000000) else pickDynamic(DynamicRole.PRIMARY)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggleCard(
            shape = RoundedCornerShape(size = 24.dp),
            title = stringResource(R.string.bgColor_oled_title),
            description = stringResource(R.string.bgColor_oled_desc),
            checked = isOledBlack,
            onCheckedChange = ::toggleOledBlack
        )

        AnimatedVisibility(visible = !isOledBlack) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SwatchRow {
                    // Material you coolors
                    DynamicSwatch(
                        DynamicRole.PRIMARY,
                        R.string.cd_color_dynamic_primary,
                        spec,
                        ::pickDynamic
                    )
                    DynamicSwatch(
                        DynamicRole.SECONDARY,
                        R.string.cd_color_dynamic_secondary,
                        spec,
                        ::pickDynamic
                    )
                    DynamicSwatch(
                        DynamicRole.TERTIARY,
                        R.string.cd_color_dynamic_tertiary,
                        spec,
                        ::pickDynamic
                    )

                    // Custom wheel pick.
                    CustomColorSwatch(
                        selectedColor = customRgb?.let { Color(0xFF000000L or it) },
                        onClick = { showPicker = true },
                    )

                    // The user's own recent picks, newest first.
                    recentRgbs.forEach { rgb ->
                        ColorSwatch(
                            color = Color(0xFF000000L or rgb),
                            selected = fixedRgb == rgb,
                            onClick = { pickFixed(0xFF000000L or rgb) },
                        )
                    }

                    // Neutrals then accents, matched on RGB so opacity changes don't drop the selection.
                    (NEUTRAL_COLORS.map { it.first } + ACCENT_COLORS).forEach { argb ->
                        ColorSwatch(
                            color = Color(argb),
                            selected = spec is ColorSpec.Fixed && spec.argb and 0xFFFFFFL == argb and 0xFFFFFFL,
                            onClick = { pickFixed(argb) },
                        )
                    }
                }

                AdjustableSlider(
                    label = stringResource(R.string.opacity),
                    valueText = "${(opacity * 100).roundToInt()}%",
                    value = opacity,
                    valueRange = 0f..1f,
                    step = 0.05f,
                    onValueChange = { onChange(spec.withOpacity(it)) },
                    onCommit = {},
                )
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = customRgb?.let { Color(0xFF000000L or it) } ?: Color.White,
            onConfirm = { picked ->
                showPicker = false
                val argb = picked.toArgb().toLong() and 0xFFFFFFFFL
                pickFixed(argb)
                scope.launch { recentColorPreferences.record(argb) }
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun DynamicSwatch(
    role: DynamicRole,
    descriptionRes: Int,
    spec: ColorSpec,
    onPick: (DynamicRole) -> Unit,
) {
    ColorSwatch(
        color = ColorSpec.Dynamic(role).resolve(),
        selected = spec is ColorSpec.Dynamic && spec.role == role,
        badge = Icons.Rounded.AutoAwesome,
        badgeDescription = stringResource(descriptionRes),
        onClick = { onPick(role) },
    )
}

/** A horizontally-scrolling row of colour swatches. */
@Composable
private fun SwatchRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}
