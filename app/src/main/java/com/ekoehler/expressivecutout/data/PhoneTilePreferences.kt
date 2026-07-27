package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.phoneTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "phone_tile_prefs")

/** The phone tile's own settings, edited on its dedicated settings screen. */
data class PhoneTileSettings(
    /** Show the caller's contact photo on the collapsed pill and expanded card. */
    val showPhoto: Boolean = DEFAULT_SHOW_PHOTO,
    /** Show the ticking call duration on the expanded card. */
    val showDuration: Boolean = DEFAULT_SHOW_DURATION,
    /** Show the call's action buttons (Hang up, and any others the dialer exposes). */
    val showActions: Boolean = DEFAULT_SHOW_ACTIONS,
    /** Fill of the hang-up / end-call button. */
    val hangUpColor: CutoutColor = DEFAULT_HANG_UP_COLOR,
    /** Fill shared by every other call button (answer, mute, speaker, …). */
    val otherButtonColor: CutoutColor = DEFAULT_OTHER_BUTTON_COLOR,
) {
    companion object {
        const val DEFAULT_SHOW_PHOTO = true
        const val DEFAULT_SHOW_DURATION = true
        const val DEFAULT_SHOW_ACTIONS = true

        /** Hang up is red by default (matches the preset red swatch). */
        val DEFAULT_HANG_UP_COLOR: CutoutColor = CutoutColor.Solid(0xFFEF4444)

        /** Every other button follows the Material You primary accent by default. */
        val DEFAULT_OTHER_BUTTON_COLOR: CutoutColor = CutoutColor.Dynamic(DynamicRole.PRIMARY)
    }
}

/** Persists the phone tile's display options (contact photo, duration, action buttons). */
class PhoneTilePreferences(private val context: Context) {

    val settings: Flow<PhoneTileSettings> = context.phoneTileDataStore.data.map { prefs ->
        PhoneTileSettings(
            showPhoto = prefs[SHOW_PHOTO] ?: PhoneTileSettings.DEFAULT_SHOW_PHOTO,
            showDuration = prefs[SHOW_DURATION] ?: PhoneTileSettings.DEFAULT_SHOW_DURATION,
            showActions = prefs[SHOW_ACTIONS] ?: PhoneTileSettings.DEFAULT_SHOW_ACTIONS,
            hangUpColor = CutoutColor.deserialize(prefs[HANG_UP_COLOR])
                ?: PhoneTileSettings.DEFAULT_HANG_UP_COLOR,
            otherButtonColor = CutoutColor.deserialize(prefs[OTHER_BUTTON_COLOR])
                ?: PhoneTileSettings.DEFAULT_OTHER_BUTTON_COLOR,
        )
    }

    suspend fun setShowPhoto(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_PHOTO] = enabled
    }

    suspend fun setShowDuration(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_DURATION] = enabled
    }

    suspend fun setShowActions(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_ACTIONS] = enabled
    }

    suspend fun setHangUpColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[HANG_UP_COLOR] = color.serialize()
    }

    suspend fun setOtherButtonColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[OTHER_BUTTON_COLOR] = color.serialize()
    }

    private companion object {
        val SHOW_PHOTO = booleanPreferencesKey("show_photo")
        val SHOW_DURATION = booleanPreferencesKey("show_duration")
        val SHOW_ACTIONS = booleanPreferencesKey("show_actions")
        val HANG_UP_COLOR = stringPreferencesKey("hang_up_color")
        val OTHER_BUTTON_COLOR = stringPreferencesKey("other_button_color")
    }
}
