package com.ekoehler.expressivecutout.ui.screen.tiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.data.MusicRightButtonAction
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.ExpressivePillRow
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import com.ekoehler.expressivecutout.ui.components.OptionSelectionCard
import com.ekoehler.expressivecutout.ui.components.PageTitle
import com.ekoehler.expressivecutout.ui.components.rememberRobotoFlexFamily
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import com.ekoehler.expressivecutout.ui.screen.SettingsToggleCard
import kotlinx.coroutines.selects.select
import kotlin.math.roundToInt

/** The pink tile accent, used as the play/pause default and the preview backdrop's default fill. */
private val MUSIC_ACCENT = Color(0xFFF472B6)

/** Fallback fill for a button asked to be [MusicButtonStyle.filled] before the user picks a colour. */
private val MUSIC_BUTTON_FILLED_DEFAULT = Color(0xFFE0E0E0)

/** Height of a transport button in the settings preview; the play/pause button is 16:9 off this. */
private const val PREVIEW_BUTTON_HEIGHT_DP = 48

/**
 * Settings specific to the music dynamic tile: whether to show the album art on the normal cutout,
 * whether to show playback controls (previous / play‑pause / next) on the expanded cutout, and the
 * colour, opacity and corner rounding of each control button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.musicTile.collectAsStateWithLifecycle()
    var playbackTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PageTitle(text = stringResource(R.string.tile_music))

        // Tiny player toggle
        SettingsToggleCard(
            shape = groupedShape(isFirst = true),
            title = stringResource(R.string.music_mini_player_title),
            description = stringResource(R.string.music_mini_player_desc),
            checked = settings.miniPlayer,
            onCheckedChange = viewModel::setMusicMiniPlayer,
        )

        // One transport button on the trailing edge of the normal cutout. The tiny player has no
        // room for it beside the camera, so it's only offered while that's off.
        AnimatedVisibility(visible = !settings.miniPlayer) {
            SettingsToggleCard(
                shape = groupedShape(),
                title = stringResource(R.string.music_right_button_title),
                description = stringResource(R.string.music_right_button_desc),
                checked = settings.rightButton,
                onCheckedChange = viewModel::setMusicRightButton,
            ) {
                // Which action the button carries, in the same card as the toggle that shows it.
                AnimatedVisibility(visible = settings.rightButton) {
                    ExpressivePillRow(
                        options = listOf(
                            stringResource(R.string.music_prev_label),
                            stringResource(R.string.music_playpause_button_title),
                            stringResource(R.string.music_next_label),
                        ),
                        selectedIndex = settings.rightButtonAction.ordinal,
                        onSelect = {
                            viewModel.setMusicRightButtonAction(MusicRightButtonAction.entries[it])
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        fillWidth = true,
                    )
                }
            }
        }

        // Cover/ring settings - the cover also shows in the expanded cutout, so these stay
        // available whether or not the tiny player is on.
        SettingsToggleCard(
            shape = RoundedCornerShape(4.dp),
            title = stringResource(R.string.music_show_art_title),
            description = stringResource(R.string.music_show_art_desc),
            checked = settings.showAlbumArt,
            onCheckedChange = viewModel::setMusicShowAlbumArt,
        )

        SettingsToggleCard(
            shape = groupedShape(),
            title = stringResource(R.string.music_album_background_title),
            description = stringResource(R.string.music_album_background_desc),
            checked = settings.showAlbumBackground,
            onCheckedChange = viewModel::setMusicShowAlbumBackground,
        )

        // Rotation and the ring only apply to the album cover, so they ride with its toggle.
        AnimatedVisibility(visible = settings.showAlbumArt) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                // Spin album art toggle
                SettingsToggleCard(
                    shape = groupedShape(),
                    title = stringResource(R.string.music_rotate_art_title),
                    description = stringResource(R.string.music_rotate_art_desc),
                    checked = settings.rotateAlbumArt,
                    onCheckedChange = viewModel::setMusicRotateAlbumArt,
                )

                // A spinning square would swing its corners, so spinning pins this on and the row
                // dims rather than disappearing — the cover stays round either way.
                SettingsToggleCard(
                    shape = groupedShape(),
                    title = stringResource(R.string.music_circle_cover_title),
                    description = stringResource(
                        if (settings.rotateAlbumArt) R.string.music_circle_cover_locked_desc
                        else R.string.music_circle_cover_desc
                    ),
                    checked = settings.circleCover || settings.rotateAlbumArt,
                    onCheckedChange = viewModel::setMusicCircleCover,
                    enabled = !settings.rotateAlbumArt,
                )

                // Ring around cover toggle
                SettingsToggleCard(
                    shape = groupedShape(),
                    title = stringResource(R.string.music_art_stroke_title),
                    description = stringResource(R.string.music_art_stroke_desc),
                    checked = settings.albumArtStroke,
                    onCheckedChange = viewModel::setMusicAlbumArtStroke,
                )

                // Ring color
                AnimatedVisibility(visible = settings.albumArtStroke) {
                    ColorPickerCard(
                        label = stringResource(R.string.music_art_stroke_color),
                        selected = settings.albumArtStrokeColor,
                        onSelect = viewModel::setMusicAlbumArtStrokeColor,
                        defaultLabel = stringResource(R.string.music_default_accent),
                        defaultColor = MUSIC_ACCENT,
                    )
                }
            }
        }

        // Container behind the note glyph that stands in for a missing cover. The glyph's own ink
        // flips to black on a bright container, so it stays legible whatever the user picks.
        ColorPickerCard(
            label = stringResource(R.string.music_cover_fallback_color),
            selected = settings.coverFallbackColor,
            onSelect = viewModel::setMusicCoverFallbackColor,
            defaultLabel = stringResource(R.string.music_cover_fallback_default),
            defaultColor = MUSIC_ACCENT.copy(alpha = 0.20f),
        )

        // Toggles if playing music extends the cutout or stays in normal/tiny
        SettingsToggleCard(
            shape = groupedShape(),
            title = stringResource(R.string.music_expand_on_play_title),
            description = stringResource(R.string.music_expand_on_play_desc),
            checked = settings.expandOnPlay,
            onCheckedChange = viewModel::setMusicExpandOnPlay,
        )

        // Toggles if music cutout is enabled when the music app is open
        SettingsToggleCard(
            shape = groupedShape(),
            title = stringResource(R.string.music_visible_in_player_title),
            description = stringResource(R.string.music_visible_in_player_desc),
            checked = settings.visibleInPlayerApp,
            onCheckedChange = viewModel::setMusicVisibleInPlayerApp,
        )

        // Toggle playback control (previous, play/pause, next buttons)
        SettingsToggleCard(
            shape = groupedShape(),
            title = stringResource(R.string.music_show_controls_title),
            description = stringResource(R.string.music_show_controls_desc),
            checked = settings.showControls,
            onCheckedChange = viewModel::setMusicShowControls,
        )

        // Show progressbar
        SettingsToggleCard(
            shape = groupedShape(isLast = true),
            title = stringResource(R.string.music_progress_title),
            description = stringResource(R.string.music_progress_description),
            checked = settings.showProgress,
            onCheckedChange = viewModel::setMusicShowProgress,
        )

        // Everything below styles the playback buttons; only meaningful when they're shown.
        if (settings.showControls) {
            SectionLabel(stringResource(R.string.music_buttons_title))

            Spacer(modifier = Modifier.height(12.dp))

            // Music playback control preview
            MusicButtonsPreview(
                skipStyle = settings.skipButton,
                playPauseStyle = settings.playPauseButton,
                playbackSelected = playbackTab,
                playPauseExpand = settings.playPauseExpand,
                playPauseText = settings.playPauseText,
                playPauseTextWidth = settings.playPauseTextWidth,
                previousExpand = settings.previousExpand,
                previousText = settings.previousText,
                previousTextWidth = settings.previousTextWidth,
                nextExpand = settings.nextExpand,
                nextText = settings.nextText,
                nextTextWidth = settings.nextTextWidth,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Play/pause - Prev/next selector
            ExpressiveSegmentedRow(
                options = listOf(stringResource(R.string.music_skip_buttons_title), stringResource(R.string.music_playpause_button_title)),
                selectedIndex = playbackTab,
                onSelect = { playbackTab = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent (
                targetState = playbackTab,
                label = "playbackControlSettings"
            ) { current ->
                when (current) {
                    // Previous/next buttons
                    0 -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Rounded/pill selector
                        ButtonPresetRow(
                            current = settings.skipButton,
                            sampleFill = settings.skipButton.previewFill(fallback = null)
                                ?: MUSIC_BUTTON_FILLED_DEFAULT,
                            onApply = viewModel::applyMusicSkipPreset,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Button color picker
                        ColorPickerCard(
                            label = stringResource(R.string.music_button_color),
                            selected = settings.skipButton.color,
                            onSelect = viewModel::setMusicSkipColor,
                            defaultLabel = stringResource(R.string.cd_color_default_plain),
                            defaultColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = groupedShape(isFirst = true),
                            allowTransparent = true
                        )

                        // Opacity/rounded corners settings
                        ButtonShapeCard(
                            style = settings.skipButton,
                            onOpacityCommit = viewModel::setMusicSkipOpacity,
                            onCornerCommit = viewModel::setMusicSkipCornerPercent,
                            shape = groupedShape(isLast = true)
                        )

                        // Previous button settings
                        SectionLabel(stringResource(R.string.music_prev_label))

                        SettingsToggleCard(
                            shape = groupedShape(isFirst = true, isLast = !settings.previousExpand),
                            title = stringResource(R.string.music_prev_expand_title),
                            description = stringResource(R.string.music_prev_expand_desc),
                            checked = settings.previousExpand,
                            onCheckedChange = viewModel::setMusicPreviousExpand,
                        )

                        AnimatedVisibility(visible = settings.previousExpand) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SettingsToggleCard(
                                    shape = groupedShape(isLast = !settings.previousText),
                                    title = stringResource(R.string.music_prev_text_title),
                                    description = stringResource(R.string.music_prev_text_desc),
                                    checked = settings.previousText,
                                    onCheckedChange = viewModel::setMusicPreviousText,
                                )

                                // The width axis only bites once the label replaces the icon.
                                AnimatedVisibility(visible = settings.previousText) {
                                    TextWidthCard(
                                        width = settings.previousTextWidth,
                                        onCommit = viewModel::setMusicPreviousTextWidth,
                                    )
                                }
                            }
                        }

                        // Next button settings
                        SectionLabel(stringResource(R.string.music_next_label))

                        SettingsToggleCard(
                            shape = groupedShape(isFirst = true, isLast = !settings.nextExpand),
                            title = stringResource(R.string.music_next_expand_title),
                            description = stringResource(R.string.music_next_expand_desc),
                            checked = settings.nextExpand,
                            onCheckedChange = viewModel::setMusicNextExpand,
                        )

                        AnimatedVisibility(visible = settings.nextExpand) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SettingsToggleCard(
                                    shape = groupedShape(isLast = !settings.nextText),
                                    title = stringResource(R.string.music_next_text_title),
                                    description = stringResource(R.string.music_next_text_desc),
                                    checked = settings.nextText,
                                    onCheckedChange = viewModel::setMusicNextText,
                                )

                                // The width axis only bites once the label replaces the icon.
                                AnimatedVisibility(visible = settings.nextText) {
                                    TextWidthCard(
                                        width = settings.nextTextWidth,
                                        onCommit = viewModel::setMusicNextTextWidth,
                                    )
                                }
                            }
                        }
                    }

                    // Play/pause buttons
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Rounded/pill selector
                        ButtonPresetRow(
                            current = settings.playPauseButton,
                            sampleFill = settings.playPauseButton.previewFill(fallback = MUSIC_ACCENT)
                                ?: MUSIC_ACCENT,
                            onApply = viewModel::applyMusicPlayPausePreset,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Button color
                        ColorPickerCard(
                            label = stringResource(R.string.music_button_color),
                            selected = settings.playPauseButton.color,
                            onSelect = viewModel::setMusicPlayPauseColor,
                            defaultLabel = stringResource(R.string.music_default_accent),
                            defaultColor = MUSIC_ACCENT,
                            allowTransparent = true,
                            shape = groupedShape(isFirst = true)
                        )

                        // Opacity/rounded corner settings
                        ButtonShapeCard(
                            style = settings.playPauseButton,
                            onOpacityCommit = viewModel::setMusicPlayPauseOpacity,
                            onCornerCommit = viewModel::setMusicPlayPauseCornerPercent,
                        )

                        // Stretch the button across the width the skip buttons leave over
                        SettingsToggleCard(
                            shape = groupedShape(isLast = !settings.playPauseExpand),
                            title = stringResource(R.string.music_playpause_expand_title),
                            description = stringResource(R.string.music_playpause_expand_desc),
                            checked = settings.playPauseExpand,
                            onCheckedChange = viewModel::setMusicPlayPauseExpand,
                        )

                        // A label only fits once the button is expanded, so it rides with that toggle.
                        AnimatedVisibility(visible = settings.playPauseExpand) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SettingsToggleCard(
                                    shape = groupedShape(isLast = !settings.playPauseText),
                                    title = stringResource(R.string.music_playpause_text_title),
                                    description = stringResource(R.string.music_playpause_text_desc),
                                    checked = settings.playPauseText,
                                    onCheckedChange = viewModel::setMusicPlayPauseText,
                                )

                                // The width axis only bites once the label replaces the icon.
                                AnimatedVisibility(visible = settings.playPauseText) {
                                    TextWidthCard(
                                        width = settings.playPauseTextWidth,
                                        onCommit = viewModel::setMusicPlayPauseTextWidth,
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
    shape: RoundedCornerShape = groupedShape()
) {
    // Local state so the sliders/preview react immediately; committed to prefs on release.
    var opacity by remember(style.opacity) { mutableFloatStateOf(style.opacity) }
    var corner by remember(style.cornerPercent) { mutableFloatStateOf(style.cornerPercent.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
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

/**
 * A card holding one button label's Roboto Flex `wdth` slider — 25 condenses the label, 151 stretches
 * it. The value is local while dragging and committed to prefs on release, like [ButtonShapeCard].
 */
