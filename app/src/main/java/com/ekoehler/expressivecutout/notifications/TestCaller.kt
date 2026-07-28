package com.ekoehler.expressivecutout.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.core.OnCall
import com.ekoehler.expressivecutout.core.OnCallBus

/**
 * Pops a fake phone call onto the island so the phone tile can be tried without placing a real call.
 * It drives the same two channels a live call would: [OnCallBus] holds the caller (name, ticking
 * duration) the tile reads, and [IslandEventBus] carries the [CutoutSignal.Call] that surfaces it.
 *
 * Tapping toggles the call: it starts one when none is showing and ends the current one otherwise,
 * so there is always a way to dismiss it even when the tile's own hang-up button is turned off. Each
 * fresh call cycles the caller name — a short one, a deliberately long one (so the call cutout can be
 * seen widening to fit it, then shrinking back), and a bare number. The cutout's hang-up button ends
 * the call through [TestCallReceiver], mirroring a real dialer removing its ongoing-call notification.
 */
object TestCaller {

    // Cycled on each fresh call so the tester can watch the cutout adapt its width to the caller name.
    private val callers = listOf(
        "Mom",
        "Alexandra Wellington-Montgomery",
        "+1 (555) 013-2288",
    )
    private var next = 0

    fun toggle(context: Context) {
        if (OnCallBus.state.value != null) {
            end()
            return
        }
        val label = callers[next % callers.size]
        next++
        // A connected call: the duration ticks from now and the tile shows the name + hang-up button.
        OnCallBus.update(
            OnCall(
                callerLabel = label,
                callerNumber = null,
                photo = null,
                startTimeMs = System.currentTimeMillis(),
                ongoing = true,
            ),
        )
        IslandEventBus.emit(
            CutoutSignal.Call(
                packageName = context.packageName,
                callerLabel = label,
                actions = listOf(
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_call_hang_up),
                        intent = endCallIntent(context),
                    ),
                ),
                ongoing = true,
            ),
        )
    }

    /** Clear the fake call so the tile dismisses, exactly as a real dialer's notification removal would. */
    fun end() = OnCallBus.update(null)

    private fun endCallIntent(context: Context): PendingIntent {
        val intent = Intent(context, TestCallReceiver::class.java).setAction(TestCallReceiver.ACTION_END)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        // The end action carries no extras to fill in, so it can be immutable (API 31+ requires a choice).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
