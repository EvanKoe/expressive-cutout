package com.ekoehler.expressivecutout.ui.screen.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.screen.ColorPickerCard
import com.ekoehler.expressivecutout.ui.screen.SettingsToggleCard

/**
 * Settings for the timer dynamic tile: whether to show the Reset / Add 1 min buttons and their
 * colours. The countdown itself is read live from the clock app's ongoing timer notification, so
 * there is nothing to style here beyond these toggles.
 */
@Composable
internal fun TimerTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.timerTile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(32.dp),
            title = stringResource(R.string.timer_show_actions_title),
            description = stringResource(R.string.timer_show_actions_desc),
            checked = settings.showActions,
            onCheckedChange = viewModel::setTimerShowActions,
        )

        // Button colours only matter when the action buttons are shown.
        if (settings.showActions) {
            Text(
                text = stringResource(R.string.timer_button_colours_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )
            ColorPickerCard(
                label = stringResource(R.string.timer_reset_color_label),
                selected = settings.resetColor,
                onSelect = { it?.let(viewModel::setTimerResetColor) },
            )
            ColorPickerCard(
                label = stringResource(R.string.timer_add_color_label),
                selected = settings.addButtonColor,
                onSelect = { it?.let(viewModel::setTimerAddButtonColor) },
            )
        }

        Text(
            text = stringResource(R.string.timer_tile_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}
