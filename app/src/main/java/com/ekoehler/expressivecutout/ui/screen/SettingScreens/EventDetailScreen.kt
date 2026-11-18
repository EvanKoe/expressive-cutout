package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.SystemEventPayload
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.overlay.animatedIcon
import com.ekoehler.expressivecutout.overlay.animationLoopsByDefault
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.swe.widgets.BadgeStyleControls
import com.ekoehler.expressivecutout.system.ShizukuStatus
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.groupedShape
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
    val persistentEvents by viewModel.eventPersistent.collectAsStateWithLifecycle()
    val persistent = persistentEvents[type] ?: false
    val hotspotCountEnabled by viewModel.hotspotCountEnabled.collectAsStateWithLifecycle()
    val hotspotCountStyle by viewModel.hotspotCountStyle.collectAsStateWithLifecycle()
    val hotspotTurnOffColor by viewModel.hotspotTurnOffColor.collectAsStateWithLifecycle()
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val eventColors by viewModel.eventColors.collectAsStateWithLifecycle()
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val animatedIcons by viewModel.eventAnimatedIcons.collectAsStateWithLifecycle()
    val animatedIconLoops by viewModel.eventAnimatedIconLoops.collectAsStateWithLifecycle()

    val source = customIcons[type]
    val colorOverride = eventColors[type]
    val hasAnimation = type.animatedIcon() != null
    val animatedEnabled = animatedIcons[type] ?: true
    val loopEnabled = animatedIconLoops[type] ?: type.animationLoopsByDefault()
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
            shape = groupedShape(isFirst = true, isLast = true),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon
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

                // Name and source
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(type.labelRes),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Change icon button
                    FilledTonalButton(onClick = { showIconSheet = true }) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                        Text(
                            text = stringResource(R.string.event_change_icon),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    // Test button
                    FilledTonalButton(
                        onClick = { IslandEventBus.emit(CutoutSignal.System(previewPayloadFor(context, type))) },
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

        // Animated icon
        if (hasAnimation && source == null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsToggleCard(
                    shape = groupedShape(isFirst = true, isLast = !animatedEnabled),
                    title = stringResource(R.string.event_animated_icon),
                    description = stringResource(R.string.event_animated_icon_desc),
                    checked = animatedEnabled,
                    onCheckedChange = { viewModel.setEventAnimatedIcon(type, it) },
                )

                // Loop animation (if enabled)
                AnimatedVisibility(visible = animatedEnabled) {
                    SettingsToggleCard(
                        shape = groupedShape(isLast = true),
                        title = stringResource(R.string.event_loop),
                        description = stringResource(R.string.event_loop_desc),
                        checked = loopEnabled,
                        onCheckedChange = { viewModel.setEventAnimatedIconLoop(type, it) },
                    )
                }
            }
        }

        // Event color
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
            shape = groupedShape(isFirst = true, isLast = true),
            defaultLabel = stringResource(if (dynamicColor) R.string.  event_color_default_dynamic else R.string.event_color_default_accent),
            defaultColor = colorFallback,
        )

        if (type == SystemEventType.HOTSPOT_ENABLED) {
            val shizukuReady = shizukuStatus == ShizukuStatus.READY
            val showCount = hotspotCountEnabled && shizukuReady

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Connected devices toggle
                SettingsToggleCard(
                    title = stringResource(R.string.hotspot_count_title),
                    description = stringResource(R.string.hotspot_count_desc),
                    disabledReason = if (shizukuReady) null else stringResource(R.string.requires_shizuku_access),
                    checked = hotspotCountEnabled,
                    onCheckedChange = { viewModel.setHotspotCountEnabled(it) },
                    enabled = shizukuReady,
                    switchBadge = painterResource(R.drawable.ic_shizuku),
                    shape = groupedShape(isFirst = true, isLast = !showCount),
                )

                // Badge style
                AnimatedVisibility(visible = showCount) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BadgeStyleControls(
                            widget = hotspotCountStyle,
                            onChange = { viewModel.setHotspotCountStyle(it) },
                        )

                        // Turn off button colour
                        ColorPickerCard(
                            label = stringResource(R.string.hotspot_turn_off_color),
                            selected = hotspotTurnOffColor,
                            onSelect = { viewModel.setHotspotTurnOffColor(it) },
                            defaultLabel = stringResource(R.string.label_default),
                            defaultColor = colorFallback,
                            shape = groupedShape(isLast = true),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Persistent event
            if (type != SystemEventType.DEVICE_LOCKED) {
                SettingsToggleCard(
                    title = stringResource(R.string.event_persistent_title),
                    description = stringResource(R.string.event_persistent_desc),
                    checked = persistent,
                    onCheckedChange = { viewModel.setEventPersistent(type, it) },
                    shape = groupedShape(isFirst = true, isLast = persistent)
                )
            }

            // Cutout duration
            AnimatedVisibility(visible = !persistent) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = groupedShape(isFirst = false, isLast = true),
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
            }
        }
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
internal fun IconChooserSheet(
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
                modifier = Modifier.clip(shape = groupedShape(isFirst = true, isLast = true)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_choose_image)) },
                    leadingContent = { Icon(Icons.Rounded.Image, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onChooseImage)
                        .clip(shape = groupedShape()),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_choose_material)) },
                    leadingContent = { Icon(Icons.Rounded.Category, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onChooseMaterial)
                        .clip(shape = groupedShape()),
                )

                AnimatedVisibility(visible = hasOverride) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_use_default)) },
                        leadingContent = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                        modifier = Modifier.clickable(onClick = onUseDefault)
                            .clip(shape = groupedShape()),
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Creates sample metadata for previewing how [type] renders with full payload details in the island. */
private fun previewPayloadFor(context: Context, type: SystemEventType): SystemEventPayload = when (type) {
    SystemEventType.CHARGING_STARTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_charging_started),
        subtitle = "85% • Fast charging",
        collapsedBadgeText = "85%",
        actionIntentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
    )
    SystemEventType.CHARGING_STOPPED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_charging_stopped),
        subtitle = "85% remaining",
        collapsedBadgeText = "85%",
        actionIntentAction = Intent.ACTION_POWER_USAGE_SUMMARY,
    )
    SystemEventType.CHARGING_COMPLETE -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_charging_complete),
        subtitle = "100% • Ready to unplug",
        collapsedBadgeText = "100%",
        actionIntentAction = Intent.ACTION_POWER_USAGE_SUMMARY,
    )
    SystemEventType.BATTERY_LOW -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_battery_low),
        subtitle = "15% • Connect charger",
        collapsedBadgeText = "15%",
        actionIntentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
    )
    SystemEventType.WIFI_CONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_wifi_connected),
        subtitle = "Pixel_5GHz",
        actionIntentAction = Settings.ACTION_WIFI_SETTINGS,
    )
    SystemEventType.WIFI_DISCONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_wifi_disconnected),
        subtitle = "Disconnected",
        actionIntentAction = Settings.ACTION_WIFI_SETTINGS,
    )
    SystemEventType.HEADPHONES_CONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_headphones_connected),
        subtitle = "Pixel Buds Pro",
        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
    )
    SystemEventType.HEADPHONES_DISCONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_headphones_disconnected),
        subtitle = "Audio routed to speaker",
        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
    )
    SystemEventType.USB_MOUNTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_usb_mounted),
        subtitle = "SanDisk Ultra USB 3.0",
        actionIntentAction = Settings.ACTION_SETTINGS,
    )
    SystemEventType.USB_UNMOUNTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_usb_unmounted),
        subtitle = "Device disconnected",
        actionIntentAction = Settings.ACTION_SETTINGS,
    )
    SystemEventType.DEVICE_LOCKED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_device_locked),
        subtitle = "Device secured",
        actionIntentAction = Settings.ACTION_SECURITY_SETTINGS,
    )
    SystemEventType.DEVICE_UNLOCKED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_device_unlocked),
        subtitle = "Device unlocked",
        actionIntentAction = Settings.ACTION_SECURITY_SETTINGS,
    )
    SystemEventType.VPN_CONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_vpn_connected),
        subtitle = "Secure tunnel active",
        actionIntentAction = Settings.ACTION_VPN_SETTINGS,
    )
    SystemEventType.VPN_DISCONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_vpn_disconnected),
        subtitle = "Tunnel closed",
        actionIntentAction = Settings.ACTION_VPN_SETTINGS,
    )
    SystemEventType.ADB_CONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_adb_connected),
        subtitle = "Host authorized",
        collapsedBadgeText = "USB",
        actionIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
    )
    SystemEventType.ADB_DISCONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_adb_disconnected),
        subtitle = "Session closed",
        collapsedBadgeText = "USB",
        actionIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
    )
    SystemEventType.WIRELESS_DEBUGGING_CONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_wireless_debugging_connected),
        subtitle = "Network: Pixel_5GHz",
        collapsedBadgeText = "Wi‑Fi",
        actionIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
    )
    SystemEventType.WIRELESS_DEBUGGING_DISCONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_wireless_debugging_disconnected),
        subtitle = "Wireless session closed",
        collapsedBadgeText = "Wi‑Fi",
        actionIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
    )
    SystemEventType.BLUETOOTH_CONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_bluetooth_connected),
        subtitle = "Logitech MX Master 3S",
        actionIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
    )
    SystemEventType.BLUETOOTH_DISCONNECTED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_bluetooth_disconnected),
        subtitle = "Device disconnected",
        actionIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
    )
    SystemEventType.HOTSPOT_ENABLED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_hotspot_enabled),
        subtitle = "Tethering ready",
        actionIntentAction = Settings.ACTION_WIRELESS_SETTINGS,
    )
    SystemEventType.HOTSPOT_DISABLED -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_hotspot_disabled),
        subtitle = "Tethering off",
        actionIntentAction = Settings.ACTION_WIRELESS_SETTINGS,
    )
    SystemEventType.RINGER_NORMAL -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_ringer_normal),
        subtitle = "Calls & alerts active",
        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
    )
    SystemEventType.RINGER_VIBRATE -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_ringer_vibrate),
        subtitle = "Calls and alerts will vibrate",
        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
    )
    SystemEventType.RINGER_SILENT -> SystemEventPayload(
        type = type,
        title = context.getString(R.string.event_ringer_silent),
        subtitle = "Calls and alerts muted",
        actionIntentAction = Settings.ACTION_SOUND_SETTINGS,
    )
}
