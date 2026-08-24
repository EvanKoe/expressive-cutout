package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.core.PermissionDotPreviewBus
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DEFAULT_CAMERA_DOT_COLOR
import com.ekoehler.expressivecutout.data.DEFAULT_LOCATION_DOT_COLOR
import com.ekoehler.expressivecutout.data.DEFAULT_MICROPHONE_DOT_COLOR
import com.ekoehler.expressivecutout.data.PermissionDotColors
import com.ekoehler.expressivecutout.data.PermissionDotPosition
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow

/**
 * "Permission dot" detail screen, reached from the switch on the Shizuku options list. Holds which
 * end of the collapsed pill the dots sit on, plus a switch and a dot colour per watched resource.
 *
 * A resource switched off here is dropped by `PermissionUsageMonitor` rather than merely hidden, so
 * an unwatched resource costs nothing and can never light a dot.
 *
 * There is no mock preview: while this screen is open the real island is pinned and every enabled
 * dot is reported as in use, so the dots are judged where they actually live.
 */
@Composable
internal fun PermissionDotScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val position by viewModel.permissionDotPosition.collectAsStateWithLifecycle()
    val kinds by viewModel.permissionDotKinds.collectAsStateWithLifecycle()
    val colors by viewModel.permissionDotColors.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The preview is the real thing: pin the island open and have the monitor report every enabled
    // resource as in use, so the dots show on the actual cutout while this screen is up. Both are
    // dropped on pause so a backgrounded app isn't left claiming the camera is in use.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        fun pin(active: Boolean) {
            IslandPreviewBus.setActive(active && Permissions.isAccessibilityGranted(context))
            PermissionDotPreviewBus.setActive(active)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> pin(true)
                Lifecycle.Event.ON_PAUSE -> pin(false)
                else -> Unit
            }
        }
        pin(true)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pin(false)
        }
    }

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

        PermissionDotKindCard(
            title = stringResource(R.string.permission_dot_location_title),
            description = stringResource(R.string.permission_dot_location_desc),
            colorLabel = stringResource(R.string.permission_dot_location_color),
            checked = kinds.location,
            onCheckedChange = viewModel::setPermissionDotLocation,
            color = colors.location,
            defaultColor = DEFAULT_LOCATION_DOT_COLOR,
            onSelectColor = viewModel::setPermissionDotLocationColor,
        )

        PermissionDotKindCard(
            title = stringResource(R.string.permission_dot_camera_title),
            description = stringResource(R.string.permission_dot_camera_desc),
            colorLabel = stringResource(R.string.permission_dot_camera_color),
            checked = kinds.camera,
            onCheckedChange = viewModel::setPermissionDotCamera,
            color = colors.camera,
            defaultColor = DEFAULT_CAMERA_DOT_COLOR,
            onSelectColor = viewModel::setPermissionDotCameraColor,
        )

        PermissionDotKindCard(
            title = stringResource(R.string.permission_dot_microphone_title),
            description = stringResource(R.string.permission_dot_microphone_desc),
            colorLabel = stringResource(R.string.permission_dot_microphone_color),
            checked = kinds.microphone,
            onCheckedChange = viewModel::setPermissionDotMicrophone,
            color = colors.microphone,
            defaultColor = DEFAULT_MICROPHONE_DOT_COLOR,
            onSelectColor = viewModel::setPermissionDotMicrophoneColor,
        )
    }
}

/**
 * One watched resource, as a single card: its switch, and — while the resource is on — the colour
 * its dot is drawn in. Grouped rather than stacked as two cards so the colour visibly belongs to
 * the resource above it. Clearing the picker falls back to [defaultColor], the stock colour.
 */
@Composable
private fun PermissionDotKindCard(
    title: String,
    description: String,
    colorLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: CutoutColor,
    defaultColor: CutoutColor,
    onSelectColor: (CutoutColor) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }

        AnimatedVisibility(visible = checked) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                // The shared picker, with its own card flattened into this one so the pair reads as
                // a single grouped setting.
                ColorPickerCard(
                    label = colorLabel,
                    selected = color,
                    onSelect = { onSelectColor(it ?: defaultColor) },
                    defaultLabel = stringResource(R.string.label_default),
                    defaultColor = defaultColor.resolve(),
                    roundedCorners = 0.dp,
                )
            }
        }
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
