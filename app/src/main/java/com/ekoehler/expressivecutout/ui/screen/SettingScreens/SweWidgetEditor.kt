package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.BatteryPercentage
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DEFAULT_CAMERA_RADIUS_DP
import com.ekoehler.expressivecutout.data.EmptyPillInfoSettings
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.SweWidget
import com.ekoehler.expressivecutout.data.UnreadShape
import com.ekoehler.expressivecutout.data.asEmptyInfoCutout
import com.ekoehler.expressivecutout.events.BatteryStatus
import com.ekoehler.expressivecutout.overlay.EmptyPillInfoContent
import com.ekoehler.expressivecutout.overlay.emptyPillSideWidthsDp
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.service.UnreadCounts
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MAX_WIDTH
import com.ekoehler.expressivecutout.ui.components.ROBOTO_FLEX_MIN_WIDTH
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.components.rememberRobotoFlexFamily
import kotlin.math.roundToInt

/** Gap between the cards of one group, so each group reads as a single block. */
private val GroupGap = 4.dp

/** The unread count the preview shows, so a badge is always visible while editing. */
private val PreviewCounts = UnreadCounts(all = 3, nonSilent = 3)

/** The battery reading the preview shows. */
private val PreviewBattery = BatteryStatus(level = 82, charging = false)

/**
 * The resting pill's widget editor: a live preview of the pill, a warning when it would run off the
 * screen, then one block per side listing its widgets in order with move / edit / remove controls
 * and an "Add widget" button. Adding opens a type picker; editing opens the widget's own settings
 * in a bottom sheet. Stateless apart from which sheet is open — every change goes out through the
 * lambdas and comes back in [settings].
 */
@Composable
internal fun SweWidgetEditor(
    settings: EmptyPillInfoSettings,
    collapsed: IslandDimensions,
    onAdd: (InfoSide, SweWidget) -> Unit,
    onUpdate: (SweWidget) -> Unit,
    onMove: (id: String, delta: Int) -> Unit,
    onSetSide: (id: String, side: InfoSide) -> Unit,
    onRemove: (id: String) -> Unit,
) {
    var addingTo by remember { mutableStateOf<InfoSide?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preview and overflow warning
        SwePreviewCard(settings = settings, collapsed = collapsed)

        // Left side
        SweSideSection(
            title = stringResource(R.string.swe_side_left_title),
            widgets = settings.left,
            onMove = onMove,
            onEdit = { editingId = it },
            onRemove = onRemove,
            onAdd = { addingTo = InfoSide.LEFT },
        )

        // Right side
        SweSideSection(
            title = stringResource(R.string.swe_side_right_title),
            widgets = settings.right,
            onMove = onMove,
            onEdit = { editingId = it },
            onRemove = onRemove,
            onAdd = { addingTo = InfoSide.RIGHT },
        )
    }

    addingTo?.let { side ->
        AddWidgetSheet(
            onPick = { widget ->
                addingTo = null
                onAdd(side, widget)
                editingId = widget.id
            },
            onDismiss = { addingTo = null },
        )
    }

    editingId?.let { id ->
        // Read back from the stored lists so every change made in the sheet shows up in it at once; a
        // widget just added may not be stored yet, so the sheet simply waits for it to appear
        val side = settings.sideOf(id)
        val widget = side?.let { settings.on(it).firstOrNull { w -> w.id == id } }
        if (widget != null) {
            SweWidgetSheet(
                widget = widget,
                side = side,
                onSideChange = { onSetSide(id, it) },
                onChange = onUpdate,
                onDismiss = { editingId = null },
            )
        }
    }
}

/**
 * A black pill drawn with the overlay's own widget code and sample data, centred on a camera dot,
 * plus a warning card when the computed pill is wider than the screen.
 */
