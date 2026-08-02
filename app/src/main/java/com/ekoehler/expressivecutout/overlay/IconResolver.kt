package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.LottieConstants
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.AssistantTileSettings
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
 * Turns a source-agnostic [CutoutSignal] into a renderable [IslandEvent], applying the user's icon
 * overrides and rasterising whatever art the signal carried. This is the one place that loads
 * drawables and reads the content resolver, so all the "impure" resolution lives here. It
 * deliberately asks the package manager nothing about other apps: everything shown comes from the
 * notification or media session itself, so the app needs no package-visibility declaration.
 */
class IconResolver(private val context: Context) {

    private val idGenerator = AtomicLong(0L)

    fun resolve(
        signal: CutoutSignal,
        customIcons: Map<SystemEventType, IconSource>,
        musicSettings: MusicTileSettings,
        phoneSettings: PhoneTileSettings,
        timerSettings: TimerTileSettings,
        assistantSettings: AssistantTileSettings = AssistantTileSettings(),
        dynamicEventColor: Boolean = false,
        dynamicEventColorRole: DynamicRole = DynamicRole.PRIMARY,
        dynamicEventColorOpacity: Float = 1f,
        animatedIconEnabled: Map<SystemEventType, Boolean> = emptyMap(),
        animatedIconLoop: Map<SystemEventType, Boolean> = emptyMap(),
        eventColorOverrides: Map<SystemEventType, CutoutColor> = emptyMap(),
    ): IslandEvent = when (signal) {
        is CutoutSignal.Notification -> resolveNotification(
            signal,
            dynamicEventColor,
            dynamicEventColorRole,
            dynamicEventColorOpacity,
        )
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
        is CutoutSignal.Assistant -> resolveAssistant(signal, assistantSettings)
    }

    private fun resolveNotification(
        signal: CutoutSignal.Notification,
        dynamicEventColor: Boolean,
        dynamicEventColorRole: DynamicRole,
        dynamicEventColorOpacity: Float,
    ): IslandEvent {
        // Everything shown comes from the notification itself — no app lookup, so the app needs no
        // package-visibility declaration at all. A notification always carries a small icon, so
        // the generic bell is a belt-and-braces fallback for one that somehow doesn't.
        val icon = signal.notificationIcon() ?: IslandIcon.Vector(Icons.Rounded.Notifications)

        val title = signal.title?.takeIf { it.isNotBlank() }
        val text = signal.text?.takeIf { it.isNotBlank() }
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = icon,
            // Expanded shows the notification's title and text; the icon conveys the app. A
            // notification with no title promotes its text to the primary line rather than
            // leaving the island to name the app it came from.
            label = title ?: text ?: context.getString(R.string.island_notification),
            detail = if (title != null) text else null,
            accent = NOTIFICATION_ACCENT,
            // A notification badge is a monochrome glyph far more often than a system event's is
            // art, so it follows "Dynamic color for all events" too when that is on.
            useThemeColor = dynamicEventColor,
            themeColorRole = dynamicEventColorRole,
            themeColorOpacity = dynamicEventColorOpacity,
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

    /**
     * The icon the posting app put on the notification itself, or null when it carries neither.
     * Colour first: a large icon is full-colour art (a sender's photo, a podcast cover) and is
     * drawn as-is. The small icon is Android's status-bar glyph — an alpha-only silhouette with no
     * colour of its own — so it is tinted with the badge's ink, following the theme like every
     * other glyph rather than being drawn as flat white pixels.
     */
    private fun CutoutSignal.Notification.notificationIcon(): IslandIcon? {
        largeIcon?.loadImageBitmapOrNull(context)?.let { return IslandIcon.Raster(it) }
        smallIcon?.loadImageBitmapOrNull(context)?.let { return IslandIcon.Raster(it, tint = true) }
        return null
    }

    private fun resolveMusic(signal: CutoutSignal.Music, settings: MusicTileSettings): IslandEvent {
        // The collapsed pill normally shows album art; the note glyph stands in when the session
        // carries none (or the user turned art off). Deliberately not the player's launcher icon:
        // resolving that would mean asking the system which apps are installed.
        val title = signal.title?.takeIf { it.isNotBlank() }
        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = IslandIcon.Vector(DynamicTile.MUSIC.defaultIcon),
            // Track title on the primary line, artist on the secondary line — both from the media
            // session itself, so an unnamed track falls back to "Music" rather than a package id.
            label = title ?: context.getString(DynamicTile.MUSIC.labelRes),
            detail = signal.artist?.takeIf { it.isNotBlank() },
            accent = Color(DynamicTile.MUSIC.accent),
            contentIntent = signal.contentIntent,
            media = MediaTileOptions(
                showAlbumArt = settings.showAlbumArt,
                rotateAlbumArt = settings.rotateAlbumArt,
                albumArtStroke = settings.albumArtStroke,
                albumArtStrokeColor = settings.albumArtStrokeColor,
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
                incomingExpandedLayout = settings.expandedIncomingLayout,
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

    private fun resolveAssistant(signal: CutoutSignal.Assistant, settings: AssistantTileSettings): IslandEvent {
        val defaultLabel = context.getString(DynamicTile.ASSISTANT.labelRes)
        val rawTitle = signal.title?.takeIf { it.isNotBlank() }
        val rawText = signal.text?.takeIf { it.isNotBlank() }

        val label = rawTitle ?: defaultLabel
        val answerText = when {
            rawText != null && rawText != label -> {
                if (rawTitle != null && rawTitle != defaultLabel) "$rawTitle\n$rawText" else rawText
            }
            rawTitle != null && rawTitle != defaultLabel -> rawTitle
            else -> rawText
        }

        return IslandEvent(
            id = idGenerator.incrementAndGet(),
            icon = IslandIcon.Vector(DynamicTile.ASSISTANT.defaultIcon),
            label = label,
            detail = rawText ?: rawTitle,
            accent = Color(DynamicTile.ASSISTANT.accent),
            iconContainerColor = settings.iconContainerColor,
            contentIntent = signal.contentIntent,
            initiallyExpanded = settings.displayAnswerInCutout,
            assistant = AssistantTileOptions(
                displayAnswerInCutout = settings.displayAnswerInCutout,
                maxCutoutHeightPercent = settings.maxCutoutHeightPercent,
                answerText = answerText,
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
