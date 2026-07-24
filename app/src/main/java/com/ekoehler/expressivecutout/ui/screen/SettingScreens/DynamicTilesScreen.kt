package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.ui.AppViewModel

/** Pink accent for the music tile, matching IconResolver.MUSIC_ACCENT. */
private val MusicAccent = Color(0xFFF472B6)

/**
 * Lists every dynamic event the cutout can display — the now-playing music tile followed by each
 * system event — as one grouped list of enable/disable switches. Each row also offers a preview
 * button that fires the matching signal so the pill shows immediately.
 */
@Composable
internal fun DynamicTilesScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val eventEnabled by viewModel.eventEnabled.collectAsStateWithLifecycle()
    val musicEnabled by viewModel.musicEnabled.collectAsStateWithLifecycle()

    val systemTypes = SystemEventType.entries
    // Music takes the first slot; the system events fill the rest. Last index is the group bottom.
    val lastIndex = systemTypes.size

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            contentPadding = contentPadding,
        ) {
            item(key = "music") {
                DynamicTileCard(
                    icon = Icons.Rounded.MusicNote,
                    accent = MusicAccent,
                    title = stringResource(R.string.event_music_playing),
                    shape = groupShape(index = 0, lastIndex = lastIndex),
                    enabled = musicEnabled,
                    onEnabledChange = viewModel::setMusicEnabled,
                    onTest = {
                        IslandEventBus.emit(
                            CutoutSignal.Music(
                                packageName = context.packageName,
                                title = context.getString(R.string.music_preview_title),
                                artist = context.getString(R.string.music_preview_artist),
                            ),
                        )
                    },
                )
            }

            itemsIndexed(systemTypes, key = { _, type -> type.name }) { index, type ->
                DynamicTileCard(
                    icon = type.defaultIcon,
                    accent = Color(type.accent),
                    title = stringResource(type.labelRes),
                    shape = groupShape(index = index + 1, lastIndex = lastIndex),
                    enabled = eventEnabled[type] != false,
                    onEnabledChange = { viewModel.setEventEnabled(type, it) },
                    onTest = { IslandEventBus.emit(CutoutSignal.System(type)) },
                )
            }
        }
    }
}

/** Grouped-list corners: the group's outer corners (first top, last bottom) are 32dp, rest 4dp. */
private fun groupShape(index: Int, lastIndex: Int): Shape = RoundedCornerShape(
    topStart = if (index == 0) 32.dp else 4.dp,
    topEnd = if (index == 0) 32.dp else 4.dp,
    bottomStart = if (index == lastIndex) 32.dp else 4.dp,
    bottomEnd = if (index == lastIndex) 32.dp else 4.dp,
)

@Composable
private fun DynamicTileCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    shape: Shape,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTest: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (enabled) 1f else 0.4f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
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
