package com.ekoehler.expressivecutout.ui.screen

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.onForRole
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel

/**
 * Lists the dynamic tiles the cutout can display — live, ongoing content such as the track
 * currently playing. Distinct from the "Events" screen, which covers momentary system events.
 * Tapping a tile opens its own settings screen; the trailing switch enables or disables it.
 */
@Composable
internal fun DynamicTilesScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenTile: (DynamicTile) -> Unit,
) {
    val tileEnabled by viewModel.tileEnabled.collectAsStateWithLifecycle()
    // The icon-container colour each tile lets the user pick, mirrored onto its list badge below.
    val phone by viewModel.phoneTile.collectAsStateWithLifecycle()
    val timer by viewModel.timerTile.collectAsStateWithLifecycle()

    val tiles = DynamicTile.entries
    val lastIndex = tiles.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            contentPadding = contentPadding,
        ) {
            itemsIndexed(tiles, key = { _, tile -> tile.name }) { index, tile ->
                // Music shows the album art / app icon, never a coloured badge, so it has no override.
                val containerColor = when (tile) {
                    DynamicTile.PHONE -> phone.iconContainerColor
                    DynamicTile.TIMER -> timer.iconContainerColor
                    DynamicTile.MUSIC -> null
                }
                DynamicTileCard(
                    tile = tile,
                    shape = groupShape(index = index, lastIndex = lastIndex),
                    enabled = tileEnabled[tile] != false,
                    containerColor = containerColor,
                    onClick = { onOpenTile(tile) },
                    onEnabledChange = { viewModel.setTileEnabled(tile, it) },
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
    tile: DynamicTile,
    shape: Shape,
    enabled: Boolean,
    containerColor: CutoutColor?,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val accent = Color(tile.accent)
    // A chosen container colour fills the badge and contrasts its glyph, matching the live cutout;
    // otherwise the badge keeps the faint accent-tinted default.
    val badgeColor: Color
    val glyphColor: Color
    if (containerColor != null) {
        badgeColor = containerColor.resolve()
        glyphColor = when (containerColor) {
            is CutoutColor.Dynamic -> MaterialTheme.colorScheme.onForRole(containerColor.role)
            is CutoutColor.Solid -> if (badgeColor.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White
        }
    } else {
        badgeColor = accent.copy(alpha = 0.18f)
        glyphColor = accent
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tile.defaultIcon,
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(tile.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(tile.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Thin divider between the (tappable) row and the switch, as in the mock.
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}