@Composable
private fun SwePreviewCard(settings: EmptyPillInfoSettings, collapsed: IslandDimensions) {
    val displayWidthDp = LocalConfiguration.current.screenWidthDp
    val density = LocalDensity.current.density
    val dims = remember(settings, collapsed, displayWidthDp, density) {
        val (left, right) = emptyPillSideWidthsDp(settings, collapsed.heightDp, density, PreviewCounts, hasBattery = true)
        collapsed.asEmptyInfoCutout(displayWidthDp, left, right, DEFAULT_CAMERA_RADIUS_DP)
    }
    val pillWidthDp = displayWidthDp * dims.widthPercent / 100f
    val overflows = pillWidthDp > displayWidthDp

    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Pill preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = groupedShape(isFirst = true, isLast = !overflows),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((collapsed.heightDp + PREVIEW_PADDING_DP * 2).dp),
                contentAlignment = Alignment.Center,
            ) {
                // The overlay pill is always black with light ink on the real screen, so the preview
                // copies those fixed colours rather than the app theme
                Box(
                    modifier = Modifier
                        .offset(x = dims.offsetXDp.dp)
                        .requiredWidth(pillWidthDp.dp)
                        .height(collapsed.heightDp.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black),
                ) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        EmptyPillInfoContent(
                            settings = settings,
                            heightDp = collapsed.heightDp,
                            counts = PreviewCounts,
                            battery = PreviewBattery,
                            enterDelayMs = 0L,
                        )
                    }
                }

                // Camera hole
                Box(
                    modifier = Modifier
                        .size((DEFAULT_CAMERA_RADIUS_DP * CAMERA_DOT_SCALE).dp)
                        .clip(CircleShape)
                        .background(Color(PREVIEW_CAMERA_COLOR)),
                )
            }
        }

        // Overflow warning
        AnimatedVisibility(visible = overflows) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(isLast = true),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null)
                    Text(
                        text = stringResource(R.string.swe_overflow_warning),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** One side's block: its title, one row per widget in order, and the add button. */
@Composable
private fun SweSideSection(
    title: String,
    widgets: List<SweWidget>,
    onMove: (String, Int) -> Unit,
    onEdit: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = groupedShape(isFirst = true),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        // Widgets, or a hint when there are none
        if (widgets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Text(
                    text = stringResource(R.string.swe_side_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
        widgets.forEachIndexed { index, widget ->
            SweWidgetRow(
                widget = widget,
                canMoveUp = index > 0,
                canMoveDown = index < widgets.lastIndex,
                onMoveUp = { onMove(widget.id, -1) },
                onMoveDown = { onMove(widget.id, 1) },
                onEdit = { onEdit(widget.id) },
                onRemove = { onRemove(widget.id) },
            )
        }

        // Add button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = groupedShape(isLast = true),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            FilledTonalButton(onClick = onAdd, modifier = Modifier.padding(12.dp)) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.swe_add_widget),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** One widget in a side's list: its icon, name and a one-line summary, then reorder / edit / remove. */
@Composable
private fun SweWidgetRow(
    widget: SweWidget,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = groupedShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon, name and summary
            WidgetBadge(icon = widget.icon())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(text = widget.label(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = widget.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Controls
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.center_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.center_move_down))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.swe_edit_widget))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.center_remove))
            }
        }
    }
}

/** A round tonal badge holding a widget type's glyph. */
@Composable
private fun WidgetBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A bottom sheet listing the widget types; picking one hands back a new widget with default settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWidgetSheet(onPick: (SweWidget) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.swe_add_widget),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            listOf(SweWidget.Clock(), SweWidget.Unread(), SweWidget.Battery()).forEach { widget ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(widget) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetBadge(icon = widget.icon())
                    Text(
                        text = widget.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
            }
        }
    }
}

/**
 * The settings of one widget in a bottom sheet: which side it sits on, then its own controls. Each
 * change is sent as a whole new widget through [onChange].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SweWidgetSheet(
    widget: SweWidget,
    side: InfoSide,
    onSideChange: (InfoSide) -> Unit,
    onChange: (SweWidget) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Text(
                text = widget.label(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Side
            InfoSegmentCard(
                title = stringResource(R.string.empty_info_side),
                options = listOf(
                    stringResource(R.string.empty_info_side_left),
                    stringResource(R.string.empty_info_side_right),
                ),
                selectedIndex = side.ordinal,
                onSelect = { onSideChange(InfoSide.entries[it]) },
                shape = groupedShape(isFirst = true, isLast = true),
            )

            // The widget's own settings
            when (widget) {
                is SweWidget.Clock -> ClockControls(widget, onChange)
                is SweWidget.Unread -> UnreadControls(widget, onChange)
                is SweWidget.Battery -> BatteryControls(widget, onChange)
            }
        }
    }
}

/** A clock's colour, then its width, weight and size sliders under a centred live preview. */
@Composable
private fun ClockControls(clock: SweWidget.Clock, onChange: (SweWidget) -> Unit) {
    var width by remember(clock.width) { mutableFloatStateOf(clock.width) }
    var weight by remember(clock.weight) { mutableFloatStateOf(clock.weight.toFloat()) }
    var size by remember(clock.sizePercent) { mutableFloatStateOf(clock.sizePercent.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Text colour
        ColorPickerCard(
            label = stringResource(R.string.empty_info_clock_color),
            selected = clock.color,
            onSelect = { onChange(clock.copy(color = it)) },
            defaultLabel = stringResource(R.string.label_default),
            defaultColor = MaterialTheme.colorScheme.onSurface,
            shape = groupedShape(isFirst = true),
        )

        // Width, weight, size and live preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = groupedShape(isLast = true),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "12:34",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize * (size / PERCENT),
                    fontFamily = rememberRobotoFlexFamily(width, weight.roundToInt()),
                    fontWeight = FontWeight(weight.roundToInt()),
                    color = clock.color?.resolve() ?: MaterialTheme.colorScheme.onSurface,
                )
                AdjustableSlider(
                    label = stringResource(R.string.empty_info_clock_width),
                    valueText = "${width.roundToInt()}",
                    value = width,
                    valueRange = ROBOTO_FLEX_MIN_WIDTH..ROBOTO_FLEX_MAX_WIDTH,
                    step = 1f,
                    onValueChange = { width = it },
                    onCommit = { onChange(clock.copy(width = width)) },
                )
                AdjustableSlider(
                    label = stringResource(R.string.empty_info_clock_weight),
                    valueText = "${weight.roundToInt()}",
                    value = weight,
                    valueRange = SweWidget.MIN_CLOCK_WEIGHT.toFloat()..SweWidget.MAX_CLOCK_WEIGHT.toFloat(),
                    step = 100f,
                    onValueChange = { weight = it },
                    onCommit = { onChange(clock.copy(weight = weight.roundToInt())) },
                )
                AdjustableSlider(
                    label = stringResource(R.string.empty_info_clock_size),
                    valueText = "${size.roundToInt()}%",
                    value = size,
                    valueRange = SweWidget.MIN_CLOCK_SIZE_PERCENT.toFloat()..SweWidget.MAX_CLOCK_SIZE_PERCENT.toFloat(),
                    step = 5f,
                    onValueChange = { size = it },
                    onCommit = { onChange(clock.copy(sizePercent = size.roundToInt())) },
                )
            }
        }
    }
}

