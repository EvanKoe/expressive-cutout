package com.ekoehler.expressivecutout.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.AppearancePreferences
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.CutoutFill
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.DynamicTilePreferences
import com.ekoehler.expressivecutout.data.EventPreferences
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.data.MusicTilePreferences
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.data.PhoneTilePreferences
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import com.ekoehler.expressivecutout.data.TimerTilePreferences
import com.ekoehler.expressivecutout.data.TimerTileSettings
import com.ekoehler.expressivecutout.data.SwipeDismissDirection
import com.ekoehler.expressivecutout.data.SwipeDismissTarget
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
    private val dynamicTilePreferences = DynamicTilePreferences(application)
    private val musicTilePreferences = MusicTilePreferences(application)
    private val phoneTilePreferences = PhoneTilePreferences(application)
    private val timerTilePreferences = TimerTilePreferences(application)

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

    val eventDynamicColor: StateFlow<Boolean> =
        eventPreferences.dynamicColor.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val eventDynamicColorRole: StateFlow<DynamicRole> =
        eventPreferences.dynamicColorRole.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DynamicRole.PRIMARY,
        )

    val eventDynamicColorOpacity: StateFlow<Float> =
        eventPreferences.dynamicColorOpacity.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 1f,
        )

    /** Per-event cutout-duration overrides; absent events follow the global normal duration. */
    val eventDurations: StateFlow<Map<SystemEventType, Int>> =
        eventPreferences.durations.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val tileEnabled: StateFlow<Map<DynamicTile, Boolean>> =
        dynamicTilePreferences.enabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val musicTile: StateFlow<MusicTileSettings> =
        musicTilePreferences.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MusicTileSettings(),
        )

    val phoneTile: StateFlow<PhoneTileSettings> =
        phoneTilePreferences.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PhoneTileSettings(),
        )

    val timerTile: StateFlow<TimerTileSettings> =
        timerTilePreferences.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerTileSettings(),
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

    fun setEventDynamicColor(enabled: Boolean) = viewModelScope.launch {
        eventPreferences.setDynamicColor(enabled)
    }

    fun setEventDynamicColorRole(role: DynamicRole) = viewModelScope.launch {
        eventPreferences.setDynamicColorRole(role)
    }

    fun setEventDynamicColorOpacity(opacity: Float) = viewModelScope.launch {
        eventPreferences.setDynamicColorOpacity(opacity)
    }

    fun setEventDuration(type: SystemEventType, seconds: Int) = viewModelScope.launch {
        eventPreferences.setDuration(type, seconds)
    }

    fun resetEventDuration(type: SystemEventType) = viewModelScope.launch {
        eventPreferences.clearDuration(type)
    }

    fun setTileEnabled(tile: DynamicTile, enabled: Boolean) = viewModelScope.launch {
        dynamicTilePreferences.setEnabled(tile, enabled)
    }

    fun setMusicShowAlbumArt(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setShowAlbumArt(enabled)
    }

    fun setMusicRotateAlbumArt(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setRotateAlbumArt(enabled)
    }

    fun setMusicShowControls(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setShowControls(enabled)
    }

    fun setPhoneShowPhoto(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setShowPhoto(enabled)
    }

    fun setPhoneShowDuration(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setShowDuration(enabled)
    }

    fun setPhoneShowActions(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setShowActions(enabled)
    }

    fun setPhoneIconContainerColor(color: CutoutColor?) = viewModelScope.launch {
        phoneTilePreferences.setIconContainerColor(color)
    }

    fun setPhoneHangUpColor(color: CutoutColor) = viewModelScope.launch {
        phoneTilePreferences.setHangUpColor(color)
    }

    fun setPhoneOtherButtonColor(color: CutoutColor) = viewModelScope.launch {
        phoneTilePreferences.setOtherButtonColor(color)
    }

    fun setTimerShowActions(enabled: Boolean) = viewModelScope.launch {
        timerTilePreferences.setShowActions(enabled)
    }

    fun setTimerIconContainerColor(color: CutoutColor?) = viewModelScope.launch {
        timerTilePreferences.setIconContainerColor(color)
    }

    fun setTimerResetColor(color: CutoutColor) = viewModelScope.launch {
        timerTilePreferences.setResetColor(color)
    }

    fun setTimerAddButtonColor(color: CutoutColor) = viewModelScope.launch {
        timerTilePreferences.setAddButtonColor(color)
    }

    fun setMusicSkipColor(color: CutoutColor?) = viewModelScope.launch {
        musicTilePreferences.setSkipColor(color)
    }

    fun setMusicSkipOpacity(opacity: Float) = viewModelScope.launch {
        musicTilePreferences.setSkipOpacity(opacity)
    }

    fun setMusicSkipCornerPercent(percent: Int) = viewModelScope.launch {
        musicTilePreferences.setSkipCornerPercent(percent)
    }

    fun setMusicPlayPauseColor(color: CutoutColor?) = viewModelScope.launch {
        musicTilePreferences.setPlayPauseColor(color)
    }

    fun setMusicPlayPauseOpacity(opacity: Float) = viewModelScope.launch {
        musicTilePreferences.setPlayPauseOpacity(opacity)
    }

    fun setMusicPlayPauseCornerPercent(percent: Int) = viewModelScope.launch {
        musicTilePreferences.setPlayPauseCornerPercent(percent)
    }

    fun applyMusicSkipPreset(preset: MusicButtonStyle) = viewModelScope.launch {
        musicTilePreferences.applySkipPreset(preset)
    }

    fun applyMusicPlayPausePreset(preset: MusicButtonStyle) = viewModelScope.launch {
        musicTilePreferences.applyPlayPausePreset(preset)
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

    fun setHideOnLockscreen(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setHideOnLockscreen(enabled)
    }

    fun setAnimationDurationMs(ms: Int) = viewModelScope.launch {
        behaviourPreferences.setAnimationDurationMs(ms)
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

    fun setSwipeToDismiss(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setSwipeToDismiss(enabled)
    }

    fun setSwipeDismissDirection(direction: SwipeDismissDirection) = viewModelScope.launch {
        behaviourPreferences.setSwipeDismissDirection(direction)
    }

    fun setSwipeDismissTarget(target: SwipeDismissTarget) = viewModelScope.launch {
        behaviourPreferences.setSwipeDismissTarget(target)
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
