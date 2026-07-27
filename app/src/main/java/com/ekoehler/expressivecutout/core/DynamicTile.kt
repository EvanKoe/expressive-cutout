package com.ekoehler.expressivecutout.core

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Timer
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
    PHONE(Icons.Rounded.Call, R.string.tile_phone, R.string.tile_phone_desc, 0xFF22C55E),
    TIMER(Icons.Rounded.Timer, R.string.tile_timer, R.string.tile_timer_desc, 0xFFF59E0B),
}
