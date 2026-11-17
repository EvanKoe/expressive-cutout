package com.ekoehler.expressivecutout.overlay

import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.core.stateFamily

/**
 * The two visible event slots and the system-event type currently occupying each slot.
 */
data class SystemEventSlots(
    val primary: IslandEvent? = null,
    val primaryType: SystemEventType? = null,
    val satellite: IslandEvent? = null,
    val satelliteType: SystemEventType? = null,
)

/**
 * The result of checking whether an incoming system event replaced an existing state event.
 */
data class SystemEventSlotTransition(
    val handled: Boolean,
    val slots: SystemEventSlots,
)

/**
 * Replaces a same-family system event in either visible slot without creating a second bubble.
 */
fun replaceSystemEventInSlots(
    slots: SystemEventSlots,
    incomingType: SystemEventType,
    incoming: IslandEvent,
): SystemEventSlotTransition {
    val incomingFamily = incomingType.stateFamily
    return when {
        slots.primaryType?.stateFamily == incomingFamily -> SystemEventSlotTransition(
            handled = true,
            slots = slots.copy(primary = incoming, primaryType = incomingType),
        )
        slots.satelliteType?.stateFamily == incomingFamily -> SystemEventSlotTransition(
            handled = true,
            slots = slots.copy(satellite = incoming, satelliteType = incomingType),
        )
        else -> SystemEventSlotTransition(handled = false, slots = slots)
    }
}
