package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.IslandPreview
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.toImageBitmap
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.ui.AppViewModel
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
) {
    var route by rememberSaveable { mutableStateOf(SettingsRoute.List) }

    // System back returns to the list before leaving the tab.
    BackHandler(enabled = route != SettingsRoute.List) { route = SettingsRoute.List }

    when (route) {
        SettingsRoute.List -> SettingsList(
            contentPadding = contentPadding,
            onOpenSizePosition = { route = SettingsRoute.SizePosition },
            onOpenEventIcons = { route = SettingsRoute.EventIcons },
        )

        SettingsRoute.SizePosition -> SizePositionScreen(
            viewModel = viewModel,
            contentPadding = contentPadding,
            onBack = { route = SettingsRoute.List },
        )

        SettingsRoute.EventIcons -> EventIconsScreen(
            viewModel = viewModel,
            contentPadding = contentPadding,
            onBack = { route = SettingsRoute.List },
        )
    }
}

private enum class SettingsRoute { List, SizePosition, EventIcons }

// region Settings list

@Composable
private fun SettingsList(
    contentPadding: PaddingValues,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(16.dp))
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
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    // Pin the real overlay open only on this screen, and only when accessibility is granted
    // (there is nothing to pin otherwise). Unpin when leaving or backgrounding.
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
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubPageHeader(title = stringResource(R.string.appearance_title), onBack = onBack)
        AppearanceSection(
            layout = layout,
            onWidthChange = viewModel::setWidth,
            onHeightChange = viewModel::setHeight,
            onOffsetXChange = viewModel::setOffsetX,
            onOffsetYChange = viewModel::setOffsetY,
            onReset = viewModel::resetLayout,
        )
    }
}

// endregion

// region Event icons screen

@Composable
private fun EventIconsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val customIcons by viewModel.customIcons.collectAsStateWithLifecycle()

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
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SubPageHeader(title = stringResource(R.string.section_icons_title), onBack = onBack) }
            items(SystemEventType.entries, key = { it.name }) { type ->
                EventIconCard(
                    type = type,
                    source = customIcons[type],
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

@Composable
private fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}

// endregion

// region Size & position

@Composable
private fun AppearanceSection(
    layout: IslandLayout,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onOffsetXChange: (Int) -> Unit,
    onOffsetYChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    var width by remember(layout.widthDp) { mutableStateOf(layout.widthDp.toFloat()) }
    var height by remember(layout.heightDp) { mutableStateOf(layout.heightDp.toFloat()) }
    var offsetX by remember(layout.offsetXDp) { mutableStateOf(layout.offsetXDp.toFloat()) }
    var offsetY by remember(layout.offsetYDp) { mutableStateOf(layout.offsetYDp.toFloat()) }
    var previewDark by remember { mutableStateOf(true) }

    val previewLabel = stringResource(R.string.preview_label)
    val previewEvent = remember(previewLabel) {
        IslandEvent(
            id = 0L,
            icon = IslandIcon.Vector(Icons.Rounded.Notifications),
            label = previewLabel,
            accent = Color(0xFF60A5FA),
        )
    }
    val cutout = rememberTopCutout()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 2.dp),
        )
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

        // A single preview whose background the user flips, showing the real cutout to scale.
        IslandPreviewPanel(
            background = if (previewDark) Color(0xFF0B0B0C) else Color(0xFFEDEFF3),
            cutout = cutout,
            widthDp = width.roundToInt(),
            heightDp = height.roundToInt(),
            offsetXDp = offsetX.roundToInt(),
            offsetYDp = offsetY.roundToInt(),
            event = previewEvent,
        )

        AdjustableSlider(
            label = stringResource(R.string.appearance_width),
            valueText = "${width.roundToInt()} dp",
            value = width,
            valueRange = IslandLayout.MIN_WIDTH_DP.toFloat()..IslandLayout.MAX_WIDTH_DP.toFloat(),
            step = 4f,
            onValueChange = { width = it },
            onCommit = { onWidthChange(width.roundToInt()) },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_height),
            valueText = "${height.roundToInt()} dp",
            value = height,
            valueRange = IslandLayout.MIN_HEIGHT_DP.toFloat()..IslandLayout.MAX_HEIGHT_DP.toFloat(),
            step = 2f,
            onValueChange = { height = it },
            onCommit = { onHeightChange(height.roundToInt()) },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_vertical),
            valueText = "${offsetY.roundToInt()} dp",
            value = offsetY,
            valueRange = IslandLayout.MIN_OFFSET_Y_DP.toFloat()..IslandLayout.MAX_OFFSET_Y_DP.toFloat(),
            step = 2f,
            onValueChange = { offsetY = it },
            onCommit = { onOffsetYChange(offsetY.roundToInt()) },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_horizontal),
            valueText = "${offsetX.roundToInt()} dp",
            value = offsetX,
            valueRange = IslandLayout.MIN_OFFSET_X_DP.toFloat()..IslandLayout.MAX_OFFSET_X_DP.toFloat(),
            step = 2f,
            onValueChange = { offsetX = it },
            onCommit = { onOffsetXChange(offsetX.roundToInt()) },
        )

        Spacer(Modifier.size(4.dp))
        Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
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
    widthDp: Int,
    heightDp: Int,
    offsetXDp: Int,
    offsetYDp: Int,
    event: IslandEvent,
) {
    val cutoutOutline = Color.White.copy(alpha = 0.28f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background),
    ) {
        val density = LocalDensity.current
        val panelWidth = maxWidth

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
                x = panelWidth / 2f + offsetXDp.dp - widthDp.dp / 2f,
                y = offsetYDp.dp,
            ),
        ) {
            IslandPreview(event = event, widthDp = widthDp, heightDp = heightDp)
        }
    }
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
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EventIconThumbnail(type = type, source = source)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            TextButton(onClick = onClick) { Text(stringResource(R.string.action_change)) }
            FilledTonalIconButton(onClick = onTest) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_test_event),
                )
            }
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
