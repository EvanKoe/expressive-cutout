package com.ekoehler.expressivecutout.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.AppearancePreferences
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.CutoutFill
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
    private val appearancePreferences = AppearancePreferences(application)
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

    val musicEnabled: StateFlow<Boolean> =
        eventPreferences.musicEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
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

    val appearance: StateFlow<AppearanceSettings> =
        appearancePreferences.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppearanceSettings(),
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

    fun setMusicEnabled(enabled: Boolean) = viewModelScope.launch {
        eventPreferences.setMusicEnabled(enabled)
    }

    fun setCollapsedDimensions(dimensions: IslandDimensions) = viewModelScope.launch {
        layoutPreferences.setCollapsed(dimensions)
    }

    fun setExpandedDimensions(dimensions: IslandDimensions) = viewModelScope.launch {
        layoutPreferences.setExpanded(dimensions)
    }

    fun resetLayout() = viewModelScope.launch { layoutPreferences.reset() }

    fun setTheme(theme: AppTheme) = viewModelScope.launch { themePreferences.setTheme(theme) }

    fun setCutoutEnabled(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setCutoutEnabled(enabled)
    }

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

    fun setShowActionButtons(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setShowActionButtons(enabled)
    }

    fun setShrinkOnSwipeUp(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setShrinkOnSwipeUp(enabled)
    }

    fun setShadowEnabled(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setShadowEnabled(enabled)
    }

    fun setStrokeEnabled(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setStrokeEnabled(enabled)
    }

    fun setStrokeWidth(widthDp: Int) = viewModelScope.launch {
        appearancePreferences.setStrokeWidth(widthDp)
    }

    fun setStrokeColor(color: CutoutColor) = viewModelScope.launch {
        appearancePreferences.setStrokeColor(color)
    }

    fun setBackgroundNormal(fill: CutoutFill) = viewModelScope.launch {
        appearancePreferences.setBackgroundNormal(fill)
    }

    fun setBackgroundExpanded(fill: CutoutFill) = viewModelScope.launch {
        appearancePreferences.setBackgroundExpanded(fill)
    }

    fun setSendButtonColor(color: CutoutColor?) = viewModelScope.launch {
        appearancePreferences.setSendButtonColor(color)
    }

    fun setCancelButtonColor(color: CutoutColor?) = viewModelScope.launch {
        appearancePreferences.setCancelButtonColor(color)
    }

    fun setActionButtonStyle(style: ActionButtonStyle) = viewModelScope.launch {
        appearancePreferences.setActionButtonStyle(style)
    }

    fun setActionButtonColor(color: CutoutColor?) = viewModelScope.launch {
        appearancePreferences.setActionButtonColor(color)
    }

    fun setActionButtonHeight(heightDp: Int) = viewModelScope.launch {
        appearancePreferences.setActionButtonHeight(heightDp)
    }

    fun setReplyInputStyle(style: ReplyInputStyle) = viewModelScope.launch {
        appearancePreferences.setReplyInputStyle(style)
    }

    fun setCancelButtonOnLeft(onLeft: Boolean) = viewModelScope.launch {
        appearancePreferences.setCancelButtonOnLeft(onLeft)
    }
}
