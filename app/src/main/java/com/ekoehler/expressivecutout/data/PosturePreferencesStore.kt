package com.ekoehler.expressivecutout.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Selects one of two preference stores for reads and writes, using the device posture at runtime or
 * the user's selected editing posture in settings.
 */
class PosturePreferencesStore(
    private val openStore: DataStore<Preferences>,
    private val closedStore: DataStore<Preferences>,
    private val isClosed: Flow<Boolean>,
) {
    /** Emits the selected posture's preferences and updates when the posture selection changes. */
    val data: Flow<Preferences> = combine(openStore.data, closedStore.data, isClosed) { open, closed, useClosed ->
        if (useClosed) closed else open
    }.distinctUntilChanged()

    /** Applies a preference edit to the currently selected posture. */
    suspend fun edit(transform: suspend (MutablePreferences) -> Unit) {
        val store = if (isClosed.first()) closedStore else openStore
        store.edit(transform)
    }
}
