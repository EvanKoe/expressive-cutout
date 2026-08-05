package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_prefs")

/**
 * Persists the collapsed and expanded island geometry independently, always emitting values
 * clamped to valid ranges.
 */
class LayoutPreferences(private val context: Context) : JsonExportable {

    val layout: Flow<IslandLayout> = context.layoutDataStore.data.map { prefs ->
        IslandLayout(
            collapsed = prefs.readDimensions(Keys.Collapsed, IslandLayout.DEFAULT_COLLAPSED),
            expanded = prefs.readDimensions(Keys.Expanded, IslandLayout.DEFAULT_EXPANDED),
        )
    }

    /**
     * Exports the layout to a JSON string { collapsed: {...}, expanded: {...} }, each state a nested
     * object of its geometry. [IslandDimensions] has no serializer of its own, so build it here.
     */
    override suspend fun toJson(): String {
        val l = layout.first()
        return JSONObject().apply {
            put("collapsed", l.collapsed.toJsonObject())
            put("expanded", l.expanded.toJsonObject())
        }.toString()
    }

    private fun IslandDimensions.toJsonObject(): JSONObject = JSONObject().apply {
        put("widthPercent", widthPercent)
        put("heightDp", heightDp)
        put("offsetXDp", offsetXDp)
        put("offsetYDp", offsetYDp)
        put("cornerTopLeftDp", cornerTopLeftDp)
        put("cornerTopRightDp", cornerTopRightDp)
        put("cornerBottomLeftDp", cornerBottomLeftDp)
        put("cornerBottomRightDp", cornerBottomRightDp)
    }

    suspend fun setCollapsed(dimensions: IslandDimensions) = context.layoutDataStore.edit {
        it.writeDimensions(Keys.Collapsed, dimensions)
    }

    suspend fun setExpanded(dimensions: IslandDimensions) = context.layoutDataStore.edit {
        it.writeDimensions(Keys.Expanded, dimensions)
    }

    suspend fun reset() = context.layoutDataStore.edit { it.clear() }

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
        )

    private fun MutablePreferences.writeDimensions(keys: Keys, dimensions: IslandDimensions) {
        this[keys.width] = dimensions.widthPercent
        this[keys.height] = dimensions.heightDp
        this[keys.offsetX] = dimensions.offsetXDp
        this[keys.offsetY] = dimensions.offsetYDp
        this[keys.cornerTopLeft] = dimensions.cornerTopLeftDp
        this[keys.cornerTopRight] = dimensions.cornerTopRightDp
        this[keys.cornerBottomLeft] = dimensions.cornerBottomLeftDp
        this[keys.cornerBottomRight] = dimensions.cornerBottomRightDp
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

        companion object {
            val Collapsed = Keys("collapsed")
            val Expanded = Keys("expanded")
        }
    }
}
