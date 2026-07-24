package com.ekoehler.expressivecutout.overlay

import android.app.PendingIntent
import android.app.RemoteInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A fully resolved, ready-to-render icon. Reducing every possible source (a Material
 * vector default, a user-picked image, or another app's launcher icon) to just two
 * cases keeps the [DynamicIsland] composable trivial and free of Android plumbing.
 */
sealed interface IslandIcon {
    data class Vector(val image: ImageVector) : IslandIcon
    data class Raster(val bitmap: ImageBitmap) : IslandIcon
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
    val initiallyExpanded: Boolean = false,
    /**
     * The tap action to run when the expanded island is tapped (a notification's content
     * intent). Null for events that have nothing to open (system events).
     */
    val contentIntent: PendingIntent? = null,
    /** Optional action buttons shown as chips in the expanded island. */
    val actions: List<IslandAction> = emptyList(),
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
)

/** Everything needed to send an inline reply through a notification action's intent. */
data class IslandReply(
    val resultKey: String,
    val remoteInputs: List<RemoteInput>,
    val hint: String?,
)
