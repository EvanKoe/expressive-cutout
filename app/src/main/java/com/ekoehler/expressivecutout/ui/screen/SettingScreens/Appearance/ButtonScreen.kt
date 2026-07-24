package com.ekoehler.expressivecutout.ui.screen

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.overlay.IslandAction
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.expandedActionsExtraDp
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
import kotlin.math.roundToInt

/** Accent used by the preview event, matching the accent shown on the sibling settings screens. */
private val PreviewAccent = Color(0xFF60A5FA)

/**
 * "Action buttons" screen (reached from the Appearance screen). Configures the chips and inline
 * reply field shown in the expanded cutout: whether they appear at all, the chip style/colour/height,
 * the reply field style, and whether the cancel button sits on the leading edge. Every control feeds
 * a live preview at the top so the effect of each choice is visible immediately.
 */
@Composable
internal fun ButtonScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    var previewDark by remember { mutableStateOf(systemInDark) }
    // Local height so the sliders/preview react immediately; committed to prefs on release.
    var buttonHeight by remember(appearance.actionButtonHeightDp) {
        mutableStateOf(appearance.actionButtonHeightDp.toFloat())
    }

    // The preview reflects the in-flight height before it is persisted.
    val previewAppearance = appearance.copy(actionButtonHeightDp = buttonHeight.roundToInt())

    val context = LocalContext.current
    val previewLabel = stringResource(R.string.preview_label)
    val previewDetail = stringResource(R.string.preview_detail)
    val replyLabel = stringResource(R.string.action_buttons_preview_reply)
    val archiveLabel = stringResource(R.string.action_buttons_preview_archive)
    val previewEvent = remember(previewLabel, previewDetail, replyLabel, archiveLabel) {
        // A harmless, never-fired intent so the preview chips have the PendingIntent they require.
        val noop = PendingIntent.getActivity(
            context, 0, Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        IslandEvent(
            id = 0L,
            icon = IslandIcon.Vector(Icons.Rounded.Notifications),
            label = previewLabel,
            detail = previewDetail,
            accent = PreviewAccent,
            actions = listOf(
                IslandAction(label = replyLabel, intent = noop),
                IslandAction(label = archiveLabel, intent = noop),
            ),
        )
    }
    val cutout = rememberTopCutout()
    val expanded = layout.expanded
    // Mirror the real island: it grows by the chip row's height so the chips clear the camera hole —
    // but only when the chips are actually shown, matching the toggle below.
    val previewHeightDp = expanded.heightDp +
        if (behaviour.showActionButtons) expandedActionsExtraDp(buttonHeight.roundToInt()) else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.appearance_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalIconButton(onClick = { previewDark = !previewDark }) {
                Icon(
                    imageVector = if (previewDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = stringResource(R.string.cd_toggle_preview_theme),
                )
            }
        }

        IslandPreviewPanel(
            background = if (previewDark) Color(0xFF0B0B0C) else Color(0xFFEDEFF3),
            cutout = cutout,
            widthPercent = expanded.widthPercent,
            heightDp = previewHeightDp,
            cornerTopLeftDp = expanded.cornerTopLeftDp,
            cornerTopRightDp = expanded.cornerTopRightDp,
            cornerBottomLeftDp = expanded.cornerBottomLeftDp,
            cornerBottomRightDp = expanded.cornerBottomRightDp,
            offsetXDp = expanded.offsetXDp,
            offsetYDp = expanded.offsetYDp,
            expanded = true,
            event = previewEvent,
            appearance = previewAppearance,
            showActions = behaviour.showActionButtons,
        )

        // Whether the chips appear at all lives with the other behaviour toggles, but it is the
        // natural on/off switch for this screen, so it leads here too.
        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.action_buttons_enable_title),
            description = stringResource(R.string.action_buttons_enable_desc),
            checked = behaviour.showActionButtons,
            onCheckedChange = viewModel::setShowActionButtons,
        )

        // --- Chip style ---
        OptionGroupCard(title = stringResource(R.string.action_buttons_style_title)) {
            ButtonStyleOption(
                title = stringResource(R.string.style_expressive_tonal_title),
                description = stringResource(R.string.style_expressive_tonal_desc),
                style = ActionButtonStyle.EXPRESSIVE_TONAL,
                selected = appearance.actionButtonStyle,
                onSelect = viewModel::setActionButtonStyle,
            )
            ButtonStyleOption(
                title = stringResource(R.string.style_expressive_filled_title),
                description = stringResource(R.string.style_expressive_filled_desc),
                style = ActionButtonStyle.EXPRESSIVE_FILLED,
                selected = appearance.actionButtonStyle,
                onSelect = viewModel::setActionButtonStyle,
            )
            ButtonStyleOption(
                title = stringResource(R.string.style_material_you_title),
                description = stringResource(R.string.style_material_you_desc),
                style = ActionButtonStyle.MATERIAL_YOU,
                selected = appearance.actionButtonStyle,
                onSelect = viewModel::setActionButtonStyle,
            )
            ButtonStyleOption(
                title = stringResource(R.string.style_outlined_title),
                description = stringResource(R.string.style_outlined_desc),
                style = ActionButtonStyle.OUTLINED,
                selected = appearance.actionButtonStyle,
                onSelect = viewModel::setActionButtonStyle,
            )
        }

        // --- Chip colour (Material scheme accents + custom) ---
        ActionButtonColorCard(
            selected = appearance.actionButtonColor,
            onSelect = viewModel::setActionButtonColor,
        )

        // --- Chip height ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                AdjustableSlider(
                    label = stringResource(R.string.action_buttons_height_title),
                    valueText = "${buttonHeight.roundToInt()} dp",
                    value = buttonHeight,
                    valueRange = AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP.toFloat()..
                        AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP.toFloat(),
                    step = 2f,
                    onValueChange = { buttonHeight = it },
                    onCommit = { viewModel.setActionButtonHeight(buttonHeight.roundToInt()) },
                )
            }
        }

        // --- Reply field style ---
        OptionGroupCard(title = stringResource(R.string.action_buttons_input_style_title)) {
            ReplyInputPreview(
                inputStyle = appearance.replyInputStyle,
                cancelOnLeft = appearance.cancelButtonOnLeft,
                heightDp = buttonHeight.roundToInt(),
            )
            Spacer(Modifier.height(4.dp))
            ReplyStyleOption(
                title = stringResource(R.string.input_expressive_title),
                description = stringResource(R.string.input_expressive_desc),
                style = ReplyInputStyle.EXPRESSIVE,
                selected = appearance.replyInputStyle,
                onSelect = viewModel::setReplyInputStyle,
            )
            ReplyStyleOption(
                title = stringResource(R.string.input_material_you_title),
                description = stringResource(R.string.input_material_you_desc),
                style = ReplyInputStyle.MATERIAL_YOU,
                selected = appearance.replyInputStyle,
                onSelect = viewModel::setReplyInputStyle,
            )
            ReplyStyleOption(
                title = stringResource(R.string.input_material_2_title),
                description = stringResource(R.string.input_material_2_desc),
                style = ReplyInputStyle.MATERIAL_2,
                selected = appearance.replyInputStyle,
                onSelect = viewModel::setReplyInputStyle,
            )
            ReplyStyleOption(
                title = stringResource(R.string.input_segmented_title),
                description = stringResource(R.string.input_segmented_desc),
                style = ReplyInputStyle.SEGMENTED,
                selected = appearance.replyInputStyle,
                onSelect = viewModel::setReplyInputStyle,
            )
        }

        // --- Cancel button placement ---
        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.action_buttons_cancel_left_title),
            description = stringResource(R.string.action_buttons_cancel_left_desc),
            checked = appearance.cancelButtonOnLeft,
            onCheckedChange = viewModel::setCancelButtonOnLeft,
        )

        // --- Send / cancel reply-button colours ---
        // Their colours default to the notification's accent (send) and a neutral tint (cancel);
        // the leading "default" swatch restores that behaviour.
        ColorPickerCard(
            label = stringResource(R.string.appearance_send_color),
            selected = appearance.sendButtonColor,
            onSelect = viewModel::setSendButtonColor,
            defaultLabel = stringResource(R.string.cd_color_default_accent),
            defaultColor = PreviewAccent,
        )
        ColorPickerCard(
            label = stringResource(R.string.appearance_cancel_color),
            selected = appearance.cancelButtonColor,
            onSelect = viewModel::setCancelButtonColor,
            defaultLabel = stringResource(R.string.cd_color_default_neutral),
            defaultColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A titled surface card that stacks a set of selectable option rows. */
@Composable
private fun OptionGroupCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ButtonStyleOption(
    title: String,
    description: String,
    style: ActionButtonStyle,
    selected: ActionButtonStyle,
    onSelect: (ActionButtonStyle) -> Unit,
) = OptionRow(
    title = title,
    description = description,
    selected = style == selected,
    onClick = { onSelect(style) },
)

@Composable
private fun ReplyStyleOption(
    title: String,
    description: String,
    style: ReplyInputStyle,
    selected: ReplyInputStyle,
    onSelect: (ReplyInputStyle) -> Unit,
) = OptionRow(
    title = title,
    description = description,
    selected = style == selected,
    onClick = { onSelect(style) },
)

/** A single-choice row: title, supporting text and a trailing radio, the whole row tappable. */
@Composable
private fun OptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        RadioButton(selected = selected, onClick = onClick)
    }
}

