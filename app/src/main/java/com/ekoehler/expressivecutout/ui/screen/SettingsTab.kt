package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.IslandPreview
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.toImageBitmap
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * "Settings" destination. A lightweight list that navigates to focused sub-screens — so the
 * heavy live preview (and the pinned overlay) only exist while the "Size & position" screen
 * is open, never on the list or the icons screen.
 */
@Composable
fun SettingsTab(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    route: SettingsRoute,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenBehaviour: () -> Unit,
) {
    // Routing (and back navigation, via the bottom bar) is owned by MainScreen.
    when (route) {
        SettingsRoute.List -> {
            val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
            SettingsList(
                contentPadding = contentPadding,
                cutoutEnabled = behaviour.cutoutEnabled,
                onCutoutEnabledChange = viewModel::setCutoutEnabled,
                onOpenSizePosition = onOpenSizePosition,
                onOpenEventIcons = onOpenEventIcons,
                onOpenBehaviour = onOpenBehaviour,
            )
        }

        SettingsRoute.SizePosition -> SizePositionScreen(viewModel, contentPadding)
        SettingsRoute.EventIcons -> EventIconsScreen(viewModel, contentPadding)
        SettingsRoute.Behaviour -> BehaviourScreen(viewModel, contentPadding)
    }
}

/** The screens reachable from the Settings tab. Hoisted to MainScreen so the bottom bar can
 *  switch to a back pill on the detail screens. */
enum class SettingsRoute { List, SizePosition, EventIcons, Behaviour }

/** Grouped-list item shape: large outer corners at the group ends, small between items. */
private fun groupedShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 32.dp else 4.dp,
    topEnd = if (isFirst) 32.dp else 4.dp,
    bottomStart = if (isLast) 32.dp else 4.dp,
    bottomEnd = if (isLast) 32.dp else 4.dp,
)

// region Settings list

@Composable
private fun SettingsList(
    contentPadding: PaddingValues,
    cutoutEnabled: Boolean,
    onCutoutEnabledChange: (Boolean) -> Unit,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenBehaviour: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CutoutEnableCard(enabled = cutoutEnabled, onEnabledChange = onCutoutEnabledChange)

        Column(
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsListItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.appearance_title),
                subtitle = stringResource(R.string.settings_size_subtitle),
                onClick = onOpenSizePosition,
            )
            SettingsListItem(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.section_icons_title),
                subtitle = stringResource(R.string.settings_icons_subtitle),
                onClick = onOpenEventIcons,
            )
            SettingsListItem(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.behaviour_title),
                subtitle = stringResource(R.string.settings_behaviour_subtitle),
                onClick = onOpenBehaviour,
            )
        }
    }
}

