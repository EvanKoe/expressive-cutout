package com.ekoehler.expressivecutout.events

import android.app.Notification
import android.app.Person
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.overlay.toImageBitmap

/**
 * Everything the phone tile needs, pulled out of a dialer's ongoing-call notification. Keeps the
 * Android extras plumbing (CallStyle, [Person], chronometer) out of the listener service.
 */
data class ParsedCall(
    val callerLabel: String,
    val photo: ImageBitmap?,
    val startTimeMs: Long?,
    val ongoing: Boolean,
    val actions: List<CutoutSignal.Notification.Action>,
)

/**
 * Recognises and reads call notifications. A call is any notification the dialer marks as
 * [Notification.CATEGORY_CALL] or renders with the [Notification.CallStyle] template. This is the
 * one place that understands the (partly version-gated) call extras, so callers stay simple.
 */
object CallNotificationParser {

    // Values of Notification.EXTRA_CALL_TYPE (API 31+); duplicated as ints so we can read them
    // without gating the whole call on the SDK level.
    private const val CALL_TYPE_INCOMING = 1
    private const val CALL_TYPE_ONGOING = 2

    // Both legacy call notifications and the modern CallStyle template carry this category
    // (CallStyle sets it automatically), so it alone reliably identifies a call notification.
    fun isCall(sbn: StatusBarNotification): Boolean =
        sbn.notification?.category == Notification.CATEGORY_CALL

    fun parse(sbn: StatusBarNotification, context: Context): ParsedCall {
        val notification = sbn.notification
        val extras = notification.extras

        val person = person(extras)
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val callerLabel = person?.name?.toString()?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.phone_caller_unknown)

        val photo = person?.icon?.toImageBitmapOrNull(context)
            ?: notification.getLargeIcon()?.toImageBitmapOrNull(context)

        // A ticking chronometer anchored to the notification's `when` is the dialer telling us the
        // call is connected and how long it has run; a count-down timer isn't call duration.
        val showsChronometer = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true
        val countsDown = extras?.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) == true
        val startTimeMs = notification.`when`.takeIf { showsChronometer && !countsDown && it > 0L }

        val ongoing = when (callType(extras)) {
            CALL_TYPE_INCOMING -> false
            CALL_TYPE_ONGOING -> true
            // No explicit type (older dialers): a running timer means it's connected.
            else -> startTimeMs != null
        }

        val actions = notification.actions.orEmpty().mapNotNull { action ->
            val label = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val intent = action.actionIntent ?: return@mapNotNull null
            CutoutSignal.Notification.Action(label, intent)
        }

        return ParsedCall(callerLabel, photo, startTimeMs, ongoing, actions)
    }

    /** The [Person] a CallStyle notification names as the caller (API 31+), or null. */
    private fun person(extras: android.os.Bundle?): Person? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || extras == null) return null
        @Suppress("DEPRECATION")
        return extras.getParcelable(Notification.EXTRA_CALL_PERSON) as? Person
    }

    /** The CallStyle call type (API 31+), or 0 when unavailable. */
    private fun callType(extras: android.os.Bundle?): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || extras == null) return 0
        return extras.getInt(Notification.EXTRA_CALL_TYPE, 0)
    }

    private fun Icon.toImageBitmapOrNull(context: Context): ImageBitmap? =
        runCatching { loadDrawable(context)?.toImageBitmap() }.getOrNull()
}