/**
 * The chip-colour card. A null selection follows the notification's own accent (the default);
 * otherwise the chosen colour is [CutoutColor.Dynamic] (Material You), one of the current Material
 * colour-scheme roles, or a custom pick from the wheel.
 */
@Composable
private fun ActionButtonColorCard(
    selected: CutoutColor?,
    onSelect: (CutoutColor?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    // The Material scheme roles offered as fixed swatches. Resolved to concrete colours now so the
    // chips stay put even if the wallpaper accent shifts later.
    val schemeColors = listOf(
        scheme.primary to stringResource(R.string.cd_color_primary),
        scheme.secondary to stringResource(R.string.cd_color_secondary),
        scheme.tertiary to stringResource(R.string.cd_color_tertiary),
        scheme.error to stringResource(R.string.cd_color_error),
    )
    val schemeArgbs = schemeColors.map { it.first.toArgb().toLong() and 0xFFFFFFFFL }
    // A Solid that matches neither the dynamic accent nor a scheme role is the user's own pick.
    val solidArgb = (selected as? CutoutColor.Solid)?.argb
    val customArgb = solidArgb?.takeIf { argb -> schemeArgbs.none { it == argb } }
    val currentColor = selected?.resolve() ?: PreviewAccent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.action_buttons_color_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    // Breathing room so the selected swatch's enlarged ring isn't clipped at the edges.
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Follow the notification's accent (the historical default).
                ColorSwatch(
                    color = PreviewAccent,
                    selected = selected == null,
                    badge = Icons.Rounded.RestartAlt,
                    badgeDescription = stringResource(R.string.cd_color_default_accent),
                    onClick = { onSelect(null) },
                )
                // Material You dynamic accent (follows the wallpaper on Android 12+).
                ColorSwatch(
                    color = CutoutColor.Dynamic.resolve(),
                    selected = selected is CutoutColor.Dynamic,
                    badge = Icons.Rounded.AutoAwesome,
                    badgeDescription = stringResource(R.string.cd_color_dynamic),
                    onClick = { onSelect(CutoutColor.Dynamic) },
                )
                // Custom wheel pick.
                CustomColorSwatch(
                    selectedColor = customArgb?.let { Color(it) },
                    onClick = { showPicker = true },
                )
                // The Material colour-scheme roles.
                schemeColors.forEachIndexed { index, (color, _) ->
                    val argb = schemeArgbs[index]
                    ColorSwatch(
                        color = color,
                        selected = selected == CutoutColor.Solid(argb),
                        onClick = { onSelect(CutoutColor.Solid(argb)) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = currentColor,
            onConfirm = { picked ->
                showPicker = false
                onSelect(CutoutColor.Solid(picked.toArgb().toLong() and 0xFFFFFFFFL))
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * An in-app rendition of the inline reply row, so the effect of the field style, height and the
 * cancel-button placement is visible without opening a notification. Uses app-theme colours rather
 * than the island's own palette; it only mirrors the shapes and layout.
 */
@Composable
private fun ReplyInputPreview(
    inputStyle: ReplyInputStyle,
    cancelOnLeft: Boolean,
    heightDp: Int,
) {
    val segmented = inputStyle == ReplyInputStyle.SEGMENTED
    val cap = (heightDp / 2).dp
    val inner = 8.dp
    val startCap = RoundedCornerShape(topStart = cap, bottomStart = cap, topEnd = inner, bottomEnd = inner)
    val endCap = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = cap, bottomEnd = cap)
    val fieldShape: Shape = when (inputStyle) {
        ReplyInputStyle.EXPRESSIVE -> CircleShape
        ReplyInputStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        ReplyInputStyle.MATERIAL_2 -> RoundedCornerShape(4.dp)
        // In the segmented bar the field's inner edges are lightly rounded; its leading edge caps
        // the bar when the cancel button isn't on the left.
        ReplyInputStyle.SEGMENTED -> if (cancelOnLeft) RoundedCornerShape(inner) else startCap
    }
    val cancel = @Composable {
        Box(
            modifier = Modifier
                .size(heightDp.dp)
                .clip(if (segmented && cancelOnLeft) startCap else if (segmented) RoundedCornerShape(inner) else CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (segmented) 4.dp else 10.dp),
    ) {
        if (cancelOnLeft) cancel()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(heightDp.dp)
                .clip(fieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.action_buttons_reply_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!cancelOnLeft) cancel()
        val send = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(heightDp.dp)
                .clip(if (segmented) endCap else CircleShape)
                .background(send),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint = if (send.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