/** An unread badge's shape, its colour and pill width when they apply, the number colour and silent counting. */
@Composable
private fun UnreadControls(unread: SweWidget.Unread, onChange: (SweWidget) -> Unit) {
    var pillWidth by remember(unread.pillWidthPercent) { mutableFloatStateOf(unread.pillWidthPercent.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Badge shape
        InfoSegmentCard(
            title = stringResource(R.string.empty_info_unread_shape),
            options = listOf(
                stringResource(R.string.empty_info_unread_shape_star),
                stringResource(R.string.empty_info_unread_shape_pill),
                stringResource(R.string.empty_info_unread_shape_none),
            ),
            selectedIndex = unread.shape.ordinal,
            onSelect = { onChange(unread.copy(shape = UnreadShape.entries[it])) },
            shape = groupedShape(isFirst = true),
        )

        // Badge colour, only when there is a badge
        AnimatedVisibility(visible = unread.shape != UnreadShape.NONE) {
            ColorPickerCard(
                label = stringResource(R.string.empty_info_unread_shape_color),
                selected = unread.shapeColor,
                onSelect = { onChange(unread.copy(shapeColor = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.primary,
                shape = groupedShape(),
            )
        }

        // Pill width, only for the pill shape
        AnimatedVisibility(visible = unread.shape == UnreadShape.PILL) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    AdjustableSlider(
                        label = stringResource(R.string.empty_info_unread_pill_width),
                        valueText = "${pillWidth.roundToInt()}%",
                        value = pillWidth,
                        valueRange = SweWidget.MIN_PILL_WIDTH_PERCENT.toFloat()..SweWidget.MAX_PILL_WIDTH_PERCENT.toFloat(),
                        step = 10f,
                        onValueChange = { pillWidth = it },
                        onCommit = { onChange(unread.copy(pillWidthPercent = pillWidth.roundToInt())) },
                    )
                }
            }
        }

        // Number colour
        ColorPickerCard(
            label = stringResource(R.string.empty_info_unread_text_color),
            selected = unread.textColor,
            onSelect = { onChange(unread.copy(textColor = it)) },
            defaultLabel = stringResource(R.string.label_default),
            defaultColor = MaterialTheme.colorScheme.onSurface,
            shape = groupedShape(),
        )

        // Silent notifications
        SettingsToggleCard(
            shape = groupedShape(isLast = true),
            title = stringResource(R.string.empty_info_unread_silent),
            description = stringResource(R.string.empty_info_unread_silent_desc),
            checked = unread.countSilent,
            onCheckedChange = { onChange(unread.copy(countSilent = it)) },
        )
    }
}

/** A battery's colour, where its percentage goes, and the percentage's side, sign and colour when shown. */
@Composable
private fun BatteryControls(battery: SweWidget.Battery, onChange: (SweWidget) -> Unit) {
    val showsPercentage = battery.percentage != BatteryPercentage.NONE

    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Battery colour
        ColorPickerCard(
            label = stringResource(R.string.empty_info_battery_color),
            selected = battery.color,
            onSelect = { onChange(battery.copy(color = it)) },
            defaultLabel = stringResource(R.string.label_default),
            defaultColor = MaterialTheme.colorScheme.onSurface,
            shape = groupedShape(isFirst = true),
        )

        // Percentage placement
        InfoSegmentCard(
            title = stringResource(R.string.empty_info_battery_pct),
            options = listOf(
                stringResource(R.string.empty_info_battery_pct_next_to),
                stringResource(R.string.empty_info_battery_pct_inside),
                stringResource(R.string.empty_info_battery_pct_none),
            ),
            selectedIndex = battery.percentage.ordinal,
            onSelect = { onChange(battery.copy(percentage = BatteryPercentage.entries[it])) },
            shape = groupedShape(isLast = !showsPercentage),
        )

        // Percentage side, only when it sits beside the icon
        AnimatedVisibility(visible = battery.percentage == BatteryPercentage.NEXT_TO) {
            InfoSegmentCard(
                title = stringResource(R.string.empty_info_battery_pct_side),
                options = listOf(
                    stringResource(R.string.empty_info_side_left),
                    stringResource(R.string.empty_info_side_right),
                ),
                selectedIndex = battery.percentageSide.ordinal,
                onSelect = { onChange(battery.copy(percentageSide = InfoSide.entries[it])) },
                shape = groupedShape(),
            )
        }

        // Percent sign and colour, only when a percentage is drawn
        AnimatedVisibility(visible = showsPercentage) {
            Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                SettingsToggleCard(
                    shape = groupedShape(),
                    title = stringResource(R.string.empty_info_battery_pct_sign),
                    description = stringResource(R.string.empty_info_battery_pct_sign_desc),
                    checked = battery.percentSign,
                    onCheckedChange = { onChange(battery.copy(percentSign = it)) },
                )
                ColorPickerCard(
                    label = stringResource(R.string.empty_info_battery_pct_color),
                    selected = battery.percentageColor,
                    onSelect = { onChange(battery.copy(percentageColor = it)) },
                    defaultLabel = stringResource(R.string.label_default),
                    defaultColor = MaterialTheme.colorScheme.onSurface,
                    shape = groupedShape(isLast = true),
                )
            }
        }
    }
}

