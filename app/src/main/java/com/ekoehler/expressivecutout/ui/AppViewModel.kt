package com.ekoehler.expressivecutout.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.EventPreferences
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandDimensions
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
    private val behaviourPreferences = BehaviourPreferences(application)
    private val eventPreferences = EventPreferences(application)

    val customIcons: StateFlow<Map<SystemEventType, IconSource>> =
        preferences.customIcons.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val eventEnabled: StateFlow<Map<SystemEventType, Boolean>> =
        eventPreferences.enabled.stateIn(
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

    val behaviour: StateFlow<BehaviourSettings> =
        behaviourPreferences.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BehaviourSettings(),
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

    fun setEventEnabled(type: SystemEventType, enabled: Boolean) = viewModelScope.launch {
        eventPreferences.setEnabled(type, enabled)
    }

    fun setCollapsedDimensions(dimensions: IslandDimensions) = viewModelScope.launch {
        layoutPreferences.setCollapsed(dimensions)
    }

    fun setExpandedDimensions(dimensions: IslandDimensions) = viewModelScope.launch {
        layoutPreferences.setExpanded(dimensions)
    }

    fun resetLayout() = viewModelScope.launch { layoutPreferences.reset() }

    fun setTheme(theme: AppTheme) = viewModelScope.launch { themePreferences.setTheme(theme) }

    fun setNormalDurationSeconds(seconds: Int) = viewModelScope.launch {
        behaviourPreferences.setNormalDurationSeconds(seconds)
    }

    fun setExpandedAutoCollapse(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setAutoCollapse(enabled)
    }

    fun setExpandedCollapseSeconds(seconds: Int) = viewModelScope.launch {
        behaviourPreferences.setCollapseSeconds(seconds)
    }

    fun setExpandedDisappearOnShrink(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setDisappearOnShrink(enabled)
    }

    fun setNotificationsAutoExpand(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setNotificationsAutoExpand(enabled)
    }
}