@Composable
private fun CutoutEnableCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                    text = stringResource(R.string.cutout_enable_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.cutout_enable_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
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

// endregion

// region Size & position screen

@Composable
private fun SizePositionScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Pin the real overlay open only on this screen, gated on accessibility. The pinned island
    // mirrors the tab being edited (collapsed vs expanded).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        fun refresh() = IslandPreviewBus.setActive(Permissions.isAccessibilityGranted(context))
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> refresh()
                Lifecycle.Event.ON_PAUSE -> IslandPreviewBus.setActive(false)
                else -> Unit
            }
        }
        refresh()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            IslandPreviewBus.setActive(false)
            IslandPreviewBus.setExpandedPreview(false)
        }
    }
    LaunchedEffect(tab) { IslandPreviewBus.setExpandedPreview(tab == 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpressiveSegmentedRow(
            options = listOf(
                stringResource(R.string.tab_normal),
                stringResource(R.string.tab_expanded),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth(),
        )
        when (tab) {
            0 -> DimensionsEditor(
                dimensions = layout.collapsed,
                defaults = IslandLayout.DEFAULT_COLLAPSED,
                expandedPreview = false,
                onChange = viewModel::setCollapsedDimensions,
            )

            else -> DimensionsEditor(
                dimensions = layout.expanded,
                defaults = IslandLayout.DEFAULT_EXPANDED,
                expandedPreview = true,
                onChange = viewModel::setExpandedDimensions,
            )
        }
    }
}

// endregion

// region Behaviour screen

@Composable
private fun BehaviourScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    var normalSeconds by remember(behaviour.normalDurationSeconds) {
        mutableStateOf(behaviour.normalDurationSeconds.toFloat())
    }
    var seconds by remember(behaviour.expandedCollapseSeconds) {
        mutableStateOf(behaviour.expandedCollapseSeconds.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Grouped list: the first item's top corners and the last item's bottom corners round.
        BehaviourSliderRow(
            shape = groupedShape(isFirst = true, isLast = false),
            label = stringResource(R.string.behaviour_normal_duration),
            valueText = "${normalSeconds.roundToInt()} s",
            value = normalSeconds,
            valueRange = BehaviourSettings.MIN_NORMAL_SECONDS.toFloat()..
                BehaviourSettings.MAX_NORMAL_SECONDS.toFloat(),
            onValueChange = { normalSeconds = it },
            onCommit = { viewModel.setNormalDurationSeconds(normalSeconds.roundToInt()) },
        )
        BehaviourToggle(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_auto_collapse),
            description = stringResource(R.string.behaviour_auto_collapse_desc),
            checked = behaviour.expandedAutoCollapse,
            onCheckedChange = viewModel::setExpandedAutoCollapse,
        )
        if (behaviour.expandedAutoCollapse) {
            BehaviourSliderRow(
                shape = groupedShape(isFirst = false, isLast = false),
                label = stringResource(R.string.behaviour_collapse_delay),
                valueText = "${seconds.roundToInt()} s",
                value = seconds,
                valueRange = BehaviourSettings.MIN_COLLAPSE_SECONDS.toFloat()..
                    BehaviourSettings.MAX_COLLAPSE_SECONDS.toFloat(),
                onValueChange = { seconds = it },
                onCommit = { viewModel.setExpandedCollapseSeconds(seconds.roundToInt()) },
            )
        }
        BehaviourToggle(
            shape = groupedShape(isFirst = false, isLast = false),
            title = stringResource(R.string.behaviour_disappear),
            description = stringResource(R.string.behaviour_disappear_desc),
            checked = behaviour.expandedDisappearOnShrink,
            onCheckedChange = viewModel::setExpandedDisappearOnShrink,
        )
        BehaviourToggle(
            shape = groupedShape(isFirst = false, isLast = true),
            title = stringResource(R.string.behaviour_notif_auto_expand),
            description = stringResource(R.string.behaviour_notif_auto_expand_desc),
            checked = behaviour.notificationsAutoExpand,
            onCheckedChange = viewModel::setNotificationsAutoExpand,
        )
    }
}

@Composable
private fun BehaviourSliderRow(
    shape: Shape,
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            AdjustableSlider(
                label = label,
                valueText = valueText,
                value = value,
                valueRange = valueRange,
                step = 1f,
                onValueChange = onValueChange,
                onCommit = onCommit,
            )
        }
    }
}

