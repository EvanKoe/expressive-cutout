package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Backing store for the collapsed and expanded island geometry. */
private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_prefs")

/**
 * Persists collapsed and expanded geometry for the regular layout and an optional closed-device
 * profile, always emitting values clamped to valid ranges.
 */
class LayoutPreferences(private val context: Context) : JsonSerializable {

    /** The latest fold posture reported by the activity, retained while the overlay runs alone. */
    val deviceClosed: Flow<Boolean> = context.layoutDataStore.data
        .map { it[Keys.DeviceClosed] ?: false }
        .distinctUntilChanged()

    /** The posture whose settings screens are currently editing, defaulting to the detected one. */
    val editingClosed: Flow<Boolean> = combine(
        deviceClosed,
        context.layoutDataStore.data.map { it[Keys.EditingClosed] },
    ) { detectedClosed, selectedClosed ->
        selectedClosed ?: detectedClosed
    }.distinctUntilChanged()

    /** The open-device geometries and optional closed-device profile. */
    val layout: Flow<IslandLayout> = context.layoutDataStore.data.map { prefs ->
        IslandLayout(
            collapsed = prefs.readDimensions(Keys.Collapsed, IslandLayout.DEFAULT_COLLAPSED),
            expanded = prefs.readDimensions(Keys.Expanded, IslandLayout.DEFAULT_EXPANDED),
            closed = if (prefs[Keys.ClosedEnabled] == true) {
                FoldableIslandLayout(
                    collapsed = prefs.readDimensions(Keys.ClosedCollapsed, IslandLayout.DEFAULT_COLLAPSED),
                    expanded = prefs.readDimensions(Keys.ClosedExpanded, IslandLayout.DEFAULT_EXPANDED),
                    animationOrigin = prefs.readAnimationOrigin(
                        Keys.ClosedOriginEnabled,
                        Keys.ClosedOriginX,
                        Keys.ClosedOriginY,
                    ),
                )
            } else {
                null
            },
            animationOrigin = prefs.readAnimationOrigin(
                Keys.OriginEnabled,
                Keys.OriginX,
                Keys.OriginY,
            ),
        )
    }

    /**
     * Exports the regular layout and optional closed-device profile as nested geometry objects.
     * [IslandDimensions] has no serializer of its own, so build it here.
     */
    override suspend fun toJson(): String {
        val l = layout.first()
        return JSONObject().apply {
            put("collapsed", l.collapsed.toJsonObject())
            put("expanded", l.expanded.toJsonObject())
            l.animationOrigin?.let { put("animationOrigin", it.toJsonObject()) }
            l.closed?.let { closed ->
                put(
                    "closed",
                    JSONObject().apply {
                        put("collapsed", closed.collapsed.toJsonObject())
                        put("expanded", closed.expanded.toJsonObject())
                        closed.animationOrigin?.let { put("animationOrigin", it.toJsonObject()) }
                    },
                )
            }
        }.toString()
    }

    /**
     * Applies layout JSON exported by [toJson]. Each state is optional; a state whose object is
     * missing a field is skipped whole rather than half-applied, and [IslandDimensions.of] clamps
     * whatever does come through.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        obj.optJSONObject("collapsed")?.toDimensionsOrNull()?.let { setCollapsed(it) }
        obj.optJSONObject("expanded")?.toDimensionsOrNull()?.let { setExpanded(it) }
        if (obj.has("animationOrigin")) {
            setAnimationOrigin(obj.optJSONObject("animationOrigin")?.toAnimationOriginOrNull(), closed = false)
        }
        obj.optJSONObject("closed")?.toFoldableLayoutOrNull()?.let { setClosed(it) }
    }

    /**
     * Reads one island geometry out of an imported settings file, returning null if any field is
     * missing or malformed rather than throwing — a partial import leaves the stored layout
     * untouched.
     */
    private fun JSONObject.toDimensionsOrNull(): IslandDimensions? = runCatching {
        IslandDimensions.of(
            widthPercent = getInt("widthPercent"),
            heightDp = getInt("heightDp"),
            offsetXDp = getInt("offsetXDp"),
            offsetYDp = getInt("offsetYDp"),
            cornerTopLeftDp = getInt("cornerTopLeftDp"),
            cornerTopRightDp = getInt("cornerTopRightDp"),
            cornerBottomLeftDp = getInt("cornerBottomLeftDp"),
            cornerBottomRightDp = getInt("cornerBottomRightDp"),
            topMarginDp = optInt("topMarginDp", IslandDimensions.DEFAULT_TOP_MARGIN_DP),
        )
    }.getOrNull()

