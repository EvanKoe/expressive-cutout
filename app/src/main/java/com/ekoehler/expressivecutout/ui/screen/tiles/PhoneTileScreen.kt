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
import com.ekoehler.expressivecutout.ui.screen.SettingsToggleCard

/**
 * Settings for the phone dynamic tile: whether to show the caller's contact photo, the ticking call
 * duration, and the call's action buttons. All the call content itself is read live from the
 * dialer's ongoing-call notification, so there is nothing to style here beyond these toggles.
 */
@Composable
internal fun PhoneTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.phoneTile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            title = stringResource(R.string.phone_show_photo_title),
            description = stringResource(R.string.phone_show_photo_desc),
            checked = settings.showPhoto,
            onCheckedChange = viewModel::setPhoneShowPhoto,
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(4.dp),
            title = stringResource(R.string.phone_show_duration_title),
            description = stringResource(R.string.phone_show_duration_desc),
            checked = settings.showDuration,
            onCheckedChange = viewModel::setPhoneShowDuration,
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
            title = stringResource(R.string.phone_show_actions_title),
            description = stringResource(R.string.phone_show_actions_desc),
            checked = settings.showActions,
            onCheckedChange = viewModel::setPhoneShowActions,
        )
        Text(
            text = stringResource(R.string.phone_tile_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}
