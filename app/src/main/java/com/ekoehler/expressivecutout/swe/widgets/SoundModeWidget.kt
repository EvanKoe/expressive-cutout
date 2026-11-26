package com.ekoehler.expressivecutout.swe.widgets

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.badgeIconSizeFor
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.swe.DEFAULT_BADGE_CORNER_PERCENT
import com.ekoehler.expressivecutout.swe.DEFAULT_BADGE_WIDTH_PERCENT
import com.ekoehler.expressivecutout.swe.MAX_BADGE_CORNER_PERCENT
import com.ekoehler.expressivecutout.swe.MAX_BADGE_WIDTH_PERCENT
import com.ekoehler.expressivecutout.swe.MIN_BADGE_WIDTH_PERCENT
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweIconBadge
import com.ekoehler.expressivecutout.swe.SweIconBadgeControls
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.swe.color
import com.ekoehler.expressivecutout.swe.inkOn
import com.ekoehler.expressivecutout.swe.putColor
import com.ekoehler.expressivecutout.swe.sweIconBadgeWidthDp
import org.json.JSONObject

/** The device's sound mode as an icon, on an optional rounded-square background. */
data class SoundModeWidget(
    override val id: String = SweWidgets.newId(),
    val iconColor: CutoutColor? = null,
    val backgroundColor: CutoutColor? = null,
    val backgroundWidthPercent: Int = DEFAULT_BADGE_WIDTH_PERCENT,
    val backgroundCornerPercent: Int = DEFAULT_BADGE_CORNER_PERCENT,
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
        sweIconBadgeWidthDp(backgroundColor != null, backgroundWidthPercent, heightDp)

    /** The current sound mode icon, crossfading when the mode changes. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        val mode by rememberSoundMode()

        SweIconBadge(backgroundColor, backgroundWidthPercent, backgroundCornerPercent, heightDp) { fill ->
            val tint = iconColor?.resolve() ?: if (fill != null) inkOn(fill) else LocalContentColor.current

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
    override fun Controls(onChange: (SweWidget) -> Unit) = SweIconBadgeControls(
        iconColor = iconColor,
        backgroundColor = backgroundColor,
        backgroundWidthPercent = backgroundWidthPercent,
        backgroundCornerPercent = backgroundCornerPercent,
        onIconColor = { onChange(copy(iconColor = it)) },
        onBackgroundColor = { onChange(copy(backgroundColor = it)) },
        onWidth = { onChange(copy(backgroundWidthPercent = it)) },
        onCorner = { onChange(copy(backgroundCornerPercent = it)) },
    )

    @Composable
    override fun summary(): String = stringResource(
        if (backgroundColor != null) R.string.widgets_soundMode_background_color else R.string.label_default,
    )

    /** The sound mode kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        /** Stored in user settings and backups; never change it once shipped. */
        private const val TAG = "sound_mode"

        override val tag: String = TAG
        override val icon: ImageVector get() = Icons.AutoMirrored.Rounded.VolumeUp
        override val titleRes: Int = R.string.widgets_soundMode_title

        override fun create(): SweWidget = SoundModeWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = SoundModeWidget(
            id = id,
            iconColor = obj.color("iconColor"),
            backgroundColor = obj.color("backgroundColor"),
            backgroundWidthPercent = obj.optInt("backgroundWidthPercent", DEFAULT_BADGE_WIDTH_PERCENT)
                .coerceIn(MIN_BADGE_WIDTH_PERCENT, MAX_BADGE_WIDTH_PERCENT),
            backgroundCornerPercent = obj.optInt("backgroundCornerPercent", DEFAULT_BADGE_CORNER_PERCENT)
                .coerceIn(0, MAX_BADGE_CORNER_PERCENT),
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
