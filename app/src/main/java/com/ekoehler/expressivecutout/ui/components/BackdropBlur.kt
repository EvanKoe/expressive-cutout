package com.ekoehler.expressivecutout.ui.components

import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/**
 * Fills this element with [color] laid over a blur of everything drawn behind it — a frosted-glass
 * background. The blur comes from [RenderNode.setBackdropRenderEffect], which only exists from
 * Android 17 QPR2 (API 37.2); older versions, and software canvases, get the plain [color].
 *
 * Clipping to a rounded shape is left to the parent (e.g. a `Surface`). No ancestor may draw into
 * an offscreen layer (`alpha` < 1, `blur`, a `RenderEffect`), or the backdrop it sees is empty.
 */
fun Modifier.backdropBlur(color: Color, radius: Dp): Modifier = drawWithCache {
    val blurNode = if (supportsBackdropBlur()) {
        RenderNode("backdropBlur").apply {
            setPosition(0, 0, size.width.toInt(), size.height.toInt())
            setBackdropRenderEffect(
                RenderEffect.createBlurEffect(radius.toPx(), radius.toPx(), Shader.TileMode.CLAMP),
            )
            val canvas = beginRecording()
            canvas.drawColor(color.toArgb())
            endRecording()
        }
    } else {
        null
    }

    onDrawBehind {
        val canvas = drawContext.canvas.nativeCanvas
        if (blurNode != null && canvas.isHardwareAccelerated) {
            canvas.drawRenderNode(blurNode)
        } else {
            drawRect(color)
        }
    }
}

/** Whether this device's framework has [RenderNode.setBackdropRenderEffect] (API 37.2+). */
internal fun supportsBackdropBlur(): Boolean =
    // SDK_INT_FULL only exists from API 36, so the major-level check must short-circuit first
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
        Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2
