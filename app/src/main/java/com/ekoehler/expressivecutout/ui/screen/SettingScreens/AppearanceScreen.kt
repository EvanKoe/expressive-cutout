package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.DefaultPresetColors
import kotlin.math.roundToInt

/** The Material You dynamic roles [ColorPickerCard] offers by default, in display order. */
private val DefaultDynamicRoles = listOf(DynamicRole.PRIMARY, DynamicRole.SECONDARY, DynamicRole.TERTIARY)

@Composable
internal fun AppearanceScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenBackground: () -> Unit,
    onOpenActionButtons: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    var strokeWidth by remember(appearance.strokeWidthDp) { mutableStateOf(appearance.strokeWidthDp.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.appearance_shadow_title),
            description = stringResource(R.string.appearance_shadow_desc),
            checked = appearance.shadowEnabled,
            onCheckedChange = viewModel::setShadowEnabled,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.appearance_stroke_title),
            description = stringResource(R.string.appearance_stroke_desc),
            checked = appearance.strokeEnabled,
            onCheckedChange = viewModel::setStrokeEnabled,
        )

        AnimatedVisibility(visible = appearance.strokeEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    AdjustableSlider(
                        label = stringResource(R.string.appearance_stroke_width),
                        valueText = "${strokeWidth.roundToInt()} dp",
                        value = strokeWidth,
                        valueRange = AppearanceSettings.MIN_STROKE_WIDTH_DP.toFloat()..
                            AppearanceSettings.MAX_STROKE_WIDTH_DP.toFloat(),
                        step = 1f,
                        onValueChange = { strokeWidth = it },
                        onCommit = { viewModel.setStrokeWidth(strokeWidth.roundToInt()) },
                    )
                }
            }
            ColorPickerCard(
                label = stringResource(R.string.appearance_stroke_color),
                selected = appearance.strokeColor,
                onSelect = { it?.let(viewModel::setStrokeColor) },
            )
        }

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.appearance_show_app_name_title),
            description = stringResource(R.string.appearance_show_app_name_desc),
            checked = appearance.showSourceAppName,
            onCheckedChange = viewModel::setShowSourceAppName,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.appearance_show_timestamp_title),
            description = stringResource(R.string.appearance_show_timestamp_desc),
            checked = appearance.showTimestamp,
            onCheckedChange = viewModel::setShowTimestamp,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.appearance_prefer_dynamic_icon_color_title),
            description = stringResource(R.string.appearance_prefer_dynamic_icon_color_desc),
            checked = appearance.preferDynamicIconColor,
            onCheckedChange = viewModel::setPreferDynamicIconColor,
        )

        // Opens the dedicated screen for the collapsed/expanded background fills (solid colours
        // and gradients, one per state).
        BackgroundCard(onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenBackground()
        })

        // Opens the dedicated screen for the expanded cutout's action chips and reply field
        // (including the send/cancel reply-button colours).
        ActionButtonsCard(onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenActionButtons()
        })
    }
}

/** A clickable card that navigates to the dedicated background-fill screen. */
@Composable
private fun BackgroundCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.FormatColorFill,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.appearance_background_color),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_background_subtitle),
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
    }
}

/** A clickable card that navigates to the dedicated action-buttons screen. */
@Composable
private fun ActionButtonsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartButton,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.action_buttons_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_action_buttons_subtitle),
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
    }
}

/**
 * The single, shared colour-selection card used by every screen that edits a [CutoutColor]. It
 * offers, in order: an optional "default" swatch (a null selection, for settings whose default
 * follows another colour, e.g. the reply buttons), several Material You dynamic-role swatches, a
 * custom pick (opens [ColorPickerDialog] with a hex field), and a row of predefined swatches.
 *
 * The predefined colours default to [DefaultPresetColors] (black, white, dark/light grey, blue,
 * red, green) but any screen can pass its own [presetColors]; likewise the dynamic roles shown can
 * be overridden via [dynamicRoles].
 */
