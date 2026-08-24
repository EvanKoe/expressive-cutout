package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.PermissionDotPosition
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow

/**
 * "Permission dot" detail screen, reached from the switch on the Shizuku options list. Holds which
 * end of the collapsed pill the dots sit on, plus one switch per watched resource.
 *
 * A resource switched off here is dropped by `PermissionUsageMonitor` rather than merely hidden, so
 * an unwatched resource costs nothing and can never light a dot.
 */
@Composable
internal fun PermissionDotScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val position by viewModel.permissionDotPosition.collectAsStateWithLifecycle()
    val kinds by viewModel.permissionDotKinds.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PermissionDotPositionCard(
            selected = position,
            onSelect = viewModel::setPermissionDotPosition,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.permission_dot_location_title),
            description = stringResource(R.string.permission_dot_location_desc),
            checked = kinds.location,
            onCheckedChange = viewModel::setPermissionDotLocation,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.permission_dot_camera_title),
            description = stringResource(R.string.permission_dot_camera_desc),
            checked = kinds.camera,
            onCheckedChange = viewModel::setPermissionDotCamera,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.permission_dot_microphone_title),
            description = stringResource(R.string.permission_dot_microphone_desc),
            checked = kinds.microphone,
            onCheckedChange = viewModel::setPermissionDotMicrophone,
        )
    }
}

/**
 * The "Position" selector: which end of the collapsed pill the dots sit on.
 * [PermissionDotPosition]'s declaration order is the option order, so the two can't drift apart.
 */
@Composable
private fun PermissionDotPositionCard(
    selected: PermissionDotPosition,
    onSelect: (PermissionDotPosition) -> Unit,
) {
    val options = PermissionDotPosition.entries
    val labels = options.map {
        stringResource(
            when (it) {
                PermissionDotPosition.LEFT -> R.string.permission_dot_position_left
                PermissionDotPosition.RIGHT -> R.string.permission_dot_position_right
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.permission_dot_position_title),
                style = MaterialTheme.typography.titleMedium,
            )
            ExpressiveSegmentedRow(
                options = labels,
                selectedIndex = options.indexOf(selected),
                onSelect = { onSelect(options[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