@Composable
private fun BehaviourToggle(
    shape: Shape,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// endregion

// region Event icons screen

@Composable
private fun EventIconsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val customIcons by viewModel.customIcons.collectAsStateWithLifecycle()
    val eventEnabled by viewModel.eventEnabled.collectAsStateWithLifecycle()

    var pendingImageType by remember { mutableStateOf<SystemEventType?>(null) }
    var editingType by remember { mutableStateOf<SystemEventType?>(null) }
    var appPickerType by remember { mutableStateOf<SystemEventType?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val type = pendingImageType
        pendingImageType = null
        if (uri != null && type != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setImageIcon(type, uri.toString())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            contentPadding = contentPadding
        ) {
            val lastIndex = SystemEventType.entries.lastIndex
            itemsIndexed(SystemEventType.entries, key = { _, type -> type.name }) { index, type ->
                // Grouped list: 4dp between, but the group's outer corners (first top, last
                // bottom) are 24dp.
                val shape = RoundedCornerShape(
                    topStart = if (index == 0) 32.dp else 4.dp,
                    topEnd = if (index == 0) 32.dp else 4.dp,
                    bottomStart = if (index == lastIndex) 32.dp else 4.dp,
                    bottomEnd = if (index == lastIndex) 32.dp else 4.dp,
                )
                EventIconCard(
                    type = type,
                    source = customIcons[type],
                    shape = shape,
                    enabled = eventEnabled[type] != false,
                    onEnabledChange = { viewModel.setEventEnabled(type, it) },
                    onClick = { editingType = type },
                    onTest = { IslandEventBus.emit(CutoutSignal.System(type)) },
                )
            }
        }

        editingType?.let { type ->
            IconChooserSheet(
                hasOverride = customIcons[type] != null,
                onChooseImage = {
                    editingType = null
                    pendingImageType = type
                    imagePicker.launch(arrayOf("image/*"))
                },
                onChooseApp = {
                    editingType = null
                    appPickerType = type
                },
                onUseDefault = {
                    editingType = null
                    viewModel.resetIcon(type)
                },
                onDismiss = { editingType = null },
            )
        }

        appPickerType?.let { type ->
            AppIconPickerSheet(
                onPick = { packageName ->
                    appPickerType = null
                    viewModel.setAppIcon(type, packageName)
                },
                onDismiss = { appPickerType = null },
            )
        }
    }
}

// endregion

// region Size & position

@Composable
private fun DimensionsEditor(
    dimensions: IslandDimensions,
    defaults: IslandDimensions,
    expandedPreview: Boolean,
    onChange: (IslandDimensions) -> Unit,
) {
    var width by remember(dimensions.widthPercent) { mutableStateOf(dimensions.widthPercent.toFloat()) }
    var height by remember(dimensions.heightDp) { mutableStateOf(dimensions.heightDp.toFloat()) }
    var offsetX by remember(dimensions.offsetXDp) { mutableStateOf(dimensions.offsetXDp.toFloat()) }
    var offsetY by remember(dimensions.offsetYDp) { mutableStateOf(dimensions.offsetYDp.toFloat()) }
    var cornerTl by remember(dimensions.cornerTopLeftDp) { mutableStateOf(dimensions.cornerTopLeftDp.toFloat()) }
    var cornerTr by remember(dimensions.cornerTopRightDp) { mutableStateOf(dimensions.cornerTopRightDp.toFloat()) }
    var cornerBl by remember(dimensions.cornerBottomLeftDp) { mutableStateOf(dimensions.cornerBottomLeftDp.toFloat()) }
    var cornerBr by remember(dimensions.cornerBottomRightDp) { mutableStateOf(dimensions.cornerBottomRightDp.toFloat()) }
    var cornerMode by remember { mutableStateOf(CornerMode.All) }
    // Default the preview backdrop to the phone's current light/dark setting.
    val systemInDark = isSystemInDarkTheme()
    var previewDark by remember { mutableStateOf(systemInDark) }

    fun commit() = onChange(
        IslandDimensions.of(
            widthPercent = width.roundToInt(),
            heightDp = height.roundToInt(),
            offsetXDp = offsetX.roundToInt(),
            offsetYDp = offsetY.roundToInt(),
            cornerTopLeftDp = cornerTl.roundToInt(),
            cornerTopRightDp = cornerTr.roundToInt(),
            cornerBottomLeftDp = cornerBl.roundToInt(),
            cornerBottomRightDp = cornerBr.roundToInt(),
        ),
    )

    val previewLabel = stringResource(R.string.preview_label)
    val previewDetail = stringResource(R.string.preview_detail)
    val previewEvent = remember(previewLabel, previewDetail) {
        IslandEvent(
            id = 0L,
            icon = IslandIcon.Vector(Icons.Rounded.Notifications),
            label = previewLabel,
            detail = previewDetail,
            accent = Color(0xFF60A5FA),
        )
    }
    val cutout = rememberTopCutout()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            widthPercent = width.roundToInt(),
            heightDp = height.roundToInt(),
            cornerTopLeftDp = cornerTl.roundToInt(),
            cornerTopRightDp = cornerTr.roundToInt(),
            cornerBottomLeftDp = cornerBl.roundToInt(),
            cornerBottomRightDp = cornerBr.roundToInt(),
            offsetXDp = offsetX.roundToInt(),
            offsetYDp = offsetY.roundToInt(),
            expanded = expandedPreview,
            event = previewEvent,
        )

        AdjustableSlider(
            label = stringResource(R.string.appearance_width),
            valueText = "${width.roundToInt()}%",
            value = width,
            valueRange = IslandDimensions.MIN_WIDTH_PERCENT.toFloat()..IslandDimensions.MAX_WIDTH_PERCENT.toFloat(),
            step = 1f,
            onValueChange = { width = it },
            onCommit = { commit() },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_height),
            valueText = "${height.roundToInt()} dp",
            value = height,
            valueRange = IslandDimensions.MIN_HEIGHT_DP.toFloat()..IslandDimensions.MAX_HEIGHT_DP.toFloat(),
            step = 2f,
            onValueChange = { height = it },
            onCommit = { commit() },
        )
        CornerRadiusControls(
            cornerTl = cornerTl,
            cornerTr = cornerTr,
            cornerBl = cornerBl,
            cornerBr = cornerBr,
            mode = cornerMode,
            onModeChange = { cornerMode = it },
            onTlChange = { cornerTl = it },
            onTrChange = { cornerTr = it },
            onBlChange = { cornerBl = it },
            onBrChange = { cornerBr = it },
            onCommit = { commit() },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_vertical),
            valueText = "${offsetY.roundToInt()} dp",
            value = offsetY,
            valueRange = IslandDimensions.MIN_OFFSET_Y_DP.toFloat()..IslandDimensions.MAX_OFFSET_Y_DP.toFloat(),
            step = 2f,
            onValueChange = { offsetY = it },
            onCommit = { commit() },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_horizontal),
            valueText = "${offsetX.roundToInt()} dp",
            value = offsetX,
            valueRange = IslandDimensions.MIN_OFFSET_X_DP.toFloat()..IslandDimensions.MAX_OFFSET_X_DP.toFloat(),
            step = 2f,
            onValueChange = { offsetX = it },
            onCommit = { commit() },
        )

        Spacer(Modifier.size(4.dp))
        Button(
            onClick = {
                width = defaults.widthPercent.toFloat()
                height = defaults.heightDp.toFloat()
                offsetX = defaults.offsetXDp.toFloat()
                offsetY = defaults.offsetYDp.toFloat()
                cornerTl = defaults.cornerTopLeftDp.toFloat()
                cornerTr = defaults.cornerTopRightDp.toFloat()
                cornerBl = defaults.cornerBottomLeftDp.toFloat()
                cornerBr = defaults.cornerBottomRightDp.toFloat()
                onChange(defaults)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_reset_layout))
        }
    }
}

