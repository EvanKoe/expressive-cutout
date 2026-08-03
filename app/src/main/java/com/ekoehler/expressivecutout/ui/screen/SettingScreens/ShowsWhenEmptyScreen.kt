package com.ekoehler.expressivecutout.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CenterShortcut
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.EmptyClickAction
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.overlay.CenterShortcutCatalog
import com.ekoehler.expressivecutout.overlay.MaterialIconCatalog
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The empty pill's detail screen, reached from the Behaviour list. Lets the user give the resting
 * (event-less) pill a glyph: a "Show icon" toggle, and — while it's on — an icon picker (a custom
 * image or a built-in Material icon) plus a container-colour picker for the disc behind it.
 */
@Composable
internal fun ShowsWhenEmptyScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val showIcon = behaviour.showsWhenEmptyShowIcon
    val source = behaviour.showsWhenEmptyIcon
    val containerColor = behaviour.showsWhenEmptyIconColor
    val clickAction = behaviour.showsWhenEmptyClickAction
    val clickPackage = behaviour.showsWhenEmptyClickPackage

    var showIconSheet by remember { mutableStateOf(false) }
    var showMaterialPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showAddShortcut by remember { mutableStateOf(false) }
    var showCenterAppPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setShowsWhenEmptyImageIcon(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(32.dp),
            title = stringResource(R.string.shows_when_empty_show_icon),
            description = stringResource(R.string.shows_when_empty_show_icon_desc),
            checked = showIcon,
            onCheckedChange = viewModel::setShowsWhenEmptyShowIcon,
        )

        // The icon + colour controls only make sense once the icon is switched on.
        AnimatedVisibility(visible = showIcon) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Hero: the chosen icon at a glance, plus the "change icon" action.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        EmptyIconThumbnail(source = source, containerColor = containerColor, size = 76.dp)
                        FilledTonalButton(onClick = { showIconSheet = true }) {
                            Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                            Text(
                                text = stringResource(R.string.event_change_icon),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }

                // The colour of the disc drawn behind the glyph on the pill.
                ColorPickerCard(
                    label = stringResource(R.string.shows_when_empty_container_color),
                    selected = containerColor,
                    onSelect = { viewModel.setShowsWhenEmptyIconColor(it) },
                    defaultLabel = stringResource(R.string.label_default),
                    defaultColor = Color.White,
                )
            }
        }

        // What tapping the resting pill does: nothing, open an app, or expand the shortcut center.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.shows_when_empty_on_click),
                    style = MaterialTheme.typography.titleMedium,
                )
                ExpressiveSegmentedRow(
                    options = listOf(
                        stringResource(R.string.shows_when_empty_click_none),
                        stringResource(R.string.shows_when_empty_click_open_app),
                        stringResource(R.string.shows_when_empty_click_open_center),
                    ),
                    selectedIndex = clickAction.ordinal,
                    onSelect = { viewModel.setShowsWhenEmptyClickAction(EmptyClickAction.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Once "Open app" is chosen, pick which app the tap launches.
        AnimatedVisibility(visible = clickAction == EmptyClickAction.OPEN_APP) {
            AppChoiceCard(
                packageName = clickPackage,
                onChoose = { showAppPicker = true },
            )
        }

        // Once "Open center" is chosen, edit the shortcuts shown in the expanded center.
        AnimatedVisibility(visible = clickAction == EmptyClickAction.OPEN_CENTER) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsToggleCard(
                    shape = RoundedCornerShape(32.dp),
                    title = stringResource(R.string.shows_when_empty_center_labels),
                    description = stringResource(R.string.shows_when_empty_center_labels_desc),
                    checked = behaviour.centerShowLabels,
                    onCheckedChange = viewModel::setCenterShowLabels,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(32.dp),
                    title = stringResource(R.string.shows_when_empty_center_fill),
                    description = stringResource(R.string.shows_when_empty_center_fill_desc),
                    checked = behaviour.centerFillContainers,
                    onCheckedChange = viewModel::setCenterFillContainers,
                )
                // Only meaningful when the row includes an app — themed icons apply to app shortcuts.
                if (behaviour.centerShortcuts.any { it is CenterShortcut.LaunchApp }) {
                    SettingsToggleCard(
                        shape = RoundedCornerShape(32.dp),
                        title = stringResource(R.string.shows_when_empty_center_themed),
                        description = stringResource(R.string.shows_when_empty_center_themed_desc),
                        checked = behaviour.centerThemedIcons,
                        onCheckedChange = viewModel::setCenterThemedIcons,
                    )
                }
                CenterShortcutsCard(
                    shortcuts = behaviour.centerShortcuts,
                    onAdd = { showAddShortcut = true },
                    onRemove = { index ->
                        viewModel.setCenterShortcuts(
                            behaviour.centerShortcuts.toMutableList().apply { removeAt(index) },
                        )
                    },
                    onMove = { index, delta ->
                        viewModel.setCenterShortcuts(behaviour.centerShortcuts.moved(index, delta))
                    },
                )
            }
        }
    }

    if (showIconSheet) {
        IconChooserSheet(
            hasOverride = source != null,
            onChooseImage = {
                showIconSheet = false
                imagePicker.launch(arrayOf("image/*"))
            },
            onChooseMaterial = {
                showIconSheet = false
                showMaterialPicker = true
            },
            onUseDefault = {
                showIconSheet = false
                viewModel.resetShowsWhenEmptyIcon()
            },
            onDismiss = { showIconSheet = false },
        )
    }

    if (showMaterialPicker) {
        MaterialIconPickerSheet(
            onPick = { iconName ->
                showMaterialPicker = false
                viewModel.setShowsWhenEmptyMaterialIcon(iconName)
            },
            onDismiss = { showMaterialPicker = false },
        )
    }

    if (showAppPicker) {
        AppPickerSheet(
            onPick = { packageName ->
                showAppPicker = false
                viewModel.setShowsWhenEmptyClickPackage(packageName)
            },
            onDismiss = { showAppPicker = false },
        )
    }

    if (showAddShortcut) {
        AddShortcutSheet(
            onPickBuiltin = { shortcut ->
                showAddShortcut = false
                viewModel.setCenterShortcuts(behaviour.centerShortcuts + shortcut)
            },
            onChooseApp = {
                showAddShortcut = false
                showCenterAppPicker = true
            },
            onDismiss = { showAddShortcut = false },
        )
    }

    if (showCenterAppPicker) {
        AppPickerSheet(
            onPick = { packageName ->
                showCenterAppPicker = false
                viewModel.setCenterShortcuts(
                    behaviour.centerShortcuts + CenterShortcut.LaunchApp(packageName),
                )
            },
            onDismiss = { showCenterAppPicker = false },
        )
    }
}

