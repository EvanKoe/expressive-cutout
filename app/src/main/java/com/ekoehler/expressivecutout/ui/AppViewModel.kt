package com.ekoehler.expressivecutout.ui

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.ActionButtonAlignment
import com.ekoehler.expressivecutout.data.ActionButtonStyle
import com.ekoehler.expressivecutout.data.ActionButtonAnimation
import com.ekoehler.expressivecutout.data.AnimationBounce
import com.ekoehler.expressivecutout.data.AnimationSpeed
import com.ekoehler.expressivecutout.data.AnimationStyle
import com.ekoehler.expressivecutout.data.AppPreferences
import com.ekoehler.expressivecutout.data.AppearancePreferences
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.ReplyInputStyle
import com.ekoehler.expressivecutout.data.SentAlignment
import com.ekoehler.expressivecutout.data.AssistantTilePreferences
import com.ekoehler.expressivecutout.data.AssistantTileSettings
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.BehaviourSettings
import com.ekoehler.expressivecutout.data.CenterShortcut
import com.ekoehler.expressivecutout.data.HorizontalCutoutMode
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.CutoutFill
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.DynamicTilePreferences
import com.ekoehler.expressivecutout.data.EmptyClickAction
import com.ekoehler.expressivecutout.data.EventPreferences
import com.ekoehler.expressivecutout.data.IconPreferences
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.JsonSerializable
import com.ekoehler.expressivecutout.data.JsonSettings
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.data.MusicTilePreferences
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.data.PhoneTilePreferences
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import com.ekoehler.expressivecutout.data.RecentColorPreferences
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import com.ekoehler.expressivecutout.data.TimerTilePreferences
import com.ekoehler.expressivecutout.data.TimerTileSettings
import com.ekoehler.expressivecutout.data.SwipeDismissDirection
import com.ekoehler.expressivecutout.data.SwipeDismissTarget
import com.ekoehler.expressivecutout.data.ThemePreferences
import com.ekoehler.expressivecutout.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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
    private val assistantTilePreferences = AssistantTilePreferences(application)
    private val appPreferences = AppPreferences(application)
    private val recentColorPreferences = RecentColorPreferences(application)
    private val statusBarPreferences = StatusBarPreferences(application)

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

    /** For events with a Lottie animation: whether it's used (absent = on) and whether it loops. */
    val eventAnimatedIcons: StateFlow<Map<SystemEventType, Boolean>> =
        eventPreferences.animatedIcons.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val eventAnimatedIconLoops: StateFlow<Map<SystemEventType, Boolean>> =
        eventPreferences.animatedIconLoops.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    /** Per-event colour overrides; absent events follow their default accent (or the dynamic role). */
    val eventColors: StateFlow<Map<SystemEventType, CutoutColor>> =
        eventPreferences.colors.stateIn(
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

    /** Packages the user muted on the Apps screen; everything else is allowed on the cutout. */
    val disabledApps: StateFlow<Set<String>> =
        appPreferences.disabledPackages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    /** Packages allowed on the cutout but never allowed to auto-expand it. */
    val normalOnlyApps: StateFlow<Set<String>> =
        appPreferences.normalOnlyPackages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
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

    val assistantTile: StateFlow<AssistantTileSettings> =
        assistantTilePreferences.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssistantTileSettings(),
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

    /**
     * Every settings store keyed by its section label, in the order they're written to and read
     * from the export document. This single list is the source of truth for both export and import —
     * adding a new store is one extra line here (and its own [JsonSerializable] implementation).
     */
    private val jsonSections: Map<String, JsonSerializable> = mapOf(
        JsonSettings.THEME to themePreferences,
        JsonSettings.LAYOUT to layoutPreferences,
        JsonSettings.ICONS to preferences,
        JsonSettings.BEHAVIOUR to behaviourPreferences,
        JsonSettings.APPEARANCE to appearancePreferences,
        JsonSettings.EVENTS to eventPreferences,
        JsonSettings.DYNAMIC_TILES to dynamicTilePreferences,
        JsonSettings.MUSIC_TILE to musicTilePreferences,
        JsonSettings.PHONE_TILE to phoneTilePreferences,
        JsonSettings.TIMER_TILE to timerTilePreferences,
        JsonSettings.ASSISTANT_TILE to assistantTilePreferences,
        JsonSettings.APPS to appPreferences,
        JsonSettings.RECENT_COLORS to recentColorPreferences,
        JsonSettings.STATUS_BAR to statusBarPreferences,
    )

    /** Exports every settings store as one JSON document; see [JsonSettings.export]. */
    suspend fun getSettingsAsJsonString(): String = JsonSettings.export(jsonSections)

    /**
     * This method uses exportSettingsToJson() but is not suspend and
     * can be called from the UI.
     * @param onReady - a callback that returns a SUCCESS (boolean) and a PATH (string)
     */
    fun exportSettingsFromUI(onReady: (success: Boolean, path: String?) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val json = getSettingsAsJsonString()
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "expressive-cutout-settings.json")
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val resolver = getApplication<Application>().contentResolver
                    val collection =
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val uri =
                        resolver.insert(collection, values) ?: throw IOException("Failed to insert")

                    resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } catch (e: Exception) {
                    Log.w("Error", "starting export ${e.message}")
                    false
                }
            }

            onReady(ok, if (ok) Environment.DIRECTORY_DOWNLOADS else null)
        }
    }

    /**
     * Reads the JSON file at [uri] (picked via the system file selector), validates that it's an
     * Expressive Cutout settings export, and applies it across the stores. Not suspend so it can be
     * called straight from the UI; the file read and apply run off the main thread.
     * @param onDone reports the [JsonSettings.ImportResult] so the caller can show feedback.
     */
    fun importSettingsFromUI(uri: Uri, onDone: (JsonSettings.ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return@withContext JsonSettings.ImportResult.ERROR
                    JsonSettings.import(json, jsonSections)
                } catch (e: Exception) {
                    Log.w("Error", "starting import ${e.message}")
                    JsonSettings.ImportResult.ERROR
                }
            }
            onDone(result)
        }
    }

    fun setImageIcon(type: SystemEventType, uri: String) = viewModelScope.launch {
        preferences.setIcon(type, IconSource.Image(uri))
    }

    fun setMaterialIcon(type: SystemEventType, iconName: String) = viewModelScope.launch {
        preferences.setIcon(type, IconSource.Material(iconName))
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

    fun setEventColor(type: SystemEventType, color: CutoutColor) = viewModelScope.launch {
        eventPreferences.setColor(type, color)
    }

    fun resetEventColor(type: SystemEventType) = viewModelScope.launch {
        eventPreferences.clearColor(type)
    }

    fun setEventAnimatedIcon(type: SystemEventType, enabled: Boolean) = viewModelScope.launch {
        eventPreferences.setAnimatedIcon(type, enabled)
    }

    fun setEventAnimatedIconLoop(type: SystemEventType, loop: Boolean) = viewModelScope.launch {
        eventPreferences.setAnimatedIconLoop(type, loop)
    }

    fun setTileEnabled(tile: DynamicTile, enabled: Boolean) = viewModelScope.launch {
        dynamicTilePreferences.setEnabled(tile, enabled)
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) = viewModelScope.launch {
        appPreferences.setEnabled(packageName, enabled)
    }

    fun setAppNormalOnly(packageName: String, normalOnly: Boolean) = viewModelScope.launch {
        appPreferences.setNormalOnly(packageName, normalOnly)
    }

    fun setMusicShowAlbumArt(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setShowAlbumArt(enabled)
    }

    fun setMusicRotateAlbumArt(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setRotateAlbumArt(enabled)
    }

    fun setMusicAlbumArtStroke(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setAlbumArtStroke(enabled)
    }

    fun setMusicAlbumArtStrokeColor(color: CutoutColor?) = viewModelScope.launch {
        musicTilePreferences.setAlbumArtStrokeColor(color)
    }

    fun setMusicExpandOnPlay(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setExpandOnPlay(enabled)
    }

    fun setMusicVisibleInPlayerApp(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setVisibleInPlayerApp(enabled)
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

    fun setPhoneExpandedIncomingLayout(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setExpandedIncomingLayout(enabled)
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

    fun setAssistantDisplayAnswerInCutout(enabled: Boolean) = viewModelScope.launch {
        assistantTilePreferences.setDisplayAnswerInCutout(enabled)
    }

    fun setAssistantMaxCutoutHeightPercent(percent: Int) = viewModelScope.launch {
        assistantTilePreferences.setMaxCutoutHeightPercent(percent)
    }

    fun setAssistantIconContainerColor(color: CutoutColor?) = viewModelScope.launch {
        assistantTilePreferences.setIconContainerColor(color)
    }

    fun setAssistantUseAnimatedIcon(enabled: Boolean) = viewModelScope.launch {
        assistantTilePreferences.setUseAnimatedIcon(enabled)
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

    fun setVibrateOnTap(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setVibrateOnTap(enabled)
    }

    fun setHideOnLockscreen(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setHideOnLockscreen(enabled)
    }

    fun setHideInLandscape(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setHideInLandscape(enabled)
    }

    fun setHorizontalCutoutMode(mode: HorizontalCutoutMode) = viewModelScope.launch {
        behaviourPreferences.setHorizontalCutoutMode(mode)
    }

    fun setAnimationStyle(style: AnimationStyle) = viewModelScope.launch {
        behaviourPreferences.setAnimationStyle(style)
    }

    fun setAnimationSpeed(speed: AnimationSpeed) = viewModelScope.launch {
        behaviourPreferences.setAnimationSpeed(speed)
    }

    fun setAnimationBounce(bounce: AnimationBounce) = viewModelScope.launch {
        behaviourPreferences.setAnimationBounce(bounce)
    }

    fun setActionButtonAnimation(animation: ActionButtonAnimation) = viewModelScope.launch {
        behaviourPreferences.setActionButtonAnimation(animation)
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

    fun setToastOnAction(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setToastOnAction(enabled)
    }

    fun setShrinkOnSwipeUp(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setShrinkOnSwipeUp(enabled)
    }

    fun setSwipeToDismiss(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setSwipeToDismiss(enabled)
    }

    fun setShowsWhenEmpty(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmpty(enabled)
    }

    fun setShowsWhenEmptyShowIcon(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmptyShowIcon(enabled)
    }

    fun setShowsWhenEmptyImageIcon(uri: String) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmptyIcon(IconSource.Image(uri))
    }

    fun setShowsWhenEmptyMaterialIcon(iconName: String) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmptyIcon(IconSource.Material(iconName))
    }

    fun resetShowsWhenEmptyIcon() = viewModelScope.launch {
        behaviourPreferences.clearShowsWhenEmptyIcon()
    }

    fun setShowsWhenEmptyIconColor(color: CutoutColor?) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmptyIconColor(color)
    }

    fun setShowsWhenEmptyClickAction(action: EmptyClickAction) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmptyClickAction(action)
    }

    fun setShowsWhenEmptyClickPackage(packageName: String?) = viewModelScope.launch {
        behaviourPreferences.setShowsWhenEmptyClickPackage(packageName)
    }

    fun setCenterShortcuts(shortcuts: List<CenterShortcut>) = viewModelScope.launch {
        behaviourPreferences.setCenterShortcuts(shortcuts)
    }

    fun setCenterShowLabels(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setCenterShowLabels(enabled)
    }

    fun setCenterFillContainers(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setCenterFillContainers(enabled)
    }

    fun setCenterThemedIcons(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setCenterThemedIcons(enabled)
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

    /**
     * Whether the user wants the system status bar's notification icons hidden. Saved even while
     * Shizuku is unreachable; `StatusBarIconController` applies it as soon as the bridge is back.
     */
    val hideNotificationIcons: StateFlow<Boolean> =
        statusBarPreferences.hideNotificationIcons.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setHideNotificationIcons(hide: Boolean) = viewModelScope.launch {
        statusBarPreferences.setHideNotificationIcons(hide)
    }

    /**
     * Whether the user wants the system status bar's info icons (clock, battery, Wi-Fi, signal)
     * hidden. Saved even while Shizuku is unreachable; `StatusBarIconController` applies it as soon
     * as the bridge is back.
     */
    val hideSystemInfo: StateFlow<Boolean> =
        statusBarPreferences.hideSystemInfo.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setHideSystemInfo(hide: Boolean) = viewModelScope.launch {
        statusBarPreferences.setHideSystemInfo(hide)
    }

    /**
     * Whether the user wants the system to silence its own alerts (sound, vibration, heads-up) for
     * new notifications, leaving the island as the only thing that reacts. Saved even while Shizuku
     * is unreachable; `StatusBarIconController` applies it as soon as the bridge is back.
     */
    val silenceSystemAlerts: StateFlow<Boolean> =
        statusBarPreferences.silenceAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setSilenceSystemAlerts(silence: Boolean) = viewModelScope.launch {
        statusBarPreferences.setSilenceAlerts(silence)
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

    fun setActionButtonAlignment(alignment: ActionButtonAlignment) = viewModelScope.launch {
        appearancePreferences.setActionButtonAlignment(alignment)
    }

    fun setReplyInputStyle(style: ReplyInputStyle) = viewModelScope.launch {
        appearancePreferences.setReplyInputStyle(style)
    }

    fun setCancelButtonOnLeft(onLeft: Boolean) = viewModelScope.launch {
        appearancePreferences.setCancelButtonOnLeft(onLeft)
    }

    fun setSentAlignment(alignment: SentAlignment) = viewModelScope.launch {
        appearancePreferences.setSentAlignment(alignment)
    }

    fun setMusicShowProgress(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setShowProgress(enabled)
    }

    fun setDismissNotifications(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setDismissNotifications(enabled)
    }

    fun setDisplayWhileDnd(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setDisplayWhileDnd(enabled)
    }
}