@Composable
private fun TextWidthCard(
    width: Float,
    onCommit: (Float) -> Unit,
    shape: RoundedCornerShape = groupedShape(isLast = true),
) {
    var current by remember(width) { mutableFloatStateOf(width) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            AdjustableSlider(
                label = stringResource(R.string.music_text_width),
                valueText = "${current.roundToInt()}",
                value = current,
                valueRange = MusicTileSettings.MIN_TEXT_WIDTH..MusicTileSettings.MAX_TEXT_WIDTH,
                step = 1f,
                onValueChange = { current = it },
                onCommit = { onCommit(current) },
            )
        }
    }
}

/** A dark panel mirroring the expanded cutout, showing the three transport buttons as styled. */
@Composable
private fun MusicButtonsPreview(
    skipStyle: MusicButtonStyle,
    playPauseStyle: MusicButtonStyle,
    // 0 if prev/next, 1 if play/pause
    playbackSelected: Int,
    playPauseExpand: Boolean,
    playPauseText: Boolean,
    playPauseTextWidth: Float,
    previousExpand: Boolean,
    previousText: Boolean,
    previousTextWidth: Float,
    nextExpand: Boolean,
    nextText: Boolean,
    nextTextWidth: Float,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewButton(
                Icons.Rounded.SkipPrevious,
                skipStyle.previewFill(fallback = null),
                skipStyle.cornerPercent,
                selected = playbackSelected == 0,
                label = stringResource(R.string.music_prev_label).uppercase()
                    .takeIf { previousExpand && previousText },
                labelWidth = previousTextWidth,
                expand = previousExpand,
                modifier = if (previousExpand) Modifier.weight(1f) else Modifier,
            )

            // The play/pause button is a 16:9 rectangle, matching the live overlay, unless it's
            // been asked to expand — then it takes the width the skip buttons leave over.
            PreviewButton(
                Icons.Rounded.PlayArrow,
                playPauseStyle.previewFill(fallback = MUSIC_ACCENT),
                playPauseStyle.cornerPercent,
                widthDp = PREVIEW_BUTTON_HEIGHT_DP * 16 / 9,
                selected = playbackSelected == 1,
                label = stringResource(R.string.music_playpause_label_play).uppercase()
                    .takeIf { playPauseExpand && playPauseText },
                labelWidth = playPauseTextWidth,
                expand = playPauseExpand,
                modifier = if (playPauseExpand) Modifier.weight(1f) else Modifier,
            )

            PreviewButton(
                Icons.Rounded.SkipNext,
                skipStyle.previewFill(fallback = null),
                skipStyle.cornerPercent,
                selected = playbackSelected == 0,
                label = stringResource(R.string.music_next_label).uppercase()
                    .takeIf { nextExpand && nextText },
                labelWidth = nextTextWidth,
                expand = nextExpand,
                modifier = if (nextExpand) Modifier.weight(1f) else Modifier,
            )
        }
    }
}

