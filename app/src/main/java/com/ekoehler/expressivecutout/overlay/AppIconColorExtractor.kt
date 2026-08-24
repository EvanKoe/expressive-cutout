package com.ekoehler.expressivecutout.overlay

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap

/**
 * Extracts the primary branding color from an application's default launcher icon.
 * Results are cached by package name to avoid redundant icon decoding and palette analysis.
 */
object AppIconColorExtractor {
    private val colorCache = LruCache<String, Int>(64)

    /**
     * Retrieves the primary color for [packageName], or null if it cannot be resolved.
     */
    fun extractAppColor(context: Context, packageName: String): Color? {
        val cached = colorCache.get(packageName)
        if (cached != null) {
            return Color(cached)
        }

        val drawable = getPlainAppIcon(context, packageName) ?: return null
        val bitmap = drawableToBitmap(drawable, 48, 48)
        val extractedInt = extractDominantColor(bitmap) ?: return null
        colorCache.put(packageName, extractedInt)
        return Color(extractedInt)
    }

    /**
     * Fetches the plain default launcher icon for an application, avoiding themed/dynamic icon tinting.
     */
    private fun getPlainAppIcon(context: Context, packageName: String): Drawable? = runCatching {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(launchIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                pm.resolveActivity(launchIntent, 0)
            }
            val icon = resolveInfo?.activityInfo?.loadIcon(pm)
            if (icon != null) return@runCatching icon
        }
        pm.getApplicationIcon(packageName)
    }.getOrNull()

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width == width && src.height == height) return src
            return Bitmap.createScaledBitmap(src, width, height, true)
        }
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Extracts the primary vibrant/dominant branding color from a [Bitmap].
     * Saturated, vivid colors are scored higher than neutral/dark/white background pixels.
     */
    private fun extractDominantColor(bitmap: Bitmap): Int? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var bestColor: Int? = null
        var highestScore = -1f

        // Count color occurrences bucketed by quantized RGB (4-bits per channel -> 4096 bins)
        val buckets = mutableMapOf<Int, Int>()
        val representativeColors = mutableMapOf<Int, LongArray>() // [rSum, gSum, bSum]

        val hsv = FloatArray(3)
        for (pixel in pixels) {
            val alpha = AndroidColor.alpha(pixel)
            if (alpha < 100) continue // Skip transparent pixels

            val r = AndroidColor.red(pixel)
            val g = AndroidColor.green(pixel)
            val b = AndroidColor.blue(pixel)

            val qr = (r shr 4)
            val qg = (g shr 4)
            val qb = (b shr 4)
            val key = (qr shl 8) or (qg shl 4) or qb

            buckets[key] = (buckets[key] ?: 0) + 1
            val sums = representativeColors.getOrPut(key) { LongArray(3) }
            sums[0] += r.toLong()
            sums[1] += g.toLong()
            sums[2] += b.toLong()
        }

        if (buckets.isEmpty()) return null

        for ((key, count) in buckets) {
            val sums = representativeColors[key] ?: continue
            val r = (sums[0] / count).toInt().coerceIn(0, 255)
            val g = (sums[1] / count).toInt().coerceIn(0, 255)
            val b = (sums[2] / count).toInt().coerceIn(0, 255)

            AndroidColor.RGBToHSV(r, g, b, hsv)
            val saturation = hsv[1] // 0.0 .. 1.0
            val value = hsv[2] // 0.0 .. 1.0

            // Score favoring saturated, well-lit colors over dull grays/whites/blacks
            val saturationWeight = saturation * saturation * 4.0f + 0.1f
            val brightnessWeight = when {
                value < 0.15f -> 0.1f // too dark
                value > 0.95f && saturation < 0.15f -> 0.1f // plain white/near white
                else -> 1.0f
            }

            val score = count * saturationWeight * brightnessWeight
            if (score > highestScore) {
                highestScore = score
                bestColor = AndroidColor.rgb(r, g, b)
            }
        }

        return bestColor
    }
}
