package com.ekoehler.expressivecutout.swe

import com.ekoehler.expressivecutout.events.BatteryStatus
import com.ekoehler.expressivecutout.service.UnreadCounts

/**
 * The live values every resting-pill widget reads from. A new data source (weather, ...) is added
 * here once, not to each widget. [battery] is null while no reading is available.
 */
data class SweData(
    val counts: UnreadCounts,
    val battery: BatteryStatus?,
)
