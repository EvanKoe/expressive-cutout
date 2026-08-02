package com.ekoehler.expressivecutout.ui.screen.tiles

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.screen.AssistantScreen

/**
 * Routes a [DynamicTile] to its own settings screen. Each tile's settings live in a dedicated file
 * in this package (e.g. [MusicTileScreen]); add a branch here when introducing a new tile.
 */
@Composable
internal fun TileSettingsScreen(
    tile: DynamicTile,
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    when (tile) {
        DynamicTile.MUSIC -> MusicTileScreen(viewModel, contentPadding)
        DynamicTile.PHONE -> PhoneTileScreen(viewModel, contentPadding)
        DynamicTile.TIMER -> TimerTileScreen(viewModel, contentPadding)
        DynamicTile.ASSISTANT -> AssistantScreen(viewModel, contentPadding)
    }
}
