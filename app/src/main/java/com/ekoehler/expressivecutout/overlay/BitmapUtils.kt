package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

private const val TAG = "BitmapUtils"
private const val MAX_ICON_PX = 128

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

private fun Bitmap.scaledToIcon(): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= MAX_ICON_PX) return this
    val scale = MAX_ICON_PX.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}