/** Moves the item at [index] by [delta] positions, returning the list unchanged if out of range. */
private fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

/** The chosen "open app" target: the app's icon + name, or a hint to pick one, plus a choose button. */
@Composable
private fun AppChoiceCard(packageName: String?, onChoose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (packageName != null) {
                AppIcon(packageName = packageName)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageName?.let { rememberAppLabel(it) }
                        ?: stringResource(R.string.shows_when_empty_no_app),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (packageName != null) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onChoose) {
                Text(stringResource(R.string.shows_when_empty_choose_app))
            }
        }
    }
}

/**
 * The editor for the expanded center's shortcut row: the current shortcuts in order, each with
 * move-up / move-down / remove controls, plus an "Add shortcut" button. Shown while the empty
 * pill's on-click is [EmptyClickAction.OPEN_CENTER].
 */
@Composable
private fun CenterShortcutsCard(
    shortcuts: List<CenterShortcut>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.shows_when_empty_center_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.shows_when_empty_center_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (shortcuts.isEmpty()) {
                Text(
                    text = stringResource(R.string.center_shortcuts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                shortcuts.forEachIndexed { index, shortcut ->
                    CenterShortcutRow(
                        shortcut = shortcut,
                        canMoveUp = index > 0,
                        canMoveDown = index < shortcuts.lastIndex,
                        onMoveUp = { onMove(index, -1) },
                        onMoveDown = { onMove(index, 1) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
            FilledTonalButton(onClick = onAdd, modifier = Modifier.padding(top = 4.dp)) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.center_add_shortcut),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** One shortcut in the editor: its glyph (or app icon) and label, with reorder and remove controls. */
@Composable
private fun CenterShortcutRow(
    shortcut: CenterShortcut,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CenterShortcutIcon(shortcut)
        Text(
            text = centerShortcutLabel(shortcut),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.center_move_up))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.center_move_down))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.center_remove))
        }
    }
}