/** A titled segmented chooser on its own card, drawn in [shape] so it can sit inside a group. */
@Composable
private fun InfoSegmentCard(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    shape: Shape,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Title
            Text(text = title, style = MaterialTheme.typography.titleMedium)

            // Segments
            ExpressiveSegmentedRow(options = options, selectedIndex = selectedIndex, onSelect = onSelect)
        }
    }
}

/** The glyph shown for a widget type in the list and the add sheet. */
private fun SweWidget.icon(): ImageVector = when (this) {
    is SweWidget.Clock -> Icons.Rounded.Schedule
    is SweWidget.Unread -> Icons.Rounded.Notifications
    is SweWidget.Battery -> Icons.Rounded.BatteryFull
}

/** A widget type's display name. */
@Composable
private fun SweWidget.label(): String = stringResource(
    when (this) {
        is SweWidget.Clock -> R.string.empty_info_clock_title
        is SweWidget.Unread -> R.string.empty_info_unread_title
        is SweWidget.Battery -> R.string.empty_info_battery_title
    },
)

/** A one-line reminder of how a widget is set up, shown under its name in the list. */
@Composable
private fun SweWidget.summary(): String = when (this) {
    is SweWidget.Clock -> "${weight} · ${sizePercent}%"
    is SweWidget.Unread -> listOfNotNull(
        stringResource(
            when (shape) {
                UnreadShape.STAR -> R.string.empty_info_unread_shape_star
                UnreadShape.PILL -> R.string.empty_info_unread_shape_pill
                UnreadShape.NONE -> R.string.empty_info_unread_shape_none
            },
        ),
        stringResource(R.string.empty_info_unread_silent).takeIf { countSilent },
    ).joinToString(" · ")
    is SweWidget.Battery -> stringResource(
        when (percentage) {
            BatteryPercentage.NEXT_TO -> R.string.empty_info_battery_pct_next_to
            BatteryPercentage.INSIDE -> R.string.empty_info_battery_pct_inside
            BatteryPercentage.NONE -> R.string.empty_info_battery_pct_none
        },
    )
}

/** Vertical room around the preview pill, in dp. */
private const val PREVIEW_PADDING_DP = 20

/** The camera dot's diameter as a multiple of the default camera radius. */
private const val CAMERA_DOT_SCALE = 1.2f

/** A dark grey for the camera hole, so it reads against the black pill. */
private const val PREVIEW_CAMERA_COLOR = 0xFF2A2A3A

/** Percent-to-fraction divisor. */
private const val PERCENT = 100f