    /**
     * Reads both geometries in a closed-device profile, leaving the profile unchanged if either is
     * missing or malformed.
     */
    private fun JSONObject.toFoldableLayoutOrNull(): FoldableIslandLayout? {
        val collapsed = optJSONObject("collapsed")?.toDimensionsOrNull() ?: return null
        val expanded = optJSONObject("expanded")?.toDimensionsOrNull() ?: return null
        return FoldableIslandLayout(
            collapsed = collapsed,
            expanded = expanded,
            animationOrigin = optJSONObject("animationOrigin")?.toAnimationOriginOrNull(),
        )
    }

    /** Reads an animation origin out of JSON, leaving it unset if either coordinate is malformed. */
    private fun JSONObject.toAnimationOriginOrNull(): AnimationOrigin? = runCatching {
        AnimationOrigin(
            offsetXDp = getInt("offsetXDp").coerceIn(AnimationOrigin.MIN_OFFSET_X_DP, AnimationOrigin.MAX_OFFSET_X_DP),
            offsetYDp = getInt("offsetYDp").coerceIn(IslandDimensions.MIN_OFFSET_Y_DP, IslandDimensions.MAX_OFFSET_Y_DP),
        )
    }.getOrNull()

    /** Writes the screen-relative animation origin in the same compact shape used by import. */
    private fun AnimationOrigin.toJsonObject(): JSONObject = JSONObject().apply {
        put("offsetXDp", offsetXDp)
        put("offsetYDp", offsetYDp)
    }

    /**
     * Writes one island geometry for export, field by field so the JSON stays readable and
     * hand-editable.
     */
    private fun IslandDimensions.toJsonObject(): JSONObject = JSONObject().apply {
        put("widthPercent", widthPercent)
        put("heightDp", heightDp)
        put("offsetXDp", offsetXDp)
        put("offsetYDp", offsetYDp)
        put("cornerTopLeftDp", cornerTopLeftDp)
        put("cornerTopRightDp", cornerTopRightDp)
        put("cornerBottomLeftDp", cornerBottomLeftDp)
        put("cornerBottomRightDp", cornerBottomRightDp)
        put("topMarginDp", topMarginDp)
    }

    suspend fun setCollapsed(dimensions: IslandDimensions) = context.layoutDataStore.edit {
        it.writeDimensions(Keys.Collapsed, dimensions)
    }

    suspend fun setExpanded(dimensions: IslandDimensions) = context.layoutDataStore.edit {
        it.writeDimensions(Keys.Expanded, dimensions)
    }

    /** Persists both geometries for the foldable's closed posture. */
    suspend fun setClosed(layout: FoldableIslandLayout) = context.layoutDataStore.edit {
        it[Keys.ClosedEnabled] = true
        it.writeDimensions(Keys.ClosedCollapsed, layout.collapsed)
        it.writeDimensions(Keys.ClosedExpanded, layout.expanded)
        it.writeAnimationOrigin(
            Keys.ClosedOriginEnabled,
            Keys.ClosedOriginX,
            Keys.ClosedOriginY,
            layout.animationOrigin,
        )
    }

    /** Saves the animation origin for the selected fold posture; null restores camera detection. */
    suspend fun setAnimationOrigin(origin: AnimationOrigin?, closed: Boolean) =
        context.layoutDataStore.edit {
            if (closed) {
                it[Keys.ClosedEnabled] = true
                it.writeAnimationOrigin(
                    Keys.ClosedOriginEnabled,
                    Keys.ClosedOriginX,
                    Keys.ClosedOriginY,
                    origin,
                )
            } else {
                it.writeAnimationOrigin(Keys.OriginEnabled, Keys.OriginX, Keys.OriginY, origin)
            }
        }

    /** Remembers the most recently detected fold posture for the overlay service. */
    suspend fun setDeviceClosed(isClosed: Boolean) = context.layoutDataStore.edit {
        it[Keys.DeviceClosed] = isClosed
    }

    /** Saves which posture the main settings screen is editing. */
    suspend fun setEditingClosed(isClosed: Boolean) = context.layoutDataStore.edit {
        it[Keys.EditingClosed] = isClosed
    }

    /** Resets all geometry settings while retaining the latest detected device posture. */
    suspend fun reset() = context.layoutDataStore.edit {
        val deviceClosed = it[Keys.DeviceClosed]
        val editingClosed = it[Keys.EditingClosed]
        it.clear()
        if (deviceClosed != null) it[Keys.DeviceClosed] = deviceClosed
        if (editingClosed != null) it[Keys.EditingClosed] = editingClosed
    }

