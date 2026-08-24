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
 * It drives the same two channels a live call would: [OnCallBus] holds the caller (name, number,
 * ticking duration) the tile reads, and [IslandEventBus] carries the [CutoutSignal.Call] that
 * surfaces it.
 *
 * Two flavours are offered, one per tester button: a [Kind.CONNECTED] call (duration ticking, single
 * hang-up) and a [Kind.INCOMING] ringing call (the number above the name plus decline / answer
 * buttons). Answering an incoming call flips it to a connected one, exactly as a real dialer's re-post
 * would. Tapping a button toggles the call: it starts one when none is showing and ends the current
 * one otherwise, so there is always a way to dismiss it even when the tile's own buttons are turned
 * off. Each fresh call cycles the caller — a short name, a deliberately long one (so the call cutout
 * can be seen widening to fit it, then shrinking back), and a bare number (no name line) — so the
 * cutout's adaptive width can be watched. The tile's buttons act through [TestCallReceiver], mirroring
 * a real dialer's own call actions.
 */
object TestCaller {

    /** Which flavour of fake call a tester button starts. */
    enum class Kind { CONNECTED, INCOMING }

    /** A caller cycled onto the fake call; [number] is null for an unknown caller shown by number. */
    private data class Caller(val label: String, val number: String?)

    private val callers = listOf(
        Caller("Mom", "+1 (555) 867-5309"),
        Caller("Alexandra Wellington-Montgomery", "+1 (555) 013-2288"),
        Caller("+1 (555) 013-2288", null),
    )
    private var next = 0

    /**
     * Starts a fake call, or ends the one already running so the same button both starts and stops
     * it. Cycles through the sample callers so repeated taps exercise different name lengths.
     */
    fun toggle(context: Context, kind: Kind) {
        if (OnCallBus.state.value != null) {
            end()
            return
        }
        val caller = callers[next % callers.size]
        next++
        when (kind) {
            Kind.CONNECTED -> startConnected(context, caller)
            Kind.INCOMING -> startIncoming(context, caller)
        }
    }

    /** A connected call: the duration ticks from now and the tile shows the name + hang-up button. */
    private fun startConnected(context: Context, caller: Caller) {
        OnCallBus.update(
            OnCall(
                callerLabel = caller.label,
                callerNumber = caller.number,
                photo = null,
                startTimeMs = System.currentTimeMillis(),
                ongoing = true,
                packageName = context.packageName,
            ),
        )
        IslandEventBus.emit(
            CutoutSignal.Call(
                packageName = context.packageName,
                callerLabel = caller.label,
                actions = listOf(
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_call_hang_up),
                        intent = broadcast(context, REQUEST_END, TestCallReceiver.ACTION_END),
                    ),
                ),
                ongoing = true,
            ),
        )
    }

    /** An incoming call: still ringing (no duration), so the tile shows the number + name and the
     *  decline / answer buttons. Answering it (below) flips it to a connected call. */
    private fun startIncoming(context: Context, caller: Caller) {
        OnCallBus.update(
            OnCall(
                callerLabel = caller.label,
                callerNumber = caller.number,
                photo = null,
                startTimeMs = null,
                ongoing = false,
                packageName = context.packageName,
            ),
        )
        IslandEventBus.emit(
            CutoutSignal.Call(
                packageName = context.packageName,
                callerLabel = caller.label,
                // Labels drive the tile's decline/answer classification (see IconResolver), so the
                // decline button ends the call and the answer button connects it.
                actions = listOf(
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_call_decline),
                        intent = broadcast(context, REQUEST_END, TestCallReceiver.ACTION_END),
                    ),
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_call_answer),
                        intent = broadcast(context, REQUEST_ANSWER, TestCallReceiver.ACTION_ANSWER),
                    ),
                ),
                ongoing = false,
            ),
        )
    }

    /** Answer the fake incoming call: flip it to a connected call ticking from now, like a real dialer. */
    fun answer(context: Context) {
        val current = OnCallBus.state.value ?: return
        OnCallBus.update(
            current.copy(
                startTimeMs = System.currentTimeMillis(),
                ongoing = true,
                packageName = context.packageName,
            ),
        )
        IslandEventBus.emit(
            CutoutSignal.Call(
                packageName = context.packageName,
                callerLabel = current.callerLabel,
                actions = listOf(
                    CutoutSignal.Notification.Action(
                        title = context.getString(R.string.test_call_hang_up),
                        intent = broadcast(context, REQUEST_END, TestCallReceiver.ACTION_END),
                    ),
                ),
                ongoing = true,
            ),
        )
    }

    /** Clear the fake call so the tile dismisses, exactly as a real dialer's notification removal would. */
    fun end() = OnCallBus.update(null)

    /** Builds the [PendingIntent] behind one of the test call's buttons. */
    private fun broadcast(context: Context, requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, TestCallReceiver::class.java).setAction(action)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        // The actions carry no extras to fill in, so they can be immutable (API 31+ requires a choice).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private const val REQUEST_END = 0
    private const val REQUEST_ANSWER = 1
}
