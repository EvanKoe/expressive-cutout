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

private val Context.timerTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_tile_prefs")

/** The timer tile's own settings, edited on its dedicated settings screen. */
data class TimerTileSettings(
    /** Show the Reset / Add 1 min buttons on the expanded card. */
    val showActions: Boolean = DEFAULT_SHOW_ACTIONS,
    /** Fill of the Reset button. */
    val resetColor: CutoutColor = DEFAULT_RESET_COLOR,
    /** Fill of the "Add 1 min" button. */
    val addButtonColor: CutoutColor = DEFAULT_ADD_BUTTON_COLOR,
) {
    companion object {
        const val DEFAULT_SHOW_ACTIONS = true

        /** Reset is red by default (matches the preset red swatch). */
        val DEFAULT_RESET_COLOR: CutoutColor = CutoutColor.Solid(0xFFEF4444)

        /** Add 1 min follows the Material You primary accent by default. */
        val DEFAULT_ADD_BUTTON_COLOR: CutoutColor = CutoutColor.Dynamic(DynamicRole.PRIMARY)
    }
}

/** Persists the timer tile's display options (action buttons and their colours). */
class TimerTilePreferences(private val context: Context) {

    val settings: Flow<TimerTileSettings> = context.timerTileDataStore.data.map { prefs ->
        TimerTileSettings(
            showActions = prefs[SHOW_ACTIONS] ?: TimerTileSettings.DEFAULT_SHOW_ACTIONS,
            resetColor = CutoutColor.deserialize(prefs[RESET_COLOR])
                ?: TimerTileSettings.DEFAULT_RESET_COLOR,
            addButtonColor = CutoutColor.deserialize(prefs[ADD_BUTTON_COLOR])
                ?: TimerTileSettings.DEFAULT_ADD_BUTTON_COLOR,
        )
    }

    suspend fun setShowActions(enabled: Boolean) = context.timerTileDataStore.edit {
        it[SHOW_ACTIONS] = enabled
    }

    suspend fun setResetColor(color: CutoutColor) = context.timerTileDataStore.edit {
        it[RESET_COLOR] = color.serialize()
    }

    suspend fun setAddButtonColor(color: CutoutColor) = context.timerTileDataStore.edit {
        it[ADD_BUTTON_COLOR] = color.serialize()
    }

    private companion object {
        val SHOW_ACTIONS = booleanPreferencesKey("show_actions")
        val RESET_COLOR = stringPreferencesKey("reset_color")
        val ADD_BUTTON_COLOR = stringPreferencesKey("add_button_color")
    }
}