/** A round badge for a shortcut: the real launcher icon for an app, otherwise its catalog glyph. */
@Composable
private fun CenterShortcutIcon(shortcut: CenterShortcut) {
    if (shortcut is CenterShortcut.LaunchApp) {
        AppIcon(packageName = shortcut.packageName)
        return
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CenterShortcutCatalog.iconFor(shortcut),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** A shortcut's label: its fixed string resource, or the app's display name for a launcher. */
@Composable
private fun centerShortcutLabel(shortcut: CenterShortcut): String {
    CenterShortcutCatalog.labelResFor(shortcut)?.let { return stringResource(it) }
    val pkg = (shortcut as? CenterShortcut.LaunchApp)?.packageName ?: return ""
    return rememberAppLabel(pkg)
}

/** A bottom sheet to add a shortcut: pick an app, or any of the built-in actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShortcutSheet(
    onPickBuiltin: (CenterShortcut) -> Unit,
    onChooseApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.center_add_shortcut),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            item {
                AddShortcutRow(
                    icon = Icons.Rounded.Apps,
                    label = stringResource(R.string.center_add_choose_app),
                    onClick = onChooseApp,
                )
            }
            items(CenterShortcut.BUILTINS, key = { it.encode() }) { shortcut ->
                AddShortcutRow(
                    icon = CenterShortcutCatalog.iconFor(shortcut),
                    label = centerShortcutLabel(shortcut),
                    onClick = { onPickBuiltin(shortcut) },
                )
            }
        }
    }
}

/** A single tappable row in the add-shortcut sheet: a round glyph badge and a label. */
@Composable
private fun AddShortcutRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

/** A searchable bottom-sheet list of launchable apps; reports the chosen package via [onPick]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apps by produceState<List<InstalledApp>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.shows_when_empty_choose_app),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.apps_search_hint)) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))

        val loaded = apps
        if (loaded == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val filtered = remember(loaded, query) {
                val needle = query.trim()
                if (needle.isEmpty()) loaded
                else loaded.filter {
                    it.label.contains(needle, ignoreCase = true) ||
                        it.packageName.contains(needle, ignoreCase = true)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(app.packageName) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(packageName = app.packageName)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Resolves a package's display label off the main thread, falling back to the package name. */
@Composable
private fun rememberAppLabel(packageName: String): String {
    val context = LocalContext.current
    val label by produceState(initialValue = packageName, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    }
    return label
}

/**
 * A round badge previewing the empty pill's chosen icon: a [containerColor] disc (a faint neutral
 * one when unset) behind the glyph — a picked image, a Material vector, or a placeholder when no
 * icon has been chosen yet. Mirrors how the overlay draws it on the resting pill.
 */
@Composable
private fun EmptyIconThumbnail(
    source: IconSource?,
    containerColor: CutoutColor?,
    size: Dp = 48.dp,
) {
    val context = LocalContext.current
    val disc = containerColor?.resolve() ?: MaterialTheme.colorScheme.surfaceVariant
    // Ink that reads on the disc: dark on a light fill, light on a dark one.
    val glyph = if (disc.luminance() > 0.5f) Color.Black.copy(alpha = 0.75f) else Color.White

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = when (val current = source) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                Uri.parse(current.uri).loadImageBitmapOrNull(context)
            }
            is IconSource.Material, null -> null
        }
    }
    val materialIcon = (source as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(disc),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        when {
            loaded != null -> Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.size(size * 0.66f).clip(CircleShape),
            )

            materialIcon != null -> Icon(
                imageVector = materialIcon,
                contentDescription = null,
                tint = glyph,
                modifier = Modifier.size(size * 0.5f),
            )

            // No icon picked yet: a faint "add an image" hint glyph.
            else -> Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = glyph.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}
