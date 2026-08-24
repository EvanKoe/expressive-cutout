package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

/** Log tag for the bitmap decoding below, which fails softly rather than throwing. */
private const val TAG = "BitmapUtils"

/**
 * Longest edge a badge icon is scaled down to. Icons arrive at whatever size their app chose, and
 * the island draws them a few dp across.
 */
private const val MAX_ICON_PX = 128

/**
 * Longest edge album art is scaled down to. Larger than an icon because the art fills the expanded
 * island, but still far below the full-size bitmap a player hands over.
 */
private const val MAX_ART_PX = 256

/** Rasterises any [Drawable] (e.g. an app launcher icon) into an [ImageBitmap]. */
fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable) {
        bitmap?.let { return it.asImageBitmap() }
    }
    val width = intrinsicWidth.takeIf { it > 0 } ?: MAX_ICON_PX
    val height = intrinsicHeight.takeIf { it > 0 } ?: MAX_ICON_PX
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

/**
 * Rasterises a notification's own [Icon] (its large icon or its small status-bar glyph), or null if
 * it cannot be loaded — a resource icon has to be read out of the posting app's package, which can
 * fail if that app is not visible to us or has since been uninstalled. Callers fall back.
 */
fun Icon.loadImageBitmapOrNull(context: Context): ImageBitmap? = runCatching {
    loadDrawable(context)?.toImageBitmap()
}.onFailure { Log.w(TAG, "Unable to load notification icon $this", it) }.getOrNull()

/**
 * Loads a user-picked image URI into a down-sampled [ImageBitmap], or null if it can no
 * longer be read (revoked permission, deleted file). Callers fall back to the default.
 */
fun Uri.loadImageBitmapOrNull(context: Context): ImageBitmap? = runCatching {
    context.contentResolver.openInputStream(this)?.use { stream ->
        android.graphics.BitmapFactory.decodeStream(stream)
            ?.scaledToIcon()
            ?.asImageBitmap()
    }
}.onFailure { Log.w(TAG, "Unable to load custom icon $this", it) }.getOrNull()

private fun Bitmap.scaledToIcon(): Bitmap = scaledToMax(MAX_ICON_PX)

/** Down-samples a media album-art [Bitmap] to a display-friendly [ImageBitmap]. */
fun Bitmap.toArtImageBitmap(): ImageBitmap = scaledToMax(MAX_ART_PX).asImageBitmap()

/**
 * Scales a bitmap down so its longest edge is at most [maxPx], returning it untouched when it is
 * already small enough. Icons and album art arrive at whatever size their app chose, and the island
 * draws them a few dp across.
 */
private fun Bitmap.scaledToMax(maxPx: Int): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= maxPx) return this
    val scale = maxPx.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}
