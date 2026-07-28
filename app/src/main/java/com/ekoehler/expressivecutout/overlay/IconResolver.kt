package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.LottieConstants
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import com.ekoehler.expressivecutout.data.TimerTileSettings
import java.util.concurrent.atomic.AtomicLong

/**
 * The Lottie animation to use for events that read better as motion than a static glyph, or null for
 * events that have none. A top-level extension (not a member of [SystemEventType]) so the enum stays a
 * pure domain type, while both the overlay and the settings preview can share this single mapping.
 */
fun SystemEventType.animatedIcon(): IslandIcon.Lottie? = when (this) {
    // Play once and hold on the "open" frame (45 of 80): the source clip loops back to a closed
    // padlock, but a device-unlocked event should rest unlocked. Tinted so the padlock follows the
    // badge glyph colour (accent by default, the role's "on" colour under a dynamic container),
    // rather than staying its baked-in light art — which vanished on a light dynamic fill.
    SystemEventType.DEVICE_UNLOCKED -> IslandIcon.Lottie(
        R.raw.unlock,
        clipStartFrame = 0,
        clipEndFrame = 45,
        tint = true,
    )
    // A charging bolt that loops for as long as the cutout is shown. It sits small within its own
    // canvas, so scale it up, and tint it to the badge colour so it follows the theme/accent.
    SystemEventType.CHARGING_STARTED -> IslandIcon.Lottie(
        R.raw.charging,
        iterations = LottieConstants.IterateForever,
        scale = 4f,
        tint = true,
    )
    else -> null
}

/**
 * Whether this event's animation loops by default — mirrors its [animatedIcon]'s own iterations, so
 * the per-event "Loop" toggle starts from the built-in behaviour (charging loops, unlock plays once).
 */
fun SystemEventType.animationLoopsByDefault(): Boolean =
    animatedIcon()?.iterations == LottieConstants.IterateForever

/**
 * Turns a source-agnostic [CutoutSignal] into a renderable [IslandEvent], applying the
 * user's icon overrides and looking up app metadata. This is the one place that touches
 * the [PackageManager] and content resolver, so all the "impure" resolution lives here.
 */
class IconResolver(private val context: Context) {

    private val idGenerator = AtomicLong(0L)

    fun resolve(
        signal: CutoutSignal,
        customIcons: Map<SystemEventType, IconSource>,
        musicSettings: MusicTileSettings,
        phoneSettings: PhoneTileSettings,
        timerSettings: TimerTileSettings,
        dynamicEventColor: Boolean = false,
        dynamicEventColorRole: DynamicRole = DynamicRole.PRIMARY,
        dynamicEventColorOpacity: Float = 1f,
        animatedIconEnabled: Map<SystemEventType, Boolean> = emptyMap(),
        animatedIconLoop: Map<SystemEventType, Boolean> = emptyMap(),
        eventColorOverrides: Map<SystemEventType, CutoutColor> = emptyMap(),
    ): IslandEvent = when (signal) {
        is CutoutSignal.Notification -> resolveNotification(signal)
        is CutoutSignal.System -> resolveSystem(
            signal.type,
            customIcons,
            dynamicEventColor,
            dynamicEventColorRole,
            dynamicEventColorOpacity,
            animatedIconEnabled,
            animatedIconLoop,
            eventColorOverrides,
        )
        is CutoutSignal.Music -> resolveMusic(signal, musicSettings)
        is CutoutSignal.Call -> resolveCall(signal, phoneSettings)
        is CutoutSignal.Timer -> resolveTimer(signal, timerSettings)
    }

    private fun resolveNotification(signal: CutoutSignal.Notification): IslandEvent {
        val packageManager = context.packageManager
        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(signal.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(signal.packageName)

        val icon = runCatching {
            packageManager.getApplicationIcon(signal.packageName).toImageBitmap()
        }.onFailure { Log.w(TAG, "No icon for ${signal.packageName}", it) }
            .map { IslandIcon.Raster(it) as IslandIcon }
            .getOrDefault(IslandIcon.Vector(SystemEventType.DEVICE_UNLOCKED.defaultIcon))

        val title = signal.title?.takeIf { it.isNotBlank() }
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            // Expanded shows the notification's title and text; the icon conveys the app.
            label = title ?: appLabel,
            detail = signal.text?.takeIf { it.isNotBlank() },
            accent = NOTIFICATION_ACCENT,
            contentIntent = signal.contentIntent,
            notificationKey = signal.key,
            actions = signal.actions.map { action ->
                IslandAction(
                    label = action.title,
                    intent = action.intent,
                    reply = action.reply?.let {
                        IslandReply(it.resultKey, it.remoteInputs, it.hint)
                    },
                )
            },
        )
    }

