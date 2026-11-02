package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which [CutoutKind] the island is currently drawing, or null while it is down.
 *
 * It exists so the status-bar flags can follow the pill without `StatusBarIconController` reaching
 * into the overlay: the overlay publishes the fact, the controller decides what to do with it.
 * While the island is down the kind is null, so the flags fall back to the user's own wishes.
 */
object CutoutWidthBus {
    private val _kind = MutableStateFlow<CutoutKind?>(null)
    val kind: StateFlow<CutoutKind?> = _kind.asStateFlow()

    /**
     * Whether the collapsed pill is drawn a little wider on its trailing edge to seat the permission
     * dots beside a tile that already writes there. Separate from [kind] because it is a few dp
     * on the right only: it reaches the system icons, which sit at that end of the status bar, but
     * not the clock or the notification icons at the other end.
     */
    private val _grownForDots = MutableStateFlow(false)
    val grownForDots: StateFlow<Boolean> = _grownForDots.asStateFlow()

    /**
     * Whether the collapsed pill is drawn a little wider on its leading edge to seat the status dot
     * beside the camera. The mirror of [grownForDots]: it reaches the notification icons, which sit
     * at that end of the status bar, but not the system icons at the other end.
     */
    private val _grownForStatusDot = MutableStateFlow(false)
    val grownForStatusDot: StateFlow<Boolean> = _grownForStatusDot.asStateFlow()

    /** Publishes the cutout kind (null when the island is down) together with the two edge-growth flags. */
    fun update(kind: CutoutKind?, grownForDots: Boolean, grownForStatusDot: Boolean) {
        _kind.value = kind
        _grownForDots.value = grownForDots
        _grownForStatusDot.value = grownForStatusDot
    }
}
