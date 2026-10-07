package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Signals that the settings screen wants the real overlay pinned open so size/position/radius
 * changes can be seen live on the actual cutout. [expandedPreview] mirrors which tab the user
 * is editing, while [closedPreview] selects the foldable profile being edited.
 */
object IslandPreviewBus {

    private val mutableActive = MutableStateFlow(false)
    val active: StateFlow<Boolean> = mutableActive

    private val mutableExpandedPreview = MutableStateFlow(false)
    val expandedPreview: StateFlow<Boolean> = mutableExpandedPreview

    private val mutableClosedPreview = MutableStateFlow<Boolean?>(null)
    val closedPreview: StateFlow<Boolean?> = mutableClosedPreview

    fun setActive(value: Boolean) {
        mutableActive.value = value
    }

    fun setExpandedPreview(value: Boolean) {
        mutableExpandedPreview.value = value
    }

    /** Selects an open or closed foldable profile for the pinned preview, or clears the override. */
    fun setClosedPreview(value: Boolean?) {
        mutableClosedPreview.value = value
    }
}
