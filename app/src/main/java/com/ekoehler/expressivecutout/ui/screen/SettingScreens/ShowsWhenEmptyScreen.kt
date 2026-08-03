package com.ekoehler.expressivecutout.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.overlay.MaterialIconCatalog
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
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

    var showIconSheet by remember { mutableStateOf(false) }
    var showMaterialPicker by remember { mutableStateOf(false) }

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
