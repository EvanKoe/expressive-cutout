package com.ekoehler.expressivecutout.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.overlay.animatedIcon
import com.ekoehler.expressivecutout.overlay.animationLoopsByDefault
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.ui.AppViewModel
import kotlin.math.roundToInt

/**
 * A single event's edit screen, reached by tapping its row on the Events list. Lets the user change
 * the event's icon (custom image / app icon / built-in default), fire a live preview of the cutout,
 * and tune how long that event lingers before it auto-dismisses.
 */
@Composable
internal fun EventDetailScreen(
    type: SystemEventType,
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val customIcons by viewModel.customIcons.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.eventDynamicColor.collectAsStateWithLifecycle()
    val dynamicColorRole by viewModel.eventDynamicColorRole.collectAsStateWithLifecycle()
    val dynamicColorOpacity by viewModel.eventDynamicColorOpacity.collectAsStateWithLifecycle()
    val durations by viewModel.eventDurations.collectAsStateWithLifecycle()
    val eventColors by viewModel.eventColors.collectAsStateWithLifecycle()
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val animatedIcons by viewModel.eventAnimatedIcons.collectAsStateWithLifecycle()
    val animatedIconLoops by viewModel.eventAnimatedIconLoops.collectAsStateWithLifecycle()

    val source = customIcons[type]
    // Per-event colour override; absent means the event follows its default accent (or, when the
    // global toggle is on, the dynamic role). The "default" swatch / reset button clears it.
    val colorOverride = eventColors[type]
    // The animated-icon controls only make sense for events that ship a Lottie and while no custom
    // image/app override is set (an override always wins over the animation on the cutout).
    val hasAnimation = type.animatedIcon() != null
    val animatedEnabled = animatedIcons[type] ?: true
    val loopEnabled = animatedIconLoops[type] ?: type.animationLoopsByDefault()
    // No override → the slider shows (and the cutout uses) the global normal duration.
    val override = durations[type]
    val defaultSeconds = behaviour.normalDurationSeconds
    var durationSeconds by remember(override, defaultSeconds) {
        mutableStateOf((override ?: defaultSeconds).toFloat())
    }

    var showIconSheet by remember { mutableStateOf(false) }
    var showMaterialPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setImageIcon(type, uri.toString())
        }
    }

    val sourceLabel = when (source) {
        null -> stringResource(R.string.label_default)
        is IconSource.Image -> stringResource(R.string.label_custom)
        is IconSource.Material -> stringResource(R.string.label_material)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero: the current icon at a glance, plus the two primary actions (change icon / test).
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EventIconThumbnail(
                    type = type,
                    source = source,
                    dynamicColor = dynamicColor,
                    dynamicColorRole = dynamicColorRole,
                    dynamicColorOpacity = dynamicColorOpacity,
                    size = 76.dp,
                    animate = animatedEnabled,
                    loop = loopEnabled,
                    colorOverride = colorOverride,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(type.labelRes),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { showIconSheet = true }) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                        Text(
                            text = stringResource(R.string.event_change_icon),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    FilledTonalButton(
                        onClick = { IslandEventBus.emit(CutoutSignal.System(type)) },
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(
                            text = stringResource(R.string.event_test),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        // Animated icon: only for events that ship a Lottie, and only while the default icon is in
        // use (a custom image/app override replaces the animation entirely).
        if (hasAnimation && source == null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsToggleCard(
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = if (animatedEnabled) 4.dp else 32.dp,
                        bottomEnd = if (animatedEnabled) 4.dp else 32.dp,
                    ),
                    title = stringResource(R.string.event_animated_icon),
                    description = stringResource(R.string.event_animated_icon_desc),
                    checked = animatedEnabled,
                    onCheckedChange = { viewModel.setEventAnimatedIcon(type, it) },
                )
                // Loop only applies while the animation is on.
                AnimatedVisibility(visible = animatedEnabled) {
                    SettingsToggleCard(
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp,
                        ),
                        title = stringResource(R.string.event_loop),
                        description = stringResource(R.string.event_loop_desc),
                        checked = loopEnabled,
                        onCheckedChange = { viewModel.setEventAnimatedIconLoop(type, it) },
                    )
                }
            }
        }

        // Colour: a per-event override that recolours the badge, winning over the event's default
        // accent and the global "Dynamic color for all events" role. The leading swatch / the reset
        // button below clear it, falling back to whichever of those two is currently in effect.
        val colorFallback = if (dynamicColor) {
            CutoutColor.Dynamic(dynamicColorRole).resolve()
        } else {
            Color(type.accent)
        }
        ColorPickerCard(
            label = stringResource(R.string.event_color_title),
            selected = colorOverride,
            onSelect = { picked ->
                if (picked == null) viewModel.resetEventColor(type)
                else viewModel.setEventColor(type, picked)
            },
            defaultLabel = if (dynamicColor) {
                stringResource(R.string.event_color_default_dynamic)
            } else {
                stringResource(R.string.event_color_default_accent)
            },
            defaultColor = colorFallback,
        )
        // Only offered once the event has its own override to fall back from — mirrors the duration
        // reset below, and the leading "default" swatch in the picker above.
        if (colorOverride != null) {
            TextButton(
                onClick = { viewModel.resetEventColor(type) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.event_color_reset))
            }
        }

        // Duration: per-event override for how long the cutout stays before it fades out.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdjustableSlider(
                    label = stringResource(R.string.event_duration_label),
                    valueText = "${durationSeconds.roundToInt()} s",
                    value = durationSeconds,
                    valueRange = BehaviourSettings.MIN_NORMAL_SECONDS.toFloat()..
                        BehaviourSettings.MAX_NORMAL_SECONDS.toFloat(),
                    step = 1f,
                    onValueChange = { durationSeconds = it },
                    onCommit = { viewModel.setEventDuration(type, durationSeconds.roundToInt()) },
                )
                // Only offered once the event has its own override to fall back from.
                if (override != null) {
                    TextButton(
                        onClick = { viewModel.resetEventDuration(type) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.event_duration_reset))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.event_duration_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }

    if (showIconSheet) {
        IconChooserSheet(
            hasOverride = source != null,
            onChooseImage = {
                showIconSheet = false
                imagePicker.launch(arrayOf("image/*"))
            },
            onChooseMaterial = {
                showIconSheet = false
                showMaterialPicker = true
            },
            onUseDefault = {
                showIconSheet = false
                viewModel.resetIcon(type)
            },
            onDismiss = { showIconSheet = false },
        )
    }

    if (showMaterialPicker) {
        MaterialIconPickerSheet(
            onPick = { iconName ->
                showMaterialPicker = false
                viewModel.setMaterialIcon(type, iconName)
            },
            onDismiss = { showMaterialPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconChooserSheet(
    hasOverride: Boolean,
    onChooseImage: () -> Unit,
    onChooseMaterial: () -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = stringResource(R.string.set_icon_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            )

            Column(
                modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_choose_image)) },
                    leadingContent = { Icon(Icons.Rounded.Image, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onChooseImage)
                        .clip(shape = RoundedCornerShape(4.dp)),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_choose_material)) },
                    leadingContent = { Icon(Icons.Rounded.Category, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onChooseMaterial)
                        .clip(shape = RoundedCornerShape(4.dp)),
                )

                AnimatedVisibility(visible = hasOverride) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_use_default)) },
                        leadingContent = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                        modifier = Modifier.clickable(onClick = onUseDefault)
                            .clip(shape = RoundedCornerShape(4.dp)),
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
