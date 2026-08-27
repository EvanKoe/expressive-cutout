package com.ekoehler.expressivecutout.overlay

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.core.NowPlayingBus
import com.ekoehler.expressivecutout.core.OnCallBus
import com.ekoehler.expressivecutout.data.AppearanceSettings

/**
 * Gap between the pill and the satellite bubble — deliberately tight, so the two read as one system
 * rather than as two unrelated overlays. Shared with [IslandOverlayController], which reserves the
 * same room in the overlay window and in the touchable region.
 */
internal const val SATELLITE_GAP_DP = 8

/**
 * The satellite bubble: the event the pill displaced, parked beside it so it stays visible instead of
 * disappearing. Deliberately a circle of the collapsed pill's own height rather than a second pill —
 * a circle reads as "secondary" without the eye having to compare widths, and its width is a
 * constant, so the pill beside it never jitters as events come and go.
 *
 * Drawn with the same [IslandSurface] and the same badge composables as the collapsed pill, so the
 * two can't drift apart in how they render a theme, a colour override or a tile's album art.
 */
@Composable
internal fun SatelliteBubble(
    event: IslandEvent,
    diameterDp: Int,
    appearance: AppearanceSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)
    val callPhoto = event.call?.takeIf { it.showPhoto }?.let { onCall?.photo }
    val badgeSize = (diameterDp * 0.72f).dp

    IslandSurface(
        modifier = modifier
            .size(diameterDp.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        shape = CircleShape,
        appearance = appearance,
        // The bubble is only ever a collapsed thing, so it never fades towards the expanded look.
        progress = 0f,
        appColor = event.appColor,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                albumArt != null -> AlbumArt(
                    bitmap = albumArt,
                    size = badgeSize,
                    rotate = event.media?.rotateAlbumArt == true,
                    playing = nowPlaying?.isPlaying == true,
                    strokeColor = albumArtStrokeFor(event),
                )

                callPhoto != null -> ContactPhoto(bitmap = callPhoto, size = badgeSize)

                else -> IconBadge(
                    event = event,
                    badgeSize = badgeSize,
                    iconSize = (diameterDp * 0.46f).dp,
                )
            }
        }
    }
}