@Composable
private fun IslandPreviewPanel(
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
    expanded: Boolean,
    event: IslandEvent,
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
                expanded = expanded,
            )
        }
    }
}

private enum class CornerMode { All, TopBottom, Each }

@Composable
private fun CornerRadiusControls(
    cornerTl: Float,
    cornerTr: Float,
    cornerBl: Float,
    cornerBr: Float,
    mode: CornerMode,
    onModeChange: (CornerMode) -> Unit,
    onTlChange: (Float) -> Unit,
    onTrChange: (Float) -> Unit,
    onBlChange: (Float) -> Unit,
    onBrChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    val range = IslandDimensions.MIN_CORNER_DP.toFloat()..IslandDimensions.MAX_CORNER_DP.toFloat()
    val modes = CornerMode.entries

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.appearance_corner),
            style = MaterialTheme.typography.bodyMedium,
        )
        ExpressiveSegmentedRow(
            options = modes.map { cornerMode ->
                when (cornerMode) {
                    CornerMode.All -> stringResource(R.string.corner_mode_all)
                    CornerMode.TopBottom -> stringResource(R.string.corner_mode_split)
                    CornerMode.Each -> stringResource(R.string.corner_mode_each)
                }
            },
            selectedIndex = mode.ordinal,
            onSelect = { onModeChange(modes[it]) },
            modifier = Modifier.fillMaxWidth(),
        )

        when (mode) {
            CornerMode.All -> CornerSlider(
                label = stringResource(R.string.appearance_corner_all),
                value = cornerTl,
                range = range,
                onValueChange = { onTlChange(it); onTrChange(it); onBlChange(it); onBrChange(it) },
                onCommit = onCommit,
            )

            CornerMode.TopBottom -> {
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_top),
                    value = cornerTl,
                    range = range,
                    onValueChange = { onTlChange(it); onTrChange(it) },
                    onCommit = onCommit,
                )
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_bottom),
                    value = cornerBl,
                    range = range,
                    onValueChange = { onBlChange(it); onBrChange(it) },
                    onCommit = onCommit,
                )
            }

            CornerMode.Each -> {
                CornerSlider(stringResource(R.string.appearance_corner_tl), cornerTl, range, onTlChange, onCommit)
                CornerSlider(stringResource(R.string.appearance_corner_tr), cornerTr, range, onTrChange, onCommit)
                CornerSlider(stringResource(R.string.appearance_corner_bl), cornerBl, range, onBlChange, onCommit)
                CornerSlider(stringResource(R.string.appearance_corner_br), cornerBr, range, onBrChange, onCommit)
            }
        }
    }
}