    private fun Preferences.readDimensions(keys: Keys, default: IslandDimensions) =
        IslandDimensions.of(
            widthPercent = this[keys.width] ?: default.widthPercent,
            heightDp = this[keys.height] ?: default.heightDp,
            offsetXDp = this[keys.offsetX] ?: default.offsetXDp,
            offsetYDp = this[keys.offsetY] ?: default.offsetYDp,
            cornerTopLeftDp = this[keys.cornerTopLeft] ?: default.cornerTopLeftDp,
            cornerTopRightDp = this[keys.cornerTopRight] ?: default.cornerTopRightDp,
            cornerBottomLeftDp = this[keys.cornerBottomLeft] ?: default.cornerBottomLeftDp,
            cornerBottomRightDp = this[keys.cornerBottomRight] ?: default.cornerBottomRightDp,
            topMarginDp = this[keys.topMargin] ?: default.topMarginDp,
        )

    /**
     * Writes one island geometry into preferences under [keys], which is what lets the collapsed
     * and expanded states share this code with different key sets.
     */
    private fun MutablePreferences.writeDimensions(keys: Keys, dimensions: IslandDimensions) {
        this[keys.width] = dimensions.widthPercent
        this[keys.height] = dimensions.heightDp
        this[keys.offsetX] = dimensions.offsetXDp
        this[keys.offsetY] = dimensions.offsetYDp
        this[keys.cornerTopLeft] = dimensions.cornerTopLeftDp
        this[keys.cornerTopRight] = dimensions.cornerTopRightDp
        this[keys.cornerBottomLeft] = dimensions.cornerBottomLeftDp
        this[keys.cornerBottomRight] = dimensions.cornerBottomRightDp
        this[keys.topMargin] = dimensions.topMarginDp
    }

    /** Stores or clears one posture's custom animation origin. */
    private fun MutablePreferences.writeAnimationOrigin(
        enabledKey: Preferences.Key<Boolean>,
        xKey: Preferences.Key<Int>,
        yKey: Preferences.Key<Int>,
        origin: AnimationOrigin?,
    ) {
        if (origin == null) {
            remove(enabledKey)
            remove(xKey)
            remove(yKey)
        } else {
            this[enabledKey] = true
            this[xKey] = origin.offsetXDp
            this[yKey] = origin.offsetYDp
        }
    }

    private fun Preferences.readAnimationOrigin(
        enabledKey: Preferences.Key<Boolean>,
        xKey: Preferences.Key<Int>,
        yKey: Preferences.Key<Int>,
    ): AnimationOrigin? {
        if (this[enabledKey] != true) return null
        val x = this[xKey] ?: return null
        val y = this[yKey] ?: return null
        return AnimationOrigin(
            x.coerceIn(AnimationOrigin.MIN_OFFSET_X_DP, AnimationOrigin.MAX_OFFSET_X_DP),
            y.coerceIn(IslandDimensions.MIN_OFFSET_Y_DP, IslandDimensions.MAX_OFFSET_Y_DP),
        )
    }

    /** The preference keys backing one island state. */
    private class Keys(prefix: String) {
        val width = intPreferencesKey("${prefix}_width_pct")
        val height = intPreferencesKey("${prefix}_height")
        val offsetX = intPreferencesKey("${prefix}_offset_x")
        val offsetY = intPreferencesKey("${prefix}_offset_y")
        val cornerTopLeft = intPreferencesKey("${prefix}_corner_tl")
        val cornerTopRight = intPreferencesKey("${prefix}_corner_tr")
        val cornerBottomLeft = intPreferencesKey("${prefix}_corner_bl")
        val cornerBottomRight = intPreferencesKey("${prefix}_corner_br")
        val topMargin = intPreferencesKey("${prefix}_top_margin")

        companion object {
            val Collapsed = Keys("collapsed")
            val Expanded = Keys("expanded")
            val ClosedCollapsed = Keys("closed_collapsed")
            val ClosedExpanded = Keys("closed_expanded")
            val ClosedEnabled = booleanPreferencesKey("closed_enabled")
            val DeviceClosed = booleanPreferencesKey("device_closed")
            val EditingClosed = booleanPreferencesKey("editing_closed")
            val OriginEnabled = booleanPreferencesKey("animation_origin_enabled")
            val OriginX = intPreferencesKey("animation_origin_x")
            val OriginY = intPreferencesKey("animation_origin_y")
            val ClosedOriginEnabled = booleanPreferencesKey("closed_animation_origin_enabled")
            val ClosedOriginX = intPreferencesKey("closed_animation_origin_x")
            val ClosedOriginY = intPreferencesKey("closed_animation_origin_y")
        }
    }
}
