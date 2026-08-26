package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.data.AppColorFallback
import com.ekoehler.expressivecutout.data.ColorSpec
import com.ekoehler.expressivecutout.data.CutoutFill
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.GradientDirection
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.overlay.resolveBaseColor
import com.ekoehler.expressivecutout.overlay.resolveBrush
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.AppColorFallbackRow
import com.ekoehler.expressivecutout.ui.components.ColorSelectionTooltip
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlin.math.roundToInt

/** Accent used by preview swatches and fallback defaults. */
private val PreviewAccent = Color(0xFF60A5FA)
private const val OledBlackArgb = 0xFF000000L
private const val DefaultGradientBlueArgb = 0xFF3B82F6L

/** Neutral swatches offered first in every picker, with their content descriptions. */
private val NeutralColors = listOf(
    0xFF0A0A0AL to R.string.cd_color_black,
    0xFF444444L to R.string.cd_color_dark_grey,
    0xFFBBBBBBL to R.string.cd_color_light_grey,
    0xFFFFFFFFL to R.string.cd_color_white,
)

/** Accent swatches shared with the other colour cards. */
private val AccentColors = listOf(
    0xFFEF4444L, 0xFFF59E0BL, 0xFF22C55EL, 0xFF3B82F6L, 0xFF8B5CF6L, 0xFFEC4899L,
)

private val PresetArgbs = NeutralColors.map { it.first } + AccentColors

/**
 * "Background" screen (reached from the Appearance screen). The collapsed ("normal") and expanded
 * cutout each get their own fill — a solid colour or a two-colour gradient.
 */
