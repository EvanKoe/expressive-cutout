package com.ekoehler.expressivecutout.core

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekoehler.expressivecutout.R

/**
 * The closed set of *dynamic tiles* the cutout can display — live, ongoing content (e.g. the
 * track currently playing) as opposed to the momentary device happenings in [SystemEventType].
 * Each tile can be turned on or off independently on the "Dynamic tiles" screen.
 */
enum class DynamicTile(
    val defaultIcon: ImageVector,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    val accent: Long,
) {
    MUSIC(Icons.Rounded.MusicNote, R.string.tile_music, R.string.tile_music_desc, 0xFFF472B6),
}