    private fun resolveMusic(signal: CutoutSignal.Music, settings: MusicTileSettings): IslandEvent {
        val packageManager = context.packageManager
        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(signal.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(signal.packageName)

        // Fallback icon for the collapsed pill when there is no album art (or it's turned off):
        // the player's own launcher icon reads better than a generic note.
        val icon = runCatching {
            packageManager.getApplicationIcon(signal.packageName).toImageBitmap()
        }.onFailure { Log.w(TAG, "No icon for ${signal.packageName}", it) }
            .map { IslandIcon.Raster(it) as IslandIcon }
            .getOrDefault(IslandIcon.Vector(DynamicTile.MUSIC.defaultIcon))

        val title = signal.title?.takeIf { it.isNotBlank() }
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            // Track title on the primary line; artist (or the app) on the secondary line.
            label = title ?: context.getString(DynamicTile.MUSIC.labelRes),
            detail = signal.artist?.takeIf { it.isNotBlank() } ?: appLabel,
            accent = Color(DynamicTile.MUSIC.accent),
            contentIntent = signal.contentIntent,
            media = MediaTileOptions(
                showAlbumArt = settings.showAlbumArt,
                rotateAlbumArt = settings.rotateAlbumArt,
                showControls = settings.showControls,
                skipStyle = settings.skipButton,
                playPauseStyle = settings.playPauseButton,
            ),
        )
    }

