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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.ui.AppViewModel

/**
 * Lists the dynamic tiles the cutout can display — live, ongoing content such as the track
 * currently playing. Distinct from the "Event icons" screen, which covers the momentary system
 * events (charging, Wi‑Fi, …). Each tile has an enable/disable switch and a preview button that
 * fires a sample so the pill shows immediately.
 */
@Composable
internal fun DynamicTilesScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val tileEnabled by viewModel.tileEnabled.collectAsStateWithLifecycle()

    val tiles = DynamicTile.entries
    val lastIndex = tiles.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            contentPadding = contentPadding,
        ) {
            itemsIndexed(tiles, key = { _, tile -> tile.name }) { index, tile ->
                DynamicTileCard(
                    tile = tile,
                    shape = groupShape(index = index, lastIndex = lastIndex),
                    enabled = tileEnabled[tile] != false,
                    onEnabledChange = { viewModel.setTileEnabled(tile, it) },
                    onTest = { IslandEventBus.emit(tile.sampleSignal(context)) },
                )
            }
        }
    }
}

/** A sample signal for the tile's preview button, so tapping ▶ shows the tile on the cutout. */
private fun DynamicTile.sampleSignal(context: android.content.Context): CutoutSignal = when (this) {
    DynamicTile.MUSIC -> CutoutSignal.Music(
        packageName = context.packageName,
        title = context.getString(R.string.music_preview_title),
        artist = context.getString(R.string.music_preview_artist),
    )
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
    tile: DynamicTile,
    shape: Shape,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTest: () -> Unit,
) {
    val accent = Color(tile.accent)
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
                        imageVector = tile.defaultIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(tile.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                )
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
