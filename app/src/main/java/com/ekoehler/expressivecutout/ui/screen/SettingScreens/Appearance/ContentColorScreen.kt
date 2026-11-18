package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.PageTitle
import com.ekoehler.expressivecutout.ui.components.groupedShape

/**
 * "Content colours" screen (reached from the Appearance screen). Shows a sample expanded cutout and
 * one colour picker per element of it: badge container, badge content, app name, timestamp, title
 * and body text. Every picker offers a default option that keeps the notification's own look.
 */
@Composable
internal fun ContentColorScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    val previewAppName = stringResource(R.string.content_colors_preview_app_name)
    val previewTitle = stringResource(R.string.content_colors_preview_title)
    val previewBody = stringResource(R.string.content_colors_preview_body)
    val previewEvent = remember(previewAppName, previewTitle, previewBody) {
        IslandEvent(
            id = 0L,
            icon = IslandIcon.Vector(Icons.Rounded.Notifications),
            label = previewTitle,
            detail = previewBody,
            appName = previewAppName,
            postTimeMs = System.currentTimeMillis() - PREVIEW_AGE_MS,
            accent = PREVIEW_ACCENT,
        )
    }
    val defaultLabel = stringResource(R.string.content_colors_default)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PageTitle(text = stringResource(R.string.content_colors_title))

        // Expanded cutout preview, forced to show every element being coloured
        ExpandedCutoutPreview(
            event = previewEvent,
            layout = layout.expanded,
            appearance = appearance.copy(showSourceAppName = true, showTimestamp = true),
            showActions = false,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Badge container colour
        ColorPickerCard(
            label = stringResource(R.string.content_colors_badge_container),
            selected = appearance.badgeContainerColor,
            onSelect = viewModel::setBadgeContainerColor,
            defaultLabel = defaultLabel,
            shape = groupedShape(isFirst = true),
            allowAppIcon = true,
        )

        // Badge content colour
        ColorPickerCard(
            label = stringResource(R.string.content_colors_badge_content),
            selected = appearance.badgeContentColor,
            onSelect = viewModel::setBadgeContentColor,
            defaultLabel = defaultLabel,
            shape = groupedShape(),
            allowAppIcon = true,
        )

        // Application name colour
        ColorPickerCard(
            label = stringResource(R.string.content_colors_app_name),
            selected = appearance.appNameColor,
            onSelect = viewModel::setAppNameColor,
            defaultLabel = defaultLabel,
            shape = groupedShape(),
            allowAppIcon = true,
        )

        // Timestamp colour
        ColorPickerCard(
            label = stringResource(R.string.content_colors_timestamp),
            selected = appearance.timestampColor,
            onSelect = viewModel::setTimestampColor,
            defaultLabel = defaultLabel,
            shape = groupedShape(),
            allowAppIcon = true,
        )

        // Title colour
        ColorPickerCard(
            label = stringResource(R.string.content_colors_title_text),
            selected = appearance.titleColor,
            onSelect = viewModel::setTitleColor,
            defaultLabel = defaultLabel,
            shape = groupedShape(),
            allowAppIcon = true,
        )

        // Content text colour
        ColorPickerCard(
            label = stringResource(R.string.appearance_text_color),
            selected = appearance.textColor,
            onSelect = viewModel::setTextColor,
            defaultLabel = stringResource(R.string.appearance_text_color_auto),
            shape = groupedShape(isLast = true),
            allowAppIcon = true,
        )
    }
}

/** How long ago the sample notification was posted, so its header reads "4m ago". */
private const val PREVIEW_AGE_MS = 4 * 60 * 1000L
