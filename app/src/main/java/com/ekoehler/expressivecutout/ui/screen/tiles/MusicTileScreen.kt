package com.ekoehler.expressivecutout.ui.screen.tiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import com.ekoehler.expressivecutout.ui.screen.ColorPickerCard
import com.ekoehler.expressivecutout.ui.screen.SettingsToggleCard
import kotlin.math.roundToInt

/** The pink tile accent, used as the play/pause default and the preview backdrop's default fill. */
private val MusicAccent = Color(0xFFF472B6)

/** Fallback fill for a button asked to be [MusicButtonStyle.filled] before the user picks a colour. */
private val MusicButtonFilledDefault = Color(0xFFE0E0E0)

/**
 * Settings specific to the music dynamic tile: whether to show the album art on the normal cutout,
 * whether to show playback controls (previous / play‑pause / next) on the expanded cutout, and the
 * colour, opacity and corner rounding of each control button.
 */
@Composable
internal fun MusicTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.musicTile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            title = stringResource(R.string.music_show_art_title),
            description = stringResource(R.string.music_show_art_desc),
            checked = settings.showAlbumArt,
            onCheckedChange = viewModel::setMusicShowAlbumArt,
        )
        // Rotation only applies to the album cover, so it rides with the album-art toggle.
        if (settings.showAlbumArt) {
            SettingsToggleCard(
                shape = RoundedCornerShape(4.dp),
                title = stringResource(R.string.music_rotate_art_title),
                description = stringResource(R.string.music_rotate_art_desc),
                checked = settings.rotateAlbumArt,
                onCheckedChange = viewModel::setMusicRotateAlbumArt,
            )
        }
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
            title = stringResource(R.string.music_show_controls_title),
            description = stringResource(R.string.music_show_controls_desc),
            checked = settings.showControls,
            onCheckedChange = viewModel::setMusicShowControls,
        )

        // Everything below styles the playback buttons; only meaningful when they're shown.
        if (settings.showControls) {
            SectionLabel(stringResource(R.string.music_buttons_title))
            MusicButtonsPreview(
                skipStyle = settings.skipButton,
                playPauseStyle = settings.playPauseButton,
            )

            SectionLabel(stringResource(R.string.music_skip_buttons_title))
            ButtonPresetRow(
                current = settings.skipButton,
                sampleFill = settings.skipButton.previewFill(fallback = null) ?: MusicButtonFilledDefault,
                onApply = viewModel::applyMusicSkipPreset,
            )
            ColorPickerCard(
                label = stringResource(R.string.music_button_color),
                selected = settings.skipButton.color,
                onSelect = viewModel::setMusicSkipColor,
                defaultLabel = stringResource(R.string.cd_color_default_plain),
                defaultColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ButtonShapeCard(
                style = settings.skipButton,
                onOpacityCommit = viewModel::setMusicSkipOpacity,
                onCornerCommit = viewModel::setMusicSkipCornerPercent,
            )

            SectionLabel(stringResource(R.string.music_playpause_button_title))
            ButtonPresetRow(
                current = settings.playPauseButton,
                sampleFill = settings.playPauseButton.previewFill(fallback = MusicAccent) ?: MusicAccent,
                onApply = viewModel::applyMusicPlayPausePreset,
            )
            ColorPickerCard(
                label = stringResource(R.string.music_button_color),
                selected = settings.playPauseButton.color,
                onSelect = viewModel::setMusicPlayPauseColor,
                defaultLabel = stringResource(R.string.music_default_accent),
                defaultColor = MusicAccent,
            )
            ButtonShapeCard(
                style = settings.playPauseButton,
                onOpacityCommit = viewModel::setMusicPlayPauseOpacity,
                onCornerCommit = viewModel::setMusicPlayPauseCornerPercent,
            )
        }
    }
}

/** A small caption above a group of related cards. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
    )
}

/**
 * A row of tappable preset chips (rounded rectangle, pill). Tapping one applies the preset's shape
 * and fill to the button while keeping its current colour; the chip matching the current style is
 * highlighted. [sampleFill] is the colour the button would fill with, used only for the swatch.
 */