@Composable
internal fun ColorPickerCard(
    label: String,
    selected: CutoutColor?,
    onSelect: (CutoutColor?) -> Unit,
    defaultLabel: String? = null,
    defaultColor: Color? = null,
    presetColors: List<Long> = DefaultPresetColors,
    dynamicRoles: List<DynamicRole> = DefaultDynamicRoles,
    roundedCorners: Dp = 24.dp
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
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    // Breathing room so the selected swatch's enlarged ring isn't clipped at the edges.
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Optional "use the default" swatch (null selection), then the Material You dynamic
                // roles, then the custom picker, then the predefined swatches.
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
                onSelect(CutoutColor.Solid(picked.toArgb().toLong() and 0xFFFFFFFFL))
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** The Material You scheme role's human-readable label, for a swatch's content description. */
@Composable
fun DynamicRole.dynamicDescription(): String = stringResource(
    when (this) {
        DynamicRole.PRIMARY -> R.string.cd_color_dynamic_primary
        DynamicRole.SECONDARY -> R.string.cd_color_dynamic_secondary
        DynamicRole.TERTIARY -> R.string.cd_color_dynamic_tertiary
    },
)

@Composable
internal fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    badge: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badgeDescription: String? = null,
) {
    val ring = MaterialTheme.colorScheme.primary
    // A faint border keeps near-white swatches visible against the card.
    val edge = MaterialTheme.colorScheme.outlineVariant
    // Expressive: the selected swatch springs up a touch and its ring thickens.
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "swatchScale",
    )
    val ringWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchRing",
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(
                width = ringWidth,
                color = if (selected) ring else edge,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Contrast the marks against the swatch itself.
        val markColor = if (color.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White
        if (badge != null) {
            Icon(
                imageVector = badge,
                contentDescription = badgeDescription,
                tint = markColor,
                modifier = Modifier.size(20.dp),
            )
        } else if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = markColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The "pick any colour" swatch. Shows a rainbow when no custom colour is active, or the chosen
 * colour (with the selection ring) once one is set. Tapping it opens [ColorPickerDialog].
 */
@Composable
internal fun CustomColorSwatch(
    selectedColor: Color?,
    onClick: () -> Unit,
) {
    val ring = MaterialTheme.colorScheme.primary
    val edge = MaterialTheme.colorScheme.outlineVariant
    val rainbow = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFFACC15),
                Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF8B5CF6),
                Color(0xFFEC4899), Color(0xFFEF4444),
            ),
        )
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (selectedColor != null) Modifier.background(selectedColor)
                else Modifier.background(rainbow),
            )
            .border(
                width = if (selectedColor != null) 3.dp else 1.dp,
                color = if (selectedColor != null) ring else edge,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val markColor = if ((selectedColor ?: Color.White).luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White
        Icon(
            imageVector = Icons.Rounded.Colorize,
            contentDescription = stringResource(R.string.cd_color_custom),
            tint = markColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** An HSV colour picker (saturation/value field + hue bar) with a two-way hex field. */
@Composable
internal fun ColorPickerDialog(
    initial: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    // HSV is the source of truth for the visual picker; the hex field mirrors it.
    val initialHsv = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(Color.hsv(initialHsv[0], initialHsv[1], initialHsv[2]).toHexRgb()) }

    val color = Color.hsv(hue, saturation, value)
    // Called after the visual picker moves, to keep the hex field showing the current colour.
    fun syncHex() { hexText = Color.hsv(hue, saturation, value).toHexRgb() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(R.string.action_select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.color_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SaturationValueField(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { s, v -> saturation = s; value = v; syncHex() },
                )
                HueBar(hue = hue, onChange = { hue = it; syncHex() })
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val cleaned = input.removePrefix("#").take(6).uppercase()
                        hexText = cleaned
                        parseHexColor(cleaned)?.let { parsed ->
                            val out = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed.toArgb(), out)
                            hue = out[0]
                            saturation = out[1]
                            value = out[2]
                        }
                    },
                    label = { Text(stringResource(R.string.color_picker_hex)) },
                    singleLine = true,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

/** Saturation (x) / value (y) selection field, tinted to the current [hue]. */
@Composable
private fun SaturationValueField(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val hueColor = Color.hsv(hue, 1f, 1f)
    // Expressive spring: the handle bounces up when grabbed and settles back when released.
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1.5f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "svThumbScale",
    )

    fun report(x: Float, y: Float) {
        if (boxSize.width == 0 || boxSize.height == 0) return
        onChange(
            (x / boxSize.width).coerceIn(0f, 1f),
            1f - (y / boxSize.height).coerceIn(0f, 1f),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .onSizeChanged { boxSize = it }
            .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        dragging = true
                        report(it.x, it.y)
                        tryAwaitRelease()
                        dragging = false
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    change.consume()
                    report(change.position.x, change.position.y)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val r = 11.dp.toPx()
                    IntOffset(
                        (saturation * boxSize.width - r).roundToInt(),
                        ((1f - value) * boxSize.height - r).roundToInt(),
                    )
                }
                .size(22.dp)
                .scale(thumbScale)
                .clip(CircleShape)
                .background(Color.hsv(hue, saturation, value))
                .border(3.dp, Color.White, CircleShape)
                .border(4.dp, Color.Black.copy(alpha = 0.25f), CircleShape),
        )
    }
}

/** Horizontal hue selector (0–360°). */
@Composable
private fun HueBar(
    hue: Float,
    onChange: (Float) -> Unit,
) {
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val hueColors = remember {
        (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
    }
    // Material 3 Expressive slider thumb: a tall pill that narrows and grows taller when grabbed.
    val thumbWidth by animateDpAsState(
        targetValue = if (dragging) 6.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "hueThumbWidth",
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (dragging) 48.dp else 40.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "hueThumbHeight",
    )

    fun report(x: Float) {
        if (barSize.width == 0) return
        onChange((x / barSize.width).coerceIn(0f, 1f) * 360f)
    }

    // Outer box is unclipped so the tall thumb can overflow the track above and below.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChanged { barSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        dragging = true
                        report(it.x)
                        tryAwaitRelease()
                        dragging = false
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    change.consume()
                    report(change.position.x)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(hueColors)),
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(((hue / 360f) * barSize.width - thumbWidth.toPx() / 2f).roundToInt(), 0)
                }
                .width(thumbWidth)
                .height(thumbHeight)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
        )
    }
}

/** "RRGGBB" (no alpha), upper-case. */
private fun Color.toHexRgb(): String = "%06X".format(toArgb() and 0xFFFFFF)

/** Parse a 6-digit "RRGGBB" hex string (with or without a leading '#') to an opaque colour. */
private fun parseHexColor(input: String): Color? {
    val hex = input.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it.digitToIntOrNull(16) == null }) return null
    return Color(0xFF000000L or hex.toLong(16))
}
