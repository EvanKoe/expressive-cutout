package com.ekoehler.expressivecutout.system

/**
 * What one read of the Wi-Fi hotspot found: its network name (null when unknown), the number of
 * connected devices the Wi-Fi service reports (null when unknown, so a real 0 can be told apart from
 * no reading), and the lowercase MAC addresses of those the neighbour table could name.
 */
data class HotspotState(
    val ssid: String? = null,
    val count: Int? = null,
    val clients: List<String> = emptyList(),
)
