package com.ekoehler.expressivecutout.ui.screen

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.ActionButtonAlignment
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.data.SentAlignment
import com.ekoehler.expressivecutout.overlay.IslandAction
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.expandedActionsExtraDp
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.OptionSelectionCard
import com.ekoehler.expressivecutout.ui.components.SelectableOption
import kotlin.math.roundToInt

/** Label and supporting line shown for each chip style in its options card. */
private val ActionButtonStyle.titleRes: Int
    get() = when (this) {
        ActionButtonStyle.EXPRESSIVE_TONAL -> R.string.style_expressive_tonal_title
        ActionButtonStyle.EXPRESSIVE_FILLED -> R.string.style_expressive_filled_title
        ActionButtonStyle.MATERIAL_YOU -> R.string.style_material_you_title
        ActionButtonStyle.OUTLINED -> R.string.style_outlined_title
    }

private val ActionButtonStyle.descriptionRes: Int
    get() = when (this) {
        ActionButtonStyle.EXPRESSIVE_TONAL -> R.string.style_expressive_tonal_desc
        ActionButtonStyle.EXPRESSIVE_FILLED -> R.string.style_expressive_filled_desc
        ActionButtonStyle.MATERIAL_YOU -> R.string.style_material_you_desc
        ActionButtonStyle.OUTLINED -> R.string.style_outlined_desc
    }

/** Label and supporting line shown for each chip alignment in its options card. */
private val ActionButtonAlignment.titleRes: Int
    get() = when (this) {
        ActionButtonAlignment.LEFT -> R.string.action_buttons_align_left_title
        ActionButtonAlignment.CENTER -> R.string.action_buttons_align_center_title
        ActionButtonAlignment.RIGHT -> R.string.action_buttons_align_right_title
        ActionButtonAlignment.FULL -> R.string.action_buttons_align_full_title
    }

private val ActionButtonAlignment.descriptionRes: Int
    get() = when (this) {
        ActionButtonAlignment.LEFT -> R.string.action_buttons_align_left_desc
        ActionButtonAlignment.CENTER -> R.string.action_buttons_align_center_desc
        ActionButtonAlignment.RIGHT -> R.string.action_buttons_align_right_desc
        ActionButtonAlignment.FULL -> R.string.action_buttons_align_full_desc
    }

/** Label and supporting line shown for each reply-field style in its options card. */
private val ReplyInputStyle.titleRes: Int
    get() = when (this) {
        ReplyInputStyle.EXPRESSIVE -> R.string.input_expressive_title
        ReplyInputStyle.MATERIAL_YOU -> R.string.input_material_you_title
        ReplyInputStyle.MATERIAL_2 -> R.string.input_material_2_title
        ReplyInputStyle.SEGMENTED -> R.string.input_segmented_title
    }

private val ReplyInputStyle.descriptionRes: Int
    get() = when (this) {
        ReplyInputStyle.EXPRESSIVE -> R.string.input_expressive_desc
        ReplyInputStyle.MATERIAL_YOU -> R.string.input_material_you_desc
        ReplyInputStyle.MATERIAL_2 -> R.string.input_material_2_desc
        ReplyInputStyle.SEGMENTED -> R.string.input_segmented_desc
    }

/** Label and supporting line shown for each "Sent" confirmation position in its options card. */
private val SentAlignment.titleRes: Int
    get() = when (this) {
        SentAlignment.LEFT -> R.string.action_buttons_align_left_title
        SentAlignment.CENTER -> R.string.action_buttons_align_center_title
        SentAlignment.RIGHT -> R.string.action_buttons_align_right_title
    }

private val SentAlignment.descriptionRes: Int
    get() = when (this) {
        SentAlignment.LEFT -> R.string.action_buttons_sent_left_desc
        SentAlignment.CENTER -> R.string.action_buttons_sent_center_desc
        SentAlignment.RIGHT -> R.string.action_buttons_sent_right_desc
    }

/** Accent used by the preview event, matching the accent shown on the sibling settings screens. */
private val PREVIEW_ACCENT = Color(0xFF60A5FA)

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
            accent = PREVIEW_ACCENT,
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
            topMarginDp = expanded.topMarginDp,
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

        // Whether tapping an action button confirms with a toast.
        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.action_buttons_toast_title),
            description = stringResource(R.string.action_buttons_toast_desc),
            checked = behaviour.toastOnAction,
            onCheckedChange = viewModel::setToastOnAction,
        )

        // --- Chip style ---
        OptionSelectionCard(
            title = stringResource(R.string.action_buttons_style_title),
            options = ActionButtonStyle.entries.map { style ->
                SelectableOption(
                    value = style,
                    title = stringResource(style.titleRes),
                    description = stringResource(style.descriptionRes),
                )
            },
            selectedValue = appearance.actionButtonStyle,
            onSelectionChange = viewModel::setActionButtonStyle,
        )

        // --- Chip colour (dynamic roles, custom, presets) ---
        // A null selection follows the notification's own accent (the historical default).
        ColorPickerCard(
            label = stringResource(R.string.action_buttons_color_title),
            selected = appearance.actionButtonColor,
            onSelect = viewModel::setActionButtonColor,
            defaultLabel = stringResource(R.string.cd_color_default_accent),
            defaultColor = PREVIEW_ACCENT,
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

        // --- Chip alignment ---
        OptionSelectionCard(
            title = stringResource(R.string.action_buttons_alignment_title),
            options = ActionButtonAlignment.entries.map { alignment ->
                SelectableOption(
                    value = alignment,
                    title = stringResource(alignment.titleRes),
                    description = stringResource(alignment.descriptionRes),
                )
            },
            selectedValue = appearance.actionButtonAlignment,
            onSelectionChange = viewModel::setActionButtonAlignment,
        )

        // --- Reply field style ---
        OptionSelectionCard(
            title = stringResource(R.string.action_buttons_input_style_title),
            options = ReplyInputStyle.entries.map { style ->
                SelectableOption(
                    value = style,
                    title = stringResource(style.titleRes),
                    description = stringResource(style.descriptionRes),
                )
            },
            selectedValue = appearance.replyInputStyle,
            onSelectionChange = viewModel::setReplyInputStyle,
            header = {
                ReplyInputPreview(
                    inputStyle = appearance.replyInputStyle,
                    cancelOnLeft = appearance.cancelButtonOnLeft,
                    heightDp = buttonHeight.roundToInt(),
                )
            },
        )

        // --- Cancel button placement ---
        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.action_buttons_cancel_left_title),
            description = stringResource(R.string.action_buttons_cancel_left_desc),
            checked = appearance.cancelButtonOnLeft,
            onCheckedChange = viewModel::setCancelButtonOnLeft,
        )

        // --- "Sent" confirmation placement ---
        OptionSelectionCard(
            title = stringResource(R.string.action_buttons_sent_alignment_title),
            options = SentAlignment.entries.map { alignment ->
                SelectableOption(
                    value = alignment,
                    title = stringResource(alignment.titleRes),
                    description = stringResource(alignment.descriptionRes),
                )
            },
            selectedValue = appearance.sentAlignment,
            onSelectionChange = viewModel::setSentAlignment,
        )

        // --- Send / cancel reply-button colours ---
        // Their colours default to the notification's accent (send) and a neutral tint (cancel);
        // the leading "default" swatch restores that behaviour.
        ColorPickerCard(
            label = stringResource(R.string.appearance_send_color),
            selected = appearance.sendButtonColor,
            onSelect = viewModel::setSendButtonColor,
            defaultLabel = stringResource(R.string.cd_color_default_accent),
            defaultColor = PREVIEW_ACCENT,
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
