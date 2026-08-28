package com.ekoehler.expressivecutout.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.ekoehler.expressivecutout.data.PageTransitionStyle

/** Builds the configured Material transition for a page change. */
internal fun pageTransition(
    style: PageTransitionStyle,
    direction: Int = 1,
): ContentTransform {
    val enter: EnterTransition
    val exit: ExitTransition
    when (style) {
        PageTransitionStyle.FADE -> {
            enter = fadeIn(tween(PAGE_TRANSITION_DURATION_MS))
            exit = fadeOut(tween(PAGE_TRANSITION_DURATION_MS))
        }

        PageTransitionStyle.SLIDE -> {
            enter = slideInHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width -> direction * width }
            exit = slideOutHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width -> -direction * width }
        }
    }
    return enter togetherWith exit
}

/** Duration shared by the selectable page transitions. */
private const val PAGE_TRANSITION_DURATION_MS = 300
