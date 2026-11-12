package com.ekoehler.expressivecutout.overlay

import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.swe.widgets.UnreadWidget

/**
 * Presentation options of the hotspot event card: the style of the connected-device count
 * ([badge], null while the count option is off) and the user's Turn-off button colour
 * ([turnOffColor], null for the default tint).
 */
data class HotspotCard(val badge: UnreadWidget?, val turnOffColor: CutoutColor?)