@Composable
internal fun BackgroundScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    // 0 = normal (collapsed), 1 = expanded.
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val expandedTab = tabIndex == 1

    val currentFill = if (expandedTab) appearance.backgroundExpanded else appearance.backgroundNormal
    val onSelect: (CutoutFill) -> Unit = if (expandedTab) viewModel::setBackgroundExpanded else viewModel::setBackgroundNormal

    // Mirror which tab is being edited (collapsed vs expanded) in the pinned live preview.
    LaunchedEffect(tabIndex) { IslandPreviewBus.setExpandedPreview(tabIndex == 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
    val modeIndex = when (selected) {
        is CutoutFill.Solid -> 0
        is CutoutFill.Gradient -> 1
    }

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
                selectedIndex = modeIndex,
                onSelect = { index ->
                    when (index) {
                        0 -> {
                            val color = when (selected) {
                                is CutoutFill.Solid -> selected.color
                                is CutoutFill.Gradient -> selected.start
                            }
                            onSelect(CutoutFill.Solid(color))
                        }
                        1 -> {
                            val start = when (selected) {
                                is CutoutFill.Solid -> selected.color
                                is CutoutFill.Gradient -> selected.start
                            }
                            onSelect(
                                CutoutFill.Gradient(
                                    start = start,
                                    end = ColorSpec.Fixed(DefaultGradientBlueArgb),
                                    direction = GradientDirection.VERTICAL,
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            when (selected) {
                is CutoutFill.Gradient -> {
                    GradientControls(gradient = selected, onSelect = onSelect)
                }
                is CutoutFill.Solid -> {
                    ColorSpecPicker(
                        spec = selected.color,
                        onChange = { onSelect(CutoutFill.Solid(it)) },
                        allowAppIcon = true,
                    )
                }
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
    val baseColor = gradient.resolveBaseColor(PreviewAccent, PreviewAccent)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(baseColor)
            .background(gradient.resolveBrush(PreviewAccent, PreviewAccent)),
    )

    Text(text = stringResource(R.string.gradient_start), style = MaterialTheme.typography.titleSmall)
    ColorSpecPicker(
        spec = gradient.start,
        onChange = { onSelect(gradient.copy(start = it)) },
        allowAppIcon = true,
    )
    Text(text = stringResource(R.string.gradient_end), style = MaterialTheme.typography.titleSmall)
    ColorSpecPicker(
        spec = gradient.end,
        onChange = { onSelect(gradient.copy(end = it)) },
        allowAppIcon = true,
    )

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

    AdjustableSlider(
        label = stringResource(R.string.opacity),
        valueText = "${(gradient.opacity * 100).roundToInt()}%",
        value = gradient.opacity,
        valueRange = 0f..1f,
        step = 0.05f,
        onValueChange = { onSelect(gradient.copy(opacity = it)) },
        onCommit = {},
    )
}

/**
 * Editor for a single [ColorSpec]: a swatch row (App Icon, dynamic roles, custom wheel, presets)
 * plus an opacity slider and optional fallback selector.
 */
@Composable
private fun ColorSpecPicker(
    spec: ColorSpec,
    onChange: (ColorSpec) -> Unit,
    allowAppIcon: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val opacity = spec.opacity
    val fixedRgb = (spec as? ColorSpec.Fixed)?.argb?.and(0xFFFFFFL)
    val customRgb = fixedRgb?.takeIf { rgb -> PresetArgbs.none { it and 0xFFFFFFL == rgb } }
    val isOledBlack = fixedRgb == 0x000000L

    fun pickFixed(argb: Long) = onChange(ColorSpec.Fixed(argb).withOpacity(opacity))
    fun pickDynamic(role: DynamicRole) = onChange(ColorSpec.Dynamic(role, opacity))

    fun toggleOledBlack(enabled: Boolean) {
        if (enabled) pickFixed(OledBlackArgb) else pickDynamic(DynamicRole.PRIMARY)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggleCard(
            shape = RoundedCornerShape(size = 24.dp),
            title = stringResource(R.string.bgColor_oled_title),
            description = stringResource(R.string.bgColor_oled_desc),
            checked = isOledBlack,
            onCheckedChange = ::toggleOledBlack,
        )

        AnimatedVisibility(visible = !isOledBlack) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SwatchRow {
                    if (allowAppIcon) {
                        ColorSwatch(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            selected = spec is ColorSpec.AppIcon,
                            badgePainter = painterResource(R.drawable.ic_play_store),
                            badgeDescription = stringResource(R.string.cd_color_app_icon),
                            onClick = {
                                val currentFallback = (spec as? ColorSpec.AppIcon)?.fallback ?: AppColorFallback.ADAPTIVE
                                onChange(ColorSpec.AppIcon(currentFallback, alpha = opacity))
                            },
                        )
                    }

                    DynamicSwatch(
                        DynamicRole.PRIMARY,
                        R.string.cd_color_dynamic_primary,
                        spec,
                        ::pickDynamic,
                    )
                    DynamicSwatch(
                        DynamicRole.SECONDARY,
                        R.string.cd_color_dynamic_secondary,
                        spec,
                        ::pickDynamic,
                    )
                    DynamicSwatch(
                        DynamicRole.TERTIARY,
                        R.string.cd_color_dynamic_tertiary,
                        spec,
                        ::pickDynamic,
                    )

                    CustomColorSwatch(
                        selectedColor = customRgb?.let { Color(0xFF000000L or it) },
                        onClick = { showPicker = true },
                    )

                    (NeutralColors.map { it.first } + AccentColors).forEach { argb ->
                        ColorSwatch(
                            color = Color(argb),
                            selected = spec is ColorSpec.Fixed && spec.argb and 0xFFFFFFL == argb and 0xFFFFFFL,
                            onClick = { pickFixed(argb) },
                        )
                    }
                }

                AnimatedVisibility(visible = allowAppIcon && spec is ColorSpec.AppIcon) {
                    val fallback = (spec as? ColorSpec.AppIcon)?.fallback ?: AppColorFallback.ADAPTIVE
                    AppColorFallbackRow(
                        fallback = fallback,
                        onSelect = { onChange(ColorSpec.AppIcon(it, alpha = opacity)) },
                    )
                }

                val tooltipText = when {
                    spec is ColorSpec.AppIcon -> stringResource(R.string.tooltip_app_icon)
                    spec is ColorSpec.Dynamic && spec.role == DynamicRole.PRIMARY -> stringResource(R.string.tooltip_dynamic_primary)
                    spec is ColorSpec.Dynamic && spec.role == DynamicRole.SECONDARY -> stringResource(R.string.tooltip_dynamic_secondary)
                    spec is ColorSpec.Dynamic && spec.role == DynamicRole.TERTIARY -> stringResource(R.string.tooltip_dynamic_tertiary)
                    spec is ColorSpec.Fixed && (spec.argb and 0xFFFFFFL == 0x000000L) -> stringResource(R.string.tooltip_oled_black)
                    spec is ColorSpec.Fixed && customRgb != null -> stringResource(R.string.tooltip_custom_color)
                    spec is ColorSpec.Fixed -> stringResource(R.string.tooltip_preset_color)
                    else -> null
                }
                ColorSelectionTooltip(text = tooltipText)

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
                pickFixed(picked.toArgb().toLong() and 0xFFFFFFFFL)
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
