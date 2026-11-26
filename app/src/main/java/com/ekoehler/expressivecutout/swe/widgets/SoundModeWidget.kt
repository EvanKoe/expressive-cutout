package com.ekoehler.expressivecutout.swe.widgets

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.ekoehler.expressivecutout.swe.SweGroupPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.badgeIconSizeFor
import com.ekoehler.expressivecutout.overlay.badgeSizeFor
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.swe.PERCENT
import com.ekoehler.expressivecutout.swe.LocalSweGroupPosition
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.sweBackgroundShape
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.swe.color
import com.ekoehler.expressivecutout.swe.inkOn
import com.ekoehler.expressivecutout.swe.putColor
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.GroupGap
import com.ekoehler.expressivecutout.ui.components.groupedShape
import com.ekoehler.expressivecutout.ui.screen.AdjustableSlider
import org.json.JSONObject
import kotlin.math.roundToInt

/** The device's sound mode as an icon, on an optional rounded-square background. */
data class SoundModeWidget(
    override val id: String = SweWidgets.newId(),
    val iconColor: CutoutColor? = null,
    val backgroundColor: CutoutColor? = null,
    val backgroundWidthPercent: Int = DEFAULT_BACKGROUND_WIDTH_PERCENT,
    val backgroundCornerPercent: Int = DEFAULT_BACKGROUND_CORNER_PERCENT,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
        putColor("iconColor", iconColor)
        putColor("backgroundColor", backgroundColor)
        put("backgroundWidthPercent", backgroundWidthPercent)
        put("backgroundCornerPercent", backgroundCornerPercent)
    }

    override fun isVisible(data: SweData): Boolean = true

    override val hasBackground: Boolean get() = backgroundColor != null

    /** Depends on the background settings only, never on the mode, so the pill does not twitch. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float =
        if (backgroundColor != null) backgroundWidthDp(heightDp) else badgeIconSizeFor(heightDp).value

    /** The background's width in dp: the badge size scaled by the width setting. */
    private fun backgroundWidthDp(heightDp: Int): Float =
        badgeSizeFor(heightDp).value * backgroundWidthPercent / PERCENT

    /** The current sound mode icon, crossfading when the mode changes. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        val mode by rememberSoundMode()
        val fill = backgroundColor?.resolve()
        val tint = iconColor?.resolve() ?: if (fill != null) inkOn(fill) else LocalContentColor.current
        val boxModifier = if (fill != null) {
            Modifier
                .size(backgroundWidthDp(heightDp).dp, badgeSizeFor(heightDp))
                .background(
                    fill,
                    sweBackgroundShape(LocalSweGroupPosition.current, heightDp, RoundedCornerShape(backgroundCornerPercent)),
                )
        } else {
            Modifier
        }

        // Optional background
        Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
            // Mode icon
            AnimatedContent(
                targetState = mode,
                transitionSpec = { (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) },
                label = "soundModeIcon",
            ) { target ->
                Icon(
                    imageVector = target.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(badgeIconSizeFor(heightDp)),
                )
            }
        }
    }

    /** The icon colour and background colour pickers, and the background's width and corner radius when it is shown. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        val hasBackground = backgroundColor != null
        val isGrouped = LocalSweGroupPosition.current != SweGroupPosition.SINGLE
        var widthValue by remember(backgroundWidthPercent) { mutableFloatStateOf(backgroundWidthPercent.toFloat()) }
        var cornerValue by remember(backgroundCornerPercent) { mutableFloatStateOf(backgroundCornerPercent.toFloat()) }

        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            // Icon colour
            ColorPickerCard(
                label = stringResource(R.string.widgets_soundMode_icon_color),
                selected = iconColor,
                onSelect = { onChange(copy(iconColor = it)) },
                defaultLabel = stringResource(R.string.label_default),
                defaultColor = MaterialTheme.colorScheme.onSurface,
                shape = groupedShape(isFirst = true),
            )

            // Background colour
            ColorPickerCard(
                label = stringResource(R.string.widgets_soundMode_background_color),
                selected = backgroundColor,
                onSelect = { onChange(copy(backgroundColor = it)) },
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
                            valueRange = MIN_BACKGROUND_WIDTH_PERCENT.toFloat()..MAX_BACKGROUND_WIDTH_PERCENT.toFloat(),
                            step = 5f,
                            onValueChange = { widthValue = it },
                            onCommit = { onChange(copy(backgroundWidthPercent = widthValue.roundToInt())) },
                        )
                        AdjustableSlider(
                            label = stringResource(R.string.appearance_corner),
                            valueText = "${cornerValue.roundToInt()}%",
                            value = cornerValue,
                            valueRange = 0f..MAX_BACKGROUND_CORNER_PERCENT.toFloat(),
                            step = 1f,
                            onValueChange = { cornerValue = it },
                            onCommit = { onChange(copy(backgroundCornerPercent = cornerValue.roundToInt())) },
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

    @Composable
    override fun summary(): String = stringResource(
        if (backgroundColor != null) R.string.widgets_soundMode_background_color else R.string.label_default,
    )

    /** The sound mode kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        /** Stored in user settings and backups; never change it once shipped. */
        private const val TAG = "sound_mode"

        /** The background's corner radius setting, in percent of its shorter side; 50 % is a full pill. */
        private const val DEFAULT_BACKGROUND_CORNER_PERCENT = 32
        private const val MAX_BACKGROUND_CORNER_PERCENT = 50

        /** The background's width setting, in percent of the badge size; 100 % is a square. */
        private const val DEFAULT_BACKGROUND_WIDTH_PERCENT = 100
        private const val MIN_BACKGROUND_WIDTH_PERCENT = 100
        private const val MAX_BACKGROUND_WIDTH_PERCENT = 250

        override val tag: String = TAG
        override val icon: ImageVector get() = Icons.AutoMirrored.Rounded.VolumeUp
        override val titleRes: Int = R.string.widgets_soundMode_title

        override fun create(): SweWidget = SoundModeWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = SoundModeWidget(
            id = id,
            iconColor = obj.color("iconColor"),
            backgroundColor = obj.color("backgroundColor"),
            backgroundWidthPercent = obj.optInt("backgroundWidthPercent", DEFAULT_BACKGROUND_WIDTH_PERCENT)
                .coerceIn(MIN_BACKGROUND_WIDTH_PERCENT, MAX_BACKGROUND_WIDTH_PERCENT),
            backgroundCornerPercent = obj.optInt("backgroundCornerPercent", DEFAULT_BACKGROUND_CORNER_PERCENT)
                .coerceIn(0, MAX_BACKGROUND_CORNER_PERCENT),
        )
    }
}

/** The sound modes the widget can show, each with its icon. */
private enum class SoundMode(val icon: ImageVector) {
    RING(Icons.AutoMirrored.Rounded.VolumeUp),
    VIBRATE(Icons.Rounded.Vibration),
    MUTE(Icons.AutoMirrored.Rounded.VolumeOff),
    DND(Icons.Rounded.DoNotDisturbOn),
}

/** Reads the device's current [SoundMode]. */
private fun currentSoundMode(context: Context): SoundMode {
    val notifications = context.getSystemService(NotificationManager::class.java)
    val filter = notifications?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN

    // Do Not Disturb makes the ringer mode read as silent, so it is checked first
    if (filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
        filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    ) {
        return SoundMode.DND
    }

    return when (context.getSystemService(AudioManager::class.java)?.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> SoundMode.VIBRATE
        AudioManager.RINGER_MODE_SILENT -> SoundMode.MUTE
        else -> SoundMode.RING
    }
}

/** The live [SoundMode], refreshed on every ringer or Do Not Disturb broadcast while composed. */
@Composable
private fun rememberSoundMode(): State<SoundMode> {
    val context = LocalContext.current
    val mode = remember { mutableStateOf(currentSoundMode(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mode.value = currentSoundMode(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return mode
}
