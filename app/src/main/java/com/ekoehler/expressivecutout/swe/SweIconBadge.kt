package com.ekoehler.expressivecutout.swe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.badgeIconSizeFor
import com.ekoehler.expressivecutout.overlay.badgeSizeFor
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import kotlin.math.roundToInt

/** The background's corner radius setting, in percent of its shorter side; 50 % is a full pill and the default. */
internal const val MAX_BADGE_CORNER_PERCENT = 50
internal const val DEFAULT_BADGE_CORNER_PERCENT = MAX_BADGE_CORNER_PERCENT

/** The background's width setting, in percent of the badge size; 100 % is a square. */
internal const val DEFAULT_BADGE_WIDTH_PERCENT = 100
internal const val MIN_BADGE_WIDTH_PERCENT = 100
internal const val MAX_BADGE_WIDTH_PERCENT = 250

/**
 * The optional rounded background behind an icon widget. Draws nothing extra when
 * [backgroundColor] is null; otherwise passes the resolved fill to [content] so it can pick a tint.
 */
@Composable
internal fun SweIconBadge(
    backgroundColor: CutoutColor?,
    backgroundWidthPercent: Int,
    backgroundCornerPercent: Int,
    heightDp: Int,
    content: @Composable (fill: Color?) -> Unit,
) {
    val fill = backgroundColor?.resolve()
    SweBackground(
        fill = fill,
        width = sweIconBadgeWidthDp(fill != null, backgroundWidthPercent, heightDp).dp,
        heightDp = heightDp,
        cornerPercent = backgroundCornerPercent,
    ) {
        content(fill)
    }
}

/** The widget's width in dp: the background's when one is drawn, else the bare icon's. Never depends on live data. */
internal fun sweIconBadgeWidthDp(hasBackground: Boolean, backgroundWidthPercent: Int, heightDp: Int): Float =
    if (hasBackground) sweIconBadgeBackgroundWidthDp(backgroundWidthPercent, heightDp) else badgeIconSizeFor(heightDp).value

/** The background's width in dp: the badge size scaled by the width setting. */
private fun sweIconBadgeBackgroundWidthDp(backgroundWidthPercent: Int, heightDp: Int): Float =
    badgeSizeFor(heightDp).value * backgroundWidthPercent / PERCENT

/** The icon colour and background colour pickers, and the background's width and corner radius when it is shown. */
@Composable
internal fun SweIconBadgeControls(
    iconColor: CutoutColor?,
    backgroundColor: CutoutColor?,
    backgroundWidthPercent: Int,
    backgroundCornerPercent: Int,
    onIconColor: (CutoutColor?) -> Unit,
    onBackgroundColor: (CutoutColor?) -> Unit,
    onWidth: (Int) -> Unit,
    onCorner: (Int) -> Unit,
) {
    val hasBackground = backgroundColor != null
    val isGrouped = LocalSweGroupPosition.current != SweGroupPosition.SINGLE
    var widthValue by remember(backgroundWidthPercent) { mutableFloatStateOf(backgroundWidthPercent.toFloat()) }
    var cornerValue by remember(backgroundCornerPercent) { mutableFloatStateOf(backgroundCornerPercent.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // Icon colour
        ColorPickerCard(
            label = stringResource(R.string.widgets_soundMode_icon_color),
            selected = iconColor,
            onSelect = onIconColor,
            defaultLabel = stringResource(R.string.label_default),
            defaultColor = MaterialTheme.colorScheme.onSurface,
            shape = groupedShape(isFirst = true),
        )

        // Background colour
        ColorPickerCard(
            label = stringResource(R.string.widgets_soundMode_background_color),
            selected = backgroundColor,
            onSelect = onBackgroundColor,
            defaultLabel = stringResource(R.string.empty_info_unread_shape_none),
            defaultColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = groupedShape(isLast = !hasBackground),
        )

        // Background width and corner radius, only when a background is drawn
        AnimatedVisibility(visible = hasBackground) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedShape(isLast = true),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdjustableSlider(
                        label = stringResource(R.string.width),
                        valueText = "${widthValue.roundToInt()}%",
                        value = widthValue,
                        valueRange = MIN_BADGE_WIDTH_PERCENT.toFloat()..MAX_BADGE_WIDTH_PERCENT.toFloat(),
                        step = 5f,
                        onValueChange = { widthValue = it },
                        onCommit = { onWidth(widthValue.roundToInt()) },
                    )
                    AdjustableSlider(
                        label = stringResource(R.string.appearance_corner),
                        valueText = "${cornerValue.roundToInt()}%",
                        value = cornerValue,
                        valueRange = 0f..MAX_BADGE_CORNER_PERCENT.toFloat(),
                        step = 1f,
                        onValueChange = { cornerValue = it },
                        onCommit = { onCorner(cornerValue.roundToInt()) },
                        enabled = !isGrouped,
                    )

                    // Why the corner slider is off
                    if (isGrouped) {
                        Text(
                            text = stringResource(R.string.widgets_background_corner_auto),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
