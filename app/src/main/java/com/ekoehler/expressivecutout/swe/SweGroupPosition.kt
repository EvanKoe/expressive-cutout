package com.ekoehler.expressivecutout.swe

import androidx.compose.runtime.compositionLocalOf

/**
 * Where a background widget sits in a run of adjacent background widgets on one side of the pill.
 * [SINGLE] means it has no background neighbour.
 */
enum class SweGroupPosition { SINGLE, START, MIDDLE, END }

/**
 * The [SweGroupPosition] of the widget being drawn. Provided around each widget's `Content`;
 * callers that provide nothing get [SweGroupPosition.SINGLE], i.e. the widget's own corners.
 */
val LocalSweGroupPosition = compositionLocalOf { SweGroupPosition.SINGLE }