/** The concrete preview fill for a button style, or null for a plain (unfilled) button. A [filled]
 *  style with no colour falls back to [MUSIC_BUTTON_FILLED_DEFAULT], mirroring the live overlay. */
@Composable
private fun MusicButtonStyle.previewFill(fallback: Color?): Color? {
    val base = color?.resolve() ?: fallback ?: if (filled) MUSIC_BUTTON_FILLED_DEFAULT else return null
    return base.copy(alpha = opacity)
}

@Composable
private fun PreviewButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fill: Color?,
    cornerPercent: Int,
    widthDp: Int = PREVIEW_BUTTON_HEIGHT_DP,
    selected: Boolean = false,
    label: String? = null,
    labelWidth: Float = MusicTileSettings.DEFAULT_TEXT_WIDTH,
    expand: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Actual button
        Box(
            modifier = Modifier
                .then(
                    if (expand) {
                        Modifier.fillMaxWidth().height(PREVIEW_BUTTON_HEIGHT_DP.dp)
                    } else {
                        Modifier.size(width = widthDp.dp, height = PREVIEW_BUTTON_HEIGHT_DP.dp)
                    }
                )
                .then(
                    if (fill != null) {
                        Modifier
                            // Corner radius keyed to height so a wide button reads as a clean pill at
                            // 50% rather than an ellipse, matching the live overlay.
                            .clip(RoundedCornerShape((PREVIEW_BUTTON_HEIGHT_DP * cornerPercent / 100f).dp))
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
            if (label != null) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = rememberRobotoFlexFamily(labelWidth),
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    maxLines = 1,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // Selected dot
        Box(
            modifier = Modifier.height(6.dp)
                .width(12.dp)
                .clip(shape = CircleShape)
                .background(color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}
