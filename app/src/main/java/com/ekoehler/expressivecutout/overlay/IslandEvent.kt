package com.ekoehler.expressivecutout.overlay

import android.app.PendingIntent
import android.app.RemoteInput
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.MusicButtonStyle

/**
 * A fully resolved, ready-to-render icon. Reducing every possible source (a Material
 * vector default, a user-picked image, or another app's launcher icon) to just two
 * cases keeps the [DynamicIsland] composable trivial and free of Android plumbing.
 */
sealed interface IslandIcon {
    data class Vector(val image: ImageVector) : IslandIcon
    data class Raster(val bitmap: ImageBitmap) : IslandIcon

    /**
     * A Lottie animation from `res/raw`, played once when the collapsed pill appears. Used for
     * events that read better as a motion beat than a static glyph (e.g. a padlock springing open
     * for [com.ekoehler.expressivecutout.core.SystemEventType.DEVICE_UNLOCKED]).
     */
    data class Lottie(@param:RawRes val resId: Int) : IslandIcon
}

/**
 * Everything the island needs to show a single moment on screen. [label] is the primary
 * line (shown when expanded); [detail] is an optional secondary line (e.g. a notification
 * title). The collapsed island shows only the icon.
 */
data class IslandEvent(
    val id: Long,
    val icon: IslandIcon,
    val label: String,
    val detail: String? = null,
    val accent: Color,
    /**
     * When true the icon badge ignores [accent] and is drawn with the theme's primary / on-primary
     * pair instead — the "Dynamic color for all events" option. Set only for system events.
     */
    val useThemeColor: Boolean = false,
    val initiallyExpanded: Boolean = false,
    /**
     * The tap action to run when the expanded island is tapped (a notification's content
     * intent). Null for events that have nothing to open (system events).
     */
    val contentIntent: PendingIntent? = null,
    /**
     * The originating notification's key, when this event mirrors one. Lets a swipe-to-dismiss
     * clear the real notification from the system, not just hide the pill. Null for everything else.
     */
    val notificationKey: String? = null,
    /** Optional action buttons shown as chips in the expanded island. */
    val actions: List<IslandAction> = emptyList(),
    /**
     * When non-null this is the music tile: the island shows album art on the collapsed pill and
     * playback controls when expanded (each gated by [MediaTileOptions]), reading live state from
     * [com.ekoehler.expressivecutout.core.NowPlayingBus]. Null for every other event.
     */
    val media: MediaTileOptions? = null,
    /**
     * When non-null this is the phone tile: the island shows the caller's photo on the collapsed
     * pill and the caller / duration / call actions when expanded (each gated by [CallTileOptions]),
     * reading live state from [com.ekoehler.expressivecutout.core.OnCallBus]. Null otherwise.
     */
    val call: CallTileOptions? = null,
)

/** Which parts of the phone tile to render (and how the call buttons look), per its settings. */
data class CallTileOptions(
    val showPhoto: Boolean,
    val showDuration: Boolean,
    val showActions: Boolean,
    /** Fill of the hang-up / end-call button. */
    val hangUpColor: CutoutColor,
    /** Fill shared by every other call button. */
    val otherButtonColor: CutoutColor,
)

/** Which parts of the music tile to render (and how the controls look), per the tile's settings. */
data class MediaTileOptions(
    val showAlbumArt: Boolean,
    /** Spin the album art while playback is live, freezing it when paused. */
    val rotateAlbumArt: Boolean,
    val showControls: Boolean,
    /** Look of the previous / next (skip) buttons. */
    val skipStyle: MusicButtonStyle = MusicButtonStyle.DEFAULT,
    /** Look of the central play / pause button. */
    val playPauseStyle: MusicButtonStyle = MusicButtonStyle.DEFAULT,
)

/**
 * A single tappable action shown in the expanded island. A plain action fires [intent] on tap;
 * when [reply] is non-null the chip opens an inline text field and the typed text is sent through
 * [intent] instead.
 */
data class IslandAction(
    val label: String,
    val intent: PendingIntent,
    val reply: IslandReply? = null,
    /** True for a destructive call action (hang up / end call), so the phone tile can tint it apart. */
    val destructive: Boolean = false,
)

/** Everything needed to send an inline reply through a notification action's intent. */
data class IslandReply(
    val resultKey: String,
    val remoteInputs: List<RemoteInput>,
    val hint: String?,
)
