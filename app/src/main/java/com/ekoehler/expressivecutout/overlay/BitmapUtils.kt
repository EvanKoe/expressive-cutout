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

private const val TAG = "BitmapUtils"
private const val MAX_ICON_PX = 128
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

private fun Bitmap.scaledToMax(maxPx: Int): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= maxPx) return this
    val scale = maxPx.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}
