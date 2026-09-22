package com.ekoehler.expressivecutout.core

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The in-call audio route behind the expanded phone tile's Mute and Speaker buttons. Dialers expose
 * neither as a notification action, so the two toggles drive [AudioManager] directly and this bus
 * holds what the platform last reported so the buttons can light up while they are on. Deliberately
 * re-read from the platform on every call rather than cached: the dialer's own in-call screen
 * changes both behind our back. Sits beside [OnCallBus], which holds who the call is with.
 */
object CallAudioBus {

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

    /** Re-reads the platform's microphone-mute and speakerphone state into the flows. */
    fun refresh(context: Context) {
        val audio = context.getSystemService<AudioManager>() ?: return
        _muted.value = audio.isMicrophoneMute
        _speakerOn.value = audio.speakerOn
    }

    /** Flips the microphone mute, then re-reads it so the button shows what actually took effect. */
    fun toggleMute(context: Context) {
        val audio = context.getSystemService<AudioManager>() ?: return
        runCatching { audio.isMicrophoneMute = !audio.isMicrophoneMute }
        _muted.value = audio.isMicrophoneMute
    }

    /** Routes the call to the built-in speaker, or back to the default device, then re-reads it. */
    fun toggleSpeaker(context: Context) {
        val audio = context.getSystemService<AudioManager>() ?: return
        runCatching { audio.speakerOn = !audio.speakerOn }
        _speakerOn.value = audio.speakerOn
    }

    /**
     * Speakerphone through whichever API the running platform supports: the communication-device
     * routing added in Android 12, and the long-deprecated flag on everything below it.
     */
    private var AudioManager.speakerOn: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            isSpeakerphoneOn
        }
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (value) {
                    availableCommunicationDevices
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        ?.let { setCommunicationDevice(it) }
                } else {
                    clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                isSpeakerphoneOn = value
            }
        }
}
