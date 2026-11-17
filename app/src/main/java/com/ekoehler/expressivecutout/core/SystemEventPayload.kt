package com.ekoehler.expressivecutout.core

import android.graphics.Bitmap

/**
 * Rich domain payload describing a device-level system event, carrying dynamic metadata,
 * collapsed badge text, secondary detail lines, and optional action intent routing.
 */
data class SystemEventPayload(
    val type: SystemEventType,
    val title: String? = null,
    val subtitle: String? = null,
    val collapsedBadgeText: String? = null,
    val secondaryLines: List<String> = emptyList(),
    val actionIntentAction: String? = null,
    val actionIntentUri: String? = null,
    val vectorIconName: String? = null,
    val iconBitmap: Bitmap? = null,
)
