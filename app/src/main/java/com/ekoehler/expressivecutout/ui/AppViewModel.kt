package com.ekoehler.expressivecutout.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.data.ThemePreferences
import com.ekoehler.expressivecutout.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds UI-facing state for the icon customisation screen and mediates writes to
 * [IconPreferences]. Using an [AndroidViewModel] keeps the DataStore off the composition
 * and survives configuration changes.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = IconPreferences(application)
    private val layoutPreferences = LayoutPreferences(application)
    private val themePreferences = ThemePreferences(application)

    val customIcons: StateFlow<Map<SystemEventType, IconSource>> =
        preferences.customIcons.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val layout: StateFlow<IslandLayout> =
        layoutPreferences.layout.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IslandLayout.DEFAULT,
        )

    val theme: StateFlow<AppTheme> =
        themePreferences.theme.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.SYSTEM,
        )

    fun setImageIcon(type: SystemEventType, uri: String) = viewModelScope.launch {
        preferences.setIcon(type, IconSource.Image(uri))
    }

    fun setAppIcon(type: SystemEventType, packageName: String) = viewModelScope.launch {
        preferences.setIcon(type, IconSource.App(packageName))
    }

    fun resetIcon(type: SystemEventType) = viewModelScope.launch {
        preferences.clearIcon(type)
    }

    fun setWidth(widthDp: Int) = viewModelScope.launch { layoutPreferences.setWidth(widthDp) }

    fun setHeight(heightDp: Int) = viewModelScope.launch { layoutPreferences.setHeight(heightDp) }

    fun setOffsetX(offsetXDp: Int) = viewModelScope.launch { layoutPreferences.setOffsetX(offsetXDp) }

    fun setOffsetY(offsetYDp: Int) = viewModelScope.launch { layoutPreferences.setOffsetY(offsetYDp) }

    fun resetLayout() = viewModelScope.launch { layoutPreferences.reset() }

    fun setTheme(theme: AppTheme) = viewModelScope.launch { themePreferences.setTheme(theme) }
}
