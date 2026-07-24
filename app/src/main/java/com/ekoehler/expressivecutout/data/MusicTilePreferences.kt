package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.musicTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "music_tile_prefs")

/** The music tile's own settings, edited on its dedicated settings screen. */
data class MusicTileSettings(
    val showAlbumArt: Boolean = DEFAULT_SHOW_ALBUM_ART,
    val showControls: Boolean = DEFAULT_SHOW_CONTROLS,
) {
    companion object {
        const val DEFAULT_SHOW_ALBUM_ART = true
        const val DEFAULT_SHOW_CONTROLS = true
    }
}

/** Persists the music tile's display options (album art on the normal cutout, expanded controls). */
class MusicTilePreferences(private val context: Context) {

    val settings: Flow<MusicTileSettings> = context.musicTileDataStore.data.map { prefs ->
        MusicTileSettings(
            showAlbumArt = prefs[SHOW_ALBUM_ART] ?: MusicTileSettings.DEFAULT_SHOW_ALBUM_ART,
            showControls = prefs[SHOW_CONTROLS] ?: MusicTileSettings.DEFAULT_SHOW_CONTROLS,
        )
    }

    suspend fun setShowAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_ALBUM_ART] = enabled
    }

    suspend fun setShowControls(enabled: Boolean) = context.musicTileDataStore.edit {
        it[SHOW_CONTROLS] = enabled
    }

    private companion object {
        val SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls")
    }
}