@Composable
private fun ButtonPresetRow(
    current: MusicButtonStyle,
    sampleFill: Color,
    onApply: (MusicButtonStyle) -> Unit,
) {
    val labels = mapOf(
        MusicButtonStyle.ROUNDED to stringResource(R.string.music_preset_rounded),
        MusicButtonStyle.PILL to stringResource(R.string.music_preset_pill),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MusicButtonStyle.PRESETS.forEach { preset ->
            val selected = current.filled == preset.filled &&
                current.cornerPercent == preset.cornerPercent
            PresetChip(
                label = labels[preset].orEmpty(),
                fill = sampleFill,
                cornerPercent = preset.cornerPercent,
                selected = selected,
                onClick = { onApply(preset) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One preset chip: a shape swatch above its name, framed and tinted while [selected]. */
@Composable
private fun PresetChip(
    label: String,
    fill: Color,
    cornerPercent: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(RoundedCornerShape(percent = cornerPercent))
                    .background(fill),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** A card with the opacity and corner-rounding sliders for one button style. */
@Composable
private fun ButtonShapeCard(
    style: MusicButtonStyle,
    onOpacityCommit: (Float) -> Unit,
    onCornerCommit: (Int) -> Unit,
) {
    // Local state so the sliders/preview react immediately; committed to prefs on release.
    var opacity by remember(style.opacity) { mutableFloatStateOf(style.opacity) }
    var corner by remember(style.cornerPercent) { mutableFloatStateOf(style.cornerPercent.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdjustableSlider(
                label = stringResource(R.string.opacity),
                valueText = "${(opacity * 100).roundToInt()}%",
                value = opacity,
                valueRange = 0f..1f,
                step = 0.05f,
                onValueChange = { opacity = it },
                onCommit = { onOpacityCommit(opacity) },
            )
            AdjustableSlider(
                label = stringResource(R.string.music_button_corners),
                valueText = "${corner.roundToInt()}%",
                value = corner,
                valueRange = MusicButtonStyle.MIN_CORNER_PERCENT.toFloat()..
                    MusicButtonStyle.MAX_CORNER_PERCENT.toFloat(),
                step = 5f,
                onValueChange = { corner = it },
                onCommit = { onCornerCommit(corner.roundToInt()) },
            )
        }
    }
}

/** A dark panel mirroring the expanded cutout, showing the three transport buttons as styled. */
@Composable
private fun MusicButtonsPreview(
    skipStyle: MusicButtonStyle,
    playPauseStyle: MusicButtonStyle,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewButton(Icons.Rounded.SkipPrevious, skipStyle.previewFill(fallback = null), skipStyle.cornerPercent)
            PreviewButton(Icons.Rounded.PlayArrow, playPauseStyle.previewFill(fallback = MusicAccent), playPauseStyle.cornerPercent)
            PreviewButton(Icons.Rounded.SkipNext, skipStyle.previewFill(fallback = null), skipStyle.cornerPercent)
        }
    }
}

/** The concrete preview fill for a button style, or null for a plain (unfilled) button. A [filled]
 *  style with no colour falls back to [MusicButtonFilledDefault], mirroring the live overlay. */
@Composable
private fun MusicButtonStyle.previewFill(fallback: Color?): Color? {
    val base = color?.resolve() ?: fallback ?: if (filled) MusicButtonFilledDefault else return null
    return base.copy(alpha = opacity)
}

@Composable
private fun PreviewButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fill: Color?,
    cornerPercent: Int,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .then(
                if (fill != null) {
                    Modifier
                        .clip(RoundedCornerShape(percent = cornerPercent))
                        .background(fill)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        val tint = when {
            fill == null -> Color(0xFFF5F5F5)
            fill.luminance() > 0.5f -> Color(0xFF0A0A0A)
            else -> Color(0xFFF5F5F5)
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    }
}