    private fun resolveCall(signal: CutoutSignal.Call, settings: PhoneTileSettings): IslandEvent {
        // The live contact photo comes from OnCallBus; this icon is only the no-photo fallback, so a
        // person avatar reads as "a contact" (the Google-dialer default look) better than a handset.
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = IslandIcon.Vector(Icons.Rounded.Person),
            label = signal.callerLabel,
            accent = Color(DynamicTile.PHONE.accent),
            iconContainerColor = settings.iconContainerColor,
            contentIntent = signal.contentIntent,
            // Deliberately no notificationKey: a swipe should hide the pill, never cancel the
            // dialer's own call notification (which wouldn't end the call and would just re-post).
            actions = signal.actions.map { action ->
                IslandAction(
                    label = action.title,
                    intent = action.intent,
                    destructive = isHangUpLabel(action.title),
                    answer = isAnswerLabel(action.title),
                )
            },
            call = CallTileOptions(
                showPhoto = settings.showPhoto,
                showDuration = settings.showDuration,
                showActions = settings.showActions,
                hangUpColor = settings.hangUpColor,
                otherButtonColor = settings.otherButtonColor,
            ),
        )
    }

    /**
     * Map a timer notification's own buttons to island chips verbatim — real labels and intents — so
     * they always match what the notification shows (Google Clock renders "Pause" / "Add 1 min" while
     * running and "Resume" / "Reset" while paused). A reset / stop / delete button is tinted apart via
     * [isResetLabel]. Public so the overlay can re-map them live as the timer's buttons change.
     */
    fun timerActions(actions: List<CutoutSignal.Notification.Action>): List<IslandAction> =
        actions.take(3).map { action ->
            IslandAction(
                label = action.title,
                intent = action.intent,
                destructive = isResetLabel(action.title),
            )
        }

    private fun resolveTimer(signal: CutoutSignal.Timer, settings: TimerTileSettings): IslandEvent {
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = IslandIcon.Vector(DynamicTile.TIMER.defaultIcon),
            label = signal.label?.takeIf { it.isNotBlank() }
                ?: context.getString(DynamicTile.TIMER.labelRes),
            accent = Color(DynamicTile.TIMER.accent),
            iconContainerColor = settings.iconContainerColor,
            contentIntent = signal.contentIntent,
            // Deliberately no notificationKey: a swipe should hide the pill, never cancel the clock's
            // own timer notification (which wouldn't stop the timer and would just re-post).
            actions = timerActions(signal.actions),
            timer = TimerTileOptions(
                showActions = settings.showActions,
                resetColor = settings.resetColor,
                addButtonColor = settings.addButtonColor,
            ),
        )
    }

    /** Best-effort match for a timer's reset / stop / delete button by its label, to tint it apart. */
    private fun isResetLabel(label: String): Boolean {
        val normalised = label.lowercase()
        return RESET_KEYWORDS.any { normalised.contains(it) }
    }

    /**
     * Best-effort match for a call's hang-up / end-call / decline button by its label. A call
     * notification carries no machine-readable flag marking which action ends the call, so we key
     * off the label; failing to match simply leaves that button on the shared "other" colour.
     */
    private fun isHangUpLabel(label: String): Boolean {
        val normalised = label.lowercase()
        return HANG_UP_KEYWORDS.any { normalised.contains(it) }
    }

    /**
     * Best-effort match for an incoming call's answer / accept button by its label, mirroring
     * [isHangUpLabel]; failing to match simply leaves the tile without a dedicated take-call button.
     */
    private fun isAnswerLabel(label: String): Boolean {
        val normalised = label.lowercase()
        return ANSWER_KEYWORDS.any { normalised.contains(it) }
    }

    private fun resolveSystem(
        type: SystemEventType,
        customIcons: Map<SystemEventType, IconSource>,
        dynamicEventColor: Boolean,
        dynamicEventColorRole: DynamicRole,
        dynamicEventColorOpacity: Float,
        animatedIconEnabled: Map<SystemEventType, Boolean>,
        animatedIconLoop: Map<SystemEventType, Boolean>,
        eventColorOverrides: Map<SystemEventType, CutoutColor>,
    ): IslandEvent {
        // A user override always wins; otherwise events with an animation (charging / unlock) use it
        // when the "Animated icon" toggle is on — looping per the "Loop" toggle — and every other
        // event (or a disabled animation) falls back to the static default glyph.
        val animated = type.animatedIcon()?.takeIf { animatedIconEnabled[type] ?: true }?.copy(
            iterations = if (animatedIconLoop[type] ?: type.animationLoopsByDefault()) {
                LottieConstants.IterateForever
            } else {
                1
            },
        )
        val icon = customIcons[type]?.toIslandIconOrNull()
            ?: animated
            ?: IslandIcon.Vector(type.defaultIcon)
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            label = context.getString(type.labelRes),
            accent = Color(type.accent),
            useThemeColor = dynamicEventColor,
            themeColorRole = dynamicEventColorRole,
            themeColorOpacity = dynamicEventColorOpacity,
            colorOverride = eventColorOverrides[type],
        )
    }

    /** Resolves the chosen override into a renderable icon, or null to fall back to the default. */
    private fun IconSource.toIslandIconOrNull(): IslandIcon? = when (this) {
        is IconSource.Image ->
            Uri.parse(uri).loadImageBitmapOrNull(context)?.let(IslandIcon::Raster)

        is IconSource.Material ->
            MaterialIconCatalog.iconFor(iconName)?.let(IslandIcon::Vector)
    }

    private companion object {
        const val TAG = "IconResolver"
        val NOTIFICATION_ACCENT = Color(0xFF38BDF8)

        // Lower-cased substrings that mark a call's end/decline action. English-led (most dialers'
        // notifications localise to the device language, but English covers the common case); the
        // phrases avoid false hits like "send" that a bare "end" would catch.
        val HANG_UP_KEYWORDS = listOf("hang up", "hangup", "hang-up", "end call", "decline", "reject")

        // Lower-cased substrings marking an incoming call's answer/accept action, so the tile can
        // render it as the take-call button (mirrors HANG_UP_KEYWORDS; English covers the common case).
        val ANSWER_KEYWORDS = listOf("answer", "accept", "pick up", "pickup", "take call")

        // Lower-cased substrings marking a timer's reset/terminate action, so it can be tinted apart.
        // "stop" and "delete" cover the common clock apps; "pause" is deliberately excluded (it isn't
        // a reset, so it takes the shared "other" colour like Add 1 min).
        val RESET_KEYWORDS = listOf("reset", "stop", "delete", "cancel", "dismiss")
    }
}
