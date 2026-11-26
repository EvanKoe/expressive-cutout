package com.ekoehler.expressivecutout.swe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ekoehler.expressivecutout.overlay.badgeSizeFor

/**
 * The shared background behind a widget: [width] wide and `badgeSizeFor(heightDp)` tall, rounded
 * by [cornerPercent] when alone and joined to its neighbours by [sweBackgroundShape] otherwise.
 * With a null [fill] it draws nothing and only reserves [width]. Every widget whose
 * [SweWidget.hasBackground] can be true draws through this, so their pills always match.
 */
@Composable
internal fun SweBackground(
    fill: Color?,
    width: Dp,
    heightDp: Int,
    cornerPercent: Int = MAX_BADGE_CORNER_PERCENT,
    content: @Composable () -> Unit,
) {
    val modifier = if (fill != null) {
        Modifier
            .size(width, badgeSizeFor(heightDp))
            .background(
                fill,
                sweBackgroundShape(LocalSweGroupPosition.current, heightDp, RoundedCornerShape(cornerPercent)),
            )
    } else {
        Modifier.width(width)
    }

    // Optional background
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()
    }
}
