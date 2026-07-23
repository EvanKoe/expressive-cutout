package com.ekoehler.expressivecutout.overlay

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
)
