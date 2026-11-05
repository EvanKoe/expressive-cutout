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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.DEFAULT_CAMERA_RADIUS_DP
import com.ekoehler.expressivecutout.data.EmptyPillInfoSettings
import com.ekoehler.expressivecutout.data.InfoSide
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.asEmptyInfoCutout
import com.ekoehler.expressivecutout.events.BatteryStatus
import com.ekoehler.expressivecutout.overlay.EmptyPillInfoContent
import com.ekoehler.expressivecutout.overlay.emptyPillSideWidthsDp
import com.ekoehler.expressivecutout.service.UnreadCounts
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.InfoSegmentCard
import com.ekoehler.expressivecutout.ui.components.groupedShape

/** The unread count the preview shows, so a badge is always visible while editing. */
private val PreviewCounts = UnreadCounts(all = 3, nonSilent = 3)

/** The battery reading the preview shows. */
private val PreviewBattery = BatteryStatus(level = 82, charging = false)

/** The live values the preview widgets draw and are sized from. */
private val PreviewData = SweData(PreviewCounts, PreviewBattery)

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
        val (left, right) = emptyPillSideWidthsDp(settings, collapsed.heightDp, density, PreviewData)
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
                            data = PreviewData,
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
            WidgetBadge(icon = widget.type.icon)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(text = stringResource(widget.type.titleRes), style = MaterialTheme.typography.bodyLarge)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = stringResource(R.string.swe_add_widget),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SweWidgets.all.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(type.create()) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetBadge(icon = type.icon)
                    Text(
                        text = stringResource(type.titleRes),
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
                text = stringResource(widget.type.titleRes),
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
            widget.Controls(onChange)
        }
    }
}

/** Vertical room around the preview pill, in dp. */
private const val PREVIEW_PADDING_DP = 20

/** The camera dot's diameter as a multiple of the default camera radius. */
private const val CAMERA_DOT_SCALE = 1.2f

/** A dark grey for the camera hole, so it reads against the black pill. */
private const val PREVIEW_CAMERA_COLOR = 0xFF2A2A3A

