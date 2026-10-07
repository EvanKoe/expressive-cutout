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
import com.ekoehler.expressivecutout.core.stateFamily
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
import com.ekoehler.expressivecutout.data.FoldableIslandLayout
import com.ekoehler.expressivecutout.data.AnimationOrigin
import com.ekoehler.expressivecutout.data.LanguagePreferences
import com.ekoehler.expressivecutout.data.LayoutPreferences
import com.ekoehler.expressivecutout.data.MusicButtonStyle
import com.ekoehler.expressivecutout.data.MusicRightButtonAction
import com.ekoehler.expressivecutout.data.MusicTilePreferences
import com.ekoehler.expressivecutout.data.MusicTileSettings
import com.ekoehler.expressivecutout.data.PhoneTilePreferences
import com.ekoehler.expressivecutout.data.PhoneTileSettings
import com.ekoehler.expressivecutout.data.RecentColorPreferences
import com.ekoehler.expressivecutout.data.PermissionDotColors
import com.ekoehler.expressivecutout.data.PermissionDotKinds
import com.ekoehler.expressivecutout.data.PermissionDotPosition
import com.ekoehler.expressivecutout.data.PermissionDotPreferences
import com.ekoehler.expressivecutout.data.PageTransitionStyle
import com.ekoehler.expressivecutout.data.StatusBarHideMode
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import com.ekoehler.expressivecutout.data.TimerTilePreferences
import com.ekoehler.expressivecutout.data.TimerTileSettings
import com.ekoehler.expressivecutout.data.SwipeDismissDirection
import com.ekoehler.expressivecutout.data.SatellitePosition
import com.ekoehler.expressivecutout.data.SwipeDismissTarget
import com.ekoehler.expressivecutout.data.ThemePreferences
import com.ekoehler.expressivecutout.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
    private val layoutPreferences = LayoutPreferences(application)
    private val preferences = IconPreferences(application, layoutPreferences.editingClosed)
    private val themePreferences = ThemePreferences(application)
    private val languagePreferences = LanguagePreferences(application)
    private val behaviourPreferences = BehaviourPreferences(application, layoutPreferences.editingClosed)
    private val appearancePreferences = AppearancePreferences(application)
    private val eventPreferences = EventPreferences(application, layoutPreferences.editingClosed)
    private val dynamicTilePreferences = DynamicTilePreferences(application, layoutPreferences.editingClosed)
    private val musicTilePreferences = MusicTilePreferences(application, layoutPreferences.editingClosed)
    private val phoneTilePreferences = PhoneTilePreferences(application, layoutPreferences.editingClosed)
    private val timerTilePreferences = TimerTilePreferences(application, layoutPreferences.editingClosed)
    private val assistantTilePreferences = AssistantTilePreferences(application, layoutPreferences.editingClosed)
    private val appPreferences = AppPreferences(application, layoutPreferences.editingClosed)
    private val recentColorPreferences = RecentColorPreferences(application)
    private val statusBarPreferences = StatusBarPreferences(application, layoutPreferences.editingClosed)
    private val permissionDotPreferences = PermissionDotPreferences(application, layoutPreferences.editingClosed)

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

    val deviceClosed: StateFlow<Boolean> =
        layoutPreferences.deviceClosed.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val editingClosed: StateFlow<Boolean> =
        layoutPreferences.editingClosed.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val theme: StateFlow<AppTheme> =
        themePreferences.theme.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.SYSTEM,
        )

    val language: StateFlow<String> = languagePreferences.language

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

    /** Settings that are shared between postures and written once in each settings export. */
    private val sharedJsonSections: Map<String, JsonSerializable> = mapOf(
        JsonSettings.LANGUAGE to languagePreferences,
        JsonSettings.LAYOUT to layoutPreferences,
        JsonSettings.THEME to themePreferences,
        JsonSettings.APPEARANCE to appearancePreferences,
        JsonSettings.RECENT_COLORS to recentColorPreferences,
    )

    /** Both complete island-configuration profiles, kept together in exported settings. */
    private val profileJsonSections: Map<String, Map<String, JsonSerializable>> = mapOf(
        "open" to createProfileJsonSections(application, flowOf(false)),
        "closed" to createProfileJsonSections(application, flowOf(true)),
    )

    /** Exports shared settings and both device-posture profiles in one JSON document. */
    suspend fun getSettingsAsJsonString(): String =
        JsonSettings.exportProfiles(sharedJsonSections, profileJsonSections)

    /**
     * Creates the stores that belong to one fold posture for settings import and export.
     */
    private fun createProfileJsonSections(
        context: Application,
        profile: kotlinx.coroutines.flow.Flow<Boolean>,
    ): Map<String, JsonSerializable> = mapOf(
        JsonSettings.ICONS to IconPreferences(context, profile),
        JsonSettings.BEHAVIOUR to BehaviourPreferences(context, profile),
        JsonSettings.EVENTS to EventPreferences(context, profile),
        JsonSettings.DYNAMIC_TILES to DynamicTilePreferences(context, profile),
        JsonSettings.MUSIC_TILE to MusicTilePreferences(context, profile),
        JsonSettings.PHONE_TILE to PhoneTilePreferences(context, profile),
        JsonSettings.TIMER_TILE to TimerTilePreferences(context, profile),
        JsonSettings.ASSISTANT_TILE to AssistantTilePreferences(context, profile),
        JsonSettings.APPS to AppPreferences(context, profile),
        JsonSettings.STATUS_BAR to StatusBarPreferences(context, profile),
        JsonSettings.PERMISSION_DOT to PermissionDotPreferences(context, profile),
    )

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
                    JsonSettings.importProfiles(
                        json,
                        sharedJsonSections,
                        profileJsonSections,
                        legacyProfile = "open",
                        sharedSectionFallbackProfiles = mapOf(
                            JsonSettings.THEME to "open",
                            JsonSettings.APPEARANCE to "open",
                        ),
                    )
                } catch (e: Exception) {
                    Log.w("Error", "starting import ${e.message}")
                    JsonSettings.ImportResult.ERROR
                }
            }
            onDone(result)
        }
    }

    fun setImageIcon(type: SystemEventType, uri: String) = viewModelScope.launch {
        forEachState(type) { preferences.setIcon(it, IconSource.Image(uri)) }
    }

    fun setMaterialIcon(type: SystemEventType, iconName: String) = viewModelScope.launch {
        forEachState(type) { preferences.setIcon(it, IconSource.Material(iconName)) }
    }

    fun resetIcon(type: SystemEventType) = viewModelScope.launch {
        forEachState(type) { preferences.clearIcon(it) }
    }

    fun setEventEnabled(type: SystemEventType, enabled: Boolean) = viewModelScope.launch {
        forEachState(type) { eventPreferences.setEnabled(it, enabled) }
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
        forEachState(type) { eventPreferences.setColor(it, color) }
    }

    fun resetEventColor(type: SystemEventType) = viewModelScope.launch {
        forEachState(type) { eventPreferences.clearColor(it) }
    }

    fun setEventAnimatedIcon(type: SystemEventType, enabled: Boolean) = viewModelScope.launch {
        forEachState(type) { eventPreferences.setAnimatedIcon(it, enabled) }
    }

    fun setEventAnimatedIconLoop(type: SystemEventType, loop: Boolean) = viewModelScope.launch {
        forEachState(type) { eventPreferences.setAnimatedIconLoop(it, loop) }
    }

    /** Applies a per-event setting to every state in the event's integration family. */
    private suspend fun forEachState(
        type: SystemEventType,
        action: suspend (SystemEventType) -> Unit,
    ) {
        type.stateFamily.members.forEach { action(it) }
    }

    fun setTileEnabled(tile: DynamicTile, enabled: Boolean) = viewModelScope.launch {
        dynamicTilePreferences.setEnabled(tile, enabled)
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) = viewModelScope.launch {
        appPreferences.setEnabled(packageName, enabled)
    }

    fun setAppsEnabled(packageNames: Collection<String>, enabled: Boolean) = viewModelScope.launch {
        appPreferences.setEnabled(packageNames, enabled)
    }

    fun setAppNormalOnly(packageName: String, normalOnly: Boolean) = viewModelScope.launch {
        appPreferences.setNormalOnly(packageName, normalOnly)
    }

    fun setMusicShowAlbumArt(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setShowAlbumArt(enabled)
    }

    fun setMusicShowAlbumBackground(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setShowAlbumBackground(enabled)
    }

    fun setMusicRotateAlbumArt(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setRotateAlbumArt(enabled)
    }

    fun setMusicCircleCover(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setCircleCover(enabled)
    }

    fun setMusicAlbumArtStroke(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setAlbumArtStroke(enabled)
    }

    fun setMusicAlbumArtStrokeColor(color: CutoutColor?) = viewModelScope.launch {
        musicTilePreferences.setAlbumArtStrokeColor(color)
    }

    fun setMusicCoverFallbackColor(color: CutoutColor?) = viewModelScope.launch {
        musicTilePreferences.setCoverFallbackColor(color)
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

    fun setPhoneShowButtonLabels(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setShowButtonLabels(enabled)
    }

    fun setPhoneExpandedIncomingLayout(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setExpandedIncomingLayout(enabled)
    }

    fun setPhoneMiniCall(enabled: Boolean) = viewModelScope.launch {
        phoneTilePreferences.setMiniCall(enabled)
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

    fun setPhoneIncomingAnswerColor(color: CutoutColor) = viewModelScope.launch {
        phoneTilePreferences.setIncomingAnswerColor(color)
    }

    fun setPhoneIncomingHangUpColor(color: CutoutColor) = viewModelScope.launch {
        phoneTilePreferences.setIncomingHangUpColor(color)
    }

    fun setPhoneExpandedHangUpColor(color: CutoutColor) = viewModelScope.launch {
        phoneTilePreferences.setExpandedHangUpColor(color)
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

    fun setMusicPreviousExpand(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setPreviousExpand(enabled)
    }

    fun setMusicPreviousText(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setPreviousText(enabled)
    }

    fun setMusicPreviousTextWidth(width: Float) = viewModelScope.launch {
        musicTilePreferences.setPreviousTextWidth(width)
    }

    fun setMusicNextExpand(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setNextExpand(enabled)
    }

    fun setMusicNextText(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setNextText(enabled)
    }

    fun setMusicNextTextWidth(width: Float) = viewModelScope.launch {
        musicTilePreferences.setNextTextWidth(width)
    }

    fun setMusicPlayPauseExpand(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setPlayPauseExpand(enabled)
    }

    fun setMusicPlayPauseText(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setPlayPauseText(enabled)
    }

    fun setMusicPlayPauseTextWidth(width: Float) = viewModelScope.launch {
        musicTilePreferences.setPlayPauseTextWidth(width)
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

    /** Saves both geometry states for the foldable's closed posture. */
    fun setClosedLayout(layout: FoldableIslandLayout) = viewModelScope.launch {
        layoutPreferences.setClosed(layout)
    }

    /** Saves the independent animation starting point for the posture being edited. */
    fun setAnimationOrigin(origin: AnimationOrigin?, closed: Boolean) = viewModelScope.launch {
        layoutPreferences.setAnimationOrigin(origin, closed)
    }

    fun setEditingClosed(isClosed: Boolean) = viewModelScope.launch {
        layoutPreferences.setEditingClosed(isClosed)
    }

    fun resetLayout() = viewModelScope.launch { layoutPreferences.reset() }

    fun setTheme(theme: AppTheme) = viewModelScope.launch { themePreferences.setTheme(theme) }

    /**
     * Switches the app to the language tagged [tag]. The store applies it to the process straight
     * away; on Android 12 and below the caller still has to restart the activity for the UI it is
     * showing to be re-read in the new language.
     */
    fun setLanguage(tag: String) = languagePreferences.setLanguage(tag)

    fun setCutoutEnabled(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setCutoutEnabled(enabled)
    }

    fun setVibrateOnTap(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setVibrateOnTap(enabled)
    }

    fun setHapticsOnPop(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setHapticsOnPop(enabled)
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

    fun setIgnoreSilentNotifications(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setIgnoreSilentNotifications(enabled)
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

    /** Toggles the radiating status dot drawn on the trailing edge of system-event pills. */
    fun setShowStatusDot(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setShowStatusDot(enabled)
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

    fun setSplitIslandEnabled(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setSplitIslandEnabled(enabled)
    }

    fun setSatellitePosition(position: SatellitePosition) = viewModelScope.launch {
        behaviourPreferences.setSatellitePosition(position)
    }

    fun setShadowEnabled(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setShadowEnabled(enabled)
    }

    /** Saves the page transition style used by in-app navigation. */
    fun setPageTransitionStyle(style: PageTransitionStyle) = viewModelScope.launch {
        appearancePreferences.setPageTransitionStyle(style)
    }

    /**
     * When the system status bar's notification icons are hidden: never, only while the island is
     * drawn wider than its normal cutout, or the whole time. Saved even while Shizuku is
     * unreachable; `StatusBarIconController` applies it as soon as the bridge is back.
     */
    val notificationIconsMode: StateFlow<StatusBarHideMode> =
        statusBarPreferences.notificationIconsMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusBarHideMode.OFF,
        )

    fun setNotificationIconsMode(mode: StatusBarHideMode) = viewModelScope.launch {
        statusBarPreferences.setNotificationIconsMode(mode)
    }

    /**
     * When the system status bar's info icons (battery, Wi-Fi, signal) are hidden. These sit at the
     * far end of the bar, so a pill that already covers the notification icons at rest often still
     * leaves these alone — which is why each setting carries its own [StatusBarHideMode].
     */
    val systemInfoMode: StateFlow<StatusBarHideMode> =
        statusBarPreferences.systemInfoMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusBarHideMode.OFF,
        )

    fun setSystemInfoMode(mode: StatusBarHideMode) = viewModelScope.launch {
        statusBarPreferences.setSystemInfoMode(mode)
    }

    /** When the system status bar's clock is hidden. See [notificationIconsMode]. */
    val clockMode: StateFlow<StatusBarHideMode> =
        statusBarPreferences.clockMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusBarHideMode.OFF,
        )

    fun setClockMode(mode: StatusBarHideMode) = viewModelScope.launch {
        statusBarPreferences.setClockMode(mode)
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

    /**
     * Whether the user wants the island to mark live microphone, camera and location use. Saved even
     * while Shizuku is unreachable; `PermissionUsageMonitor` starts reading as soon as the bridge is
     * back, and reports nothing until then.
     */
    val permissionDotEnabled: StateFlow<Boolean> =
        permissionDotPreferences.enabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setPermissionDotEnabled(enabled: Boolean) = viewModelScope.launch {
        permissionDotPreferences.setEnabled(enabled)
    }

    /** Which end of the collapsed pill the permission dots sit on. */
    val permissionDotPosition: StateFlow<PermissionDotPosition> =
        permissionDotPreferences.position.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PermissionDotPosition.RIGHT,
        )

    fun setPermissionDotPosition(position: PermissionDotPosition) = viewModelScope.launch {
        permissionDotPreferences.setPosition(position)
    }

    /** Whether the dots stack vertically rather than running along the pill. */
    val permissionDotVertical: StateFlow<Boolean> =
        permissionDotPreferences.vertical.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setPermissionDotVertical(vertical: Boolean) = viewModelScope.launch {
        permissionDotPreferences.setVertical(vertical)
    }

    /** Which resources get a dot; one switched off is neither polled for nor drawn. */
    val permissionDotKinds: StateFlow<PermissionDotKinds> =
        permissionDotPreferences.kinds.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PermissionDotKinds(),
        )

    fun setPermissionDotLocation(enabled: Boolean) = viewModelScope.launch {
        permissionDotPreferences.setLocation(enabled)
    }

    fun setPermissionDotCamera(enabled: Boolean) = viewModelScope.launch {
        permissionDotPreferences.setCamera(enabled)
    }

    fun setPermissionDotMicrophone(enabled: Boolean) = viewModelScope.launch {
        permissionDotPreferences.setMicrophone(enabled)
    }

    /** The colour each dot is drawn in, one per watched resource. */
    val permissionDotColors: StateFlow<PermissionDotColors> =
        permissionDotPreferences.colors.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PermissionDotColors(),
        )

    fun setPermissionDotLocationColor(color: CutoutColor) = viewModelScope.launch {
        permissionDotPreferences.setLocationColor(color)
    }

    fun setPermissionDotCameraColor(color: CutoutColor) = viewModelScope.launch {
        permissionDotPreferences.setCameraColor(color)
    }

    fun setPermissionDotMicrophoneColor(color: CutoutColor) = viewModelScope.launch {
        permissionDotPreferences.setMicrophoneColor(color)
    }

    fun setStrokeEnabled(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setStrokeEnabled(enabled)
    }

    fun setShowSourceAppName(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setShowSourceAppName(enabled)
    }

    fun setShowTimestamp(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setShowTimestamp(enabled)
    }

    fun setShowFullNotificationText(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setShowFullNotificationText(enabled)
    }

    fun setPreferDynamicIconColor(enabled: Boolean) = viewModelScope.launch {
        appearancePreferences.setPreferDynamicIconColor(enabled)
    }

    fun setStrokeWidth(widthDp: Int) = viewModelScope.launch {
        appearancePreferences.setStrokeWidth(widthDp)
    }

    fun setStrokeOpacity(opacity: Float) = viewModelScope.launch {
        appearancePreferences.setStrokeOpacity(opacity)
    }

    fun setStrokeColor(color: CutoutColor) = viewModelScope.launch {
        appearancePreferences.setStrokeColor(color)
    }

    fun setTextColor(color: CutoutColor?) = viewModelScope.launch {
        appearancePreferences.setTextColor(color)
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

    fun setMusicMiniPlayer(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setMiniPlayer(enabled)
    }

    fun setMusicRightButton(enabled: Boolean) = viewModelScope.launch {
        musicTilePreferences.setRightButton(enabled)
    }

    fun setMusicRightButtonAction(action: MusicRightButtonAction) = viewModelScope.launch {
        musicTilePreferences.setRightButtonAction(action)
    }

    fun setDismissNotifications(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setDismissNotifications(enabled)
    }

    fun setDisplayWhileDnd(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setDisplayWhileDnd(enabled)
    }

    fun setAlertOnNotification(enabled: Boolean) = viewModelScope.launch {
        behaviourPreferences.setAlertOnNotification(enabled)
    }
}