@Composable
private fun CornerSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    AdjustableSlider(
        label = label,
        valueText = "${value.roundToInt()} dp",
        value = value,
        valueRange = range,
        step = 1f,
        onValueChange = onValueChange,
        onCommit = onCommit,
    )
}

@Composable
private fun AdjustableSlider(
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
}

// endregion

// region Event icons

@Composable
private fun EventIconCard(
    type: SystemEventType,
    source: IconSource?,
    shape: Shape,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onTest: () -> Unit,
) {
    val context = LocalContext.current
    val defaultLabel = stringResource(R.string.label_default)
    val imageLabel = stringResource(R.string.label_custom)
    val appFallback = stringResource(R.string.label_app)
    // Resolve the app label once per package (a PackageManager binder call), not on every
    // recomposition/scroll frame.
    val appPackage = (source as? IconSource.App)?.packageName
    val resolvedAppName = remember(appPackage) { appPackage?.let { appLabelOf(context, it) } }
    val sourceLabel = when (source) {
        null -> defaultLabel
        is IconSource.Image -> imageLabel
        is IconSource.App -> resolvedAppName ?: appFallback
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tapping this area opens the icon chooser; dimmed when the event is disabled.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (enabled) 1f else 0.4f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventIconThumbnail(type = type, source = source)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(type.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalIconButton(onClick = onTest, enabled = enabled) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_test_event),
                )
            }
            Spacer(Modifier.width(4.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun EventIconThumbnail(type: SystemEventType, source: IconSource?) {
    val context = LocalContext.current
    val accent = Color(type.accent)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = when (val current = source) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                Uri.parse(current.uri).loadImageBitmapOrNull(context)
            }

            is IconSource.App -> withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(current.packageName).toImageBitmap()
                }.getOrNull()
            }

            null -> null
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            androidx.compose.foundation.Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = type.defaultIcon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconChooserSheet(
    hasOverride: Boolean,
    onChooseImage: () -> Unit,
    onChooseApp: () -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.set_icon_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.action_choose_image)) },
            leadingContent = { Icon(Icons.Rounded.Image, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onChooseImage),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.action_choose_app)) },
            leadingContent = { Icon(Icons.Rounded.Apps, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onChooseApp),
        )
        if (hasOverride) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_use_default)) },
                leadingContent = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onUseDefault),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun appLabelOf(context: Context, packageName: String): String? = runCatching {
    val info = context.packageManager.getApplicationInfo(packageName, 0)
    context.packageManager.getApplicationLabel(info).toString()
}.getOrNull()

// endregion

// region Device cutout

private data class TopCutout(
    val widthPx: Int,
    val heightPx: Int,
    val centerXFraction: Float,
)

/** Reads the device's top display cutout once, or null if there isn't one to represent. */
@Composable
private fun rememberTopCutout(): TopCutout? {
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

// endregion
