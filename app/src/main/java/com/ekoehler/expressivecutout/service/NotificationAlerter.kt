package com.ekoehler.expressivecutout.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Plays the ring and the buzz a notification would have made, for the island to use when it takes
 * one over.
 *
 * Holding a notification back from the shade cancels it, and the framework stops whatever sound or
 * vibration is playing for a notification it cancels — so an alert that began the instant the
 * notification posted is cut off a few milliseconds later, well before it registers as anything.
 * This puts it back as the pill appears, drawn from the notification's own channel so an app still
 * sounds like itself.
 */
class NotificationAlerter(private val context: Context) {

    private val audioManager by lazy { context.getSystemService(AudioManager::class.java) }

    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    // The sound currently playing, so a notification landing on the heels of another replaces its
    // ring rather than layering over it — as the framework does with its own.
    private var playing: Ringtone? = null

    /**
     * Sound and buzz for a notification on [channel], following the ringer switch the way the shade
     * would: silent stays silent, vibrate buzzes without a ring, and only a phone free to make noise
     * makes any. A channel too quiet to have alerted at all ([importance] below
     * [NotificationManager.IMPORTANCE_DEFAULT], or asking for neither a sound nor a vibration) is
     * left alone — the island is standing in for the alert, not inventing one.
     *
     * Blocking: opens the ringtone. Call it off the main thread.
     */
    fun alert(channel: NotificationChannel?, importance: Int) {
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) return
        val ringerMode = audioManager?.ringerMode ?: return
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) return

        // A null channel means the framework told us nothing about how this one should sound, so it
        // gets the phone's default notification treatment rather than being dropped.
        val wantsSound = channel == null || channel.sound != null
        val wantsBuzz = channel == null || channel.shouldVibrate()
        if (!wantsSound && !wantsBuzz) return

        // On vibrate, a notification that would have rung buzzes instead, whatever its channel asked
        // for — that is the whole point of the switch.
        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            buzz(channel)
            return
        }
        if (wantsSound) {
            play(
                channel?.sound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                channel?.audioAttributes,
            )
        }
        if (wantsBuzz) buzz(channel)
    }

    /** Stop a ring still playing, so nothing outlives the island that started it. */
    @Synchronized
    fun stop() {
        runCatching { playing?.takeIf { it.isPlaying }?.stop() }
            .onFailure { Log.w(TAG, "Failed to stop notification sound", it) }
        playing = null
    }

    @Synchronized
    private fun play(uri: Uri?, attributes: AudioAttributes?) {
        val sound = uri ?: return
        stop()
        runCatching {
            val ringtone = RingtoneManager.getRingtone(context, sound) ?: return
            ringtone.audioAttributes = attributes ?: NOTIFICATION_ATTRIBUTES
            ringtone.play()
            playing = ringtone
        }.onFailure { Log.w(TAG, "Failed to play notification sound", it) }
    }

    /**
     * Buzz with the channel's own pattern where it has one. Most channels don't — they take the
     * phone's default vibration, which isn't ours to read — so those get a crisp double tap, which is
     * what a notification feels like on modern hardware.
     */
    private fun buzz(channel: NotificationChannel?) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val pattern = channel?.vibrationPattern
        val effect = if (pattern != null && pattern.isNotEmpty()) {
            VibrationEffect.createWaveform(pattern, NO_REPEAT)
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, NOTIFICATION_ATTRIBUTES)
            }
        }.onFailure { Log.w(TAG, "Failed to vibrate", it) }
    }

    private companion object {
        const val TAG = "NotificationAlerter"

        // Play the waveform once through rather than looping it.
        const val NO_REPEAT = -1

        // What the framework would have used for a notification, so the ring lands on the
        // notification volume and ducks against media the same way.
        val NOTIFICATION_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
    }
}
