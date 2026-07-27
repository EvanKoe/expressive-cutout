package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.toImageBitmap
import com.ekoehler.expressivecutout.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun EventIconsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val customIcons by viewModel.customIcons.collectAsStateWithLifecycle()
    val eventEnabled by viewModel.eventEnabled.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.eventDynamicColor.collectAsStateWithLifecycle()

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
            // The dynamic-colour toggle is the top row of the same grouped list, so it carries the
            // group's rounded top corners; the events below flow on beneath it.
            item(key = "dynamic_color") {
                SettingsToggleCard(
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp,
                    ),
                    title = stringResource(R.string.dynamic_event_color),
                    description = stringResource(R.string.dynamic_event_color_desc),
                    checked = dynamicColor,
                    onCheckedChange = { viewModel.setEventDynamicColor(it) },
                )
            }
            itemsIndexed(SystemEventType.entries, key = { _, type -> type.name }) { index, type ->
                // Grouped list: 4dp between, and the last event carries the group's rounded bottom
                // corners (the toggle above holds the top ones).
                val shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = if (index == lastIndex) 32.dp else 4.dp,
                    bottomEnd = if (index == lastIndex) 32.dp else 4.dp,
                )
                EventIconCard(
                    type = type,
                    source = customIcons[type],
                    shape = shape,
                    enabled = eventEnabled[type] != false,
                    dynamicColor = dynamicColor,
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

@Composable
private fun EventIconCard(
    type: SystemEventType,
    source: IconSource?,
    shape: Shape,
    enabled: Boolean,
    dynamicColor: Boolean,
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
                EventIconThumbnail(type = type, source = source, dynamicColor = dynamicColor)
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
private fun EventIconThumbnail(type: SystemEventType, source: IconSource?, dynamicColor: Boolean) {
    val context = LocalContext.current
    // Mirror the overlay's IconBadge: with "Dynamic color" on, a solid primary disc + on-primary
    // glyph replaces the event's own accent tint.
    val badgeColor = if (dynamicColor) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(type.accent).copy(alpha = 0.18f)
    }
    val glyphColor = if (dynamicColor) MaterialTheme.colorScheme.onPrimary else Color(type.accent)
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
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = type.defaultIcon,
                contentDescription = null,
                tint = glyphColor,
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
