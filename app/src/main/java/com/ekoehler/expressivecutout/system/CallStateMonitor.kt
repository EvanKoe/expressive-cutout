package com.ekoehler.expressivecutout.system

import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.getSystemService
import com.ekoehler.expressivecutout.permissions.Permissions

/**
 * The platform's own call state, so the phone tile can close the moment a call ends instead of
 * waiting to notice the dialer's notification is gone — and so a dialer that leaves that
 * notification posted after a hang-up can be seen through at all. It needs the optional
 * READ_PHONE_STATE grant: [start] reports whether it could register, leaving the caller to fall
 * back to watching the notification panel when it could not.
 *
 * Registration lasts as long as the service rather than as long as a call, so [isIdle] is always
 * answerable — a stale call notification has to be recognisable on the post that carries it, not
 * only while we happen to be watching a call.
 *
 * Telephony speaks for the dialer's calls alone — a VoIP call in a messaging app never reaches it —
 * so [knowsAbout] says which calls this can be trusted to speak for.
 */
class CallStateMonitor(private val context: Context) {

    /**
     * The registered listener, held as [Any] because its type is version-dependent: a
     * [TelephonyCallback] on Android 12+ and a [PhoneStateListener] below it. Null while stopped.
     */
    private var listener: Any? = null

    /** The last state the platform reported, or null while nothing is registered. */
    private var lastState: Int? = null

    /**
     * True only when the platform has actually told us there is no call. Null-safe by design: an
     * unregistered monitor knows nothing, and must never be read as "the call is over".
     */
    val isIdle: Boolean get() = lastState == TelephonyManager.CALL_STATE_IDLE

    /**
     * Start reporting call state, calling [onCallEnded] on every return to idle, and say whether
     * that worked. False means the grant is missing or the platform refused, and the caller's own
     * fallback stands. Safe to call again — a second call reports the registration already in place,
     * which is how a grant given later is picked up.
     */
    fun start(onCallEnded: () -> Unit): Boolean {
        if (listener != null) return true
        if (!Permissions.isPhoneStateGranted(context)) return false
        val telephony = context.getSystemService<TelephonyManager>() ?: return false

        val registered: Any = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = onState(state, onCallEnded)
            }
        } else {
            @Suppress("DEPRECATION")
            object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                    onState(state, onCallEnded)
            }
        }

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.registerTelephonyCallback(
                    context.mainExecutor,
                    registered as TelephonyCallback,
                )
            } else {
                @Suppress("DEPRECATION")
                telephony.listen(
                    registered as PhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE,
                )
            }
            listener = registered
        }.onFailure { Log.w(TAG, "Call state unavailable; the panel is the only signal", it) }
            .isSuccess
    }

    /** Unregisters and forgets the platform's state, so [isIdle] stops claiming to know it. */
    fun stop() {
        val registered = listener ?: return
        listener = null
        lastState = null
        val telephony = context.getSystemService<TelephonyManager>() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.unregisterTelephonyCallback(registered as TelephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                telephony.listen(registered as PhoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
        }.onFailure { Log.w(TAG, "Failed to drop the call state listener", it) }
    }

    /**
     * A return to idle is the hang-up. Reported unconditionally — including the state replayed at
     * registration — because the caller already ignores an idle that no live call belongs to.
     */
    private fun onState(state: Int, onCallEnded: () -> Unit) {
        lastState = state
        if (state == TelephonyManager.CALL_STATE_IDLE) onCallEnded()
    }

    companion object {
        private const val TAG = "CallStateMonitor"

        /**
         * Whether telephony speaks for a call [packageName] posted. Only the default dialer's calls
         * reach [TelephonyManager]'s state, so a messaging app's VoIP call is left to the panel.
         */
        fun knowsAbout(context: Context, packageName: String?): Boolean {
            if (packageName == null) return false
            val dialer = runCatching { context.getSystemService<TelecomManager>()?.defaultDialerPackage }
                .getOrNull() ?: return false
            return dialer == packageName
        }
    }
}
