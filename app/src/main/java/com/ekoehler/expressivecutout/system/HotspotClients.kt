package com.ekoehler.expressivecutout.system

import android.content.Context
import android.os.IBinder
import android.util.Log
import com.ekoehler.expressivecutout.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * The Wi-Fi hotspot's network name and connected devices, read through Shizuku because no public
 * API exposes them, published as one [state]. Polls only while the hotspot is on, the feature is
 * enabled and Shizuku is [ShizukuStatus.READY]; otherwise [state] is empty and nothing runs.
 *
 * The AP state broadcast only reports changes, so whether the hotspot is already on is probed
 * once at [start] and whenever Shizuku becomes ready.
 *
 * MACs are held in memory only and never logged or persisted.
 */
object HotspotClients {

    private val _state = MutableStateFlow(HotspotState())

    /** The latest hotspot name and connected devices, empty whenever polling is not active. */
    val state: StateFlow<HotspotState> = _state.asStateFlow()

    private var scope: CoroutineScope? = null
    private var pollJob: Job? = null
    private var statusJob: Job? = null
    private var hotspotOn = false
    private var enabled = true

    /** Begins watching Shizuku's status on [scope]. Paired with [stop]. */
    @Synchronized
    fun start(scope: CoroutineScope) {
        stop()
        if (BuildConfig.DEBUG) selfCheck()
        this.scope = scope
        statusJob = scope.launch {
            ShizukuState.status.collect { status ->
                if (status == ShizukuStatus.READY) probeHotspotOn()
                reconcile()
            }
        }
    }

    /** Stops polling and clears [state]. */
    @Synchronized
    fun stop() {
        statusJob?.cancel()
        statusJob = null
        scope = null
        reconcile()
    }

    /** Records whether the hotspot is on, as reported by the AP state broadcast. */
    @Synchronized
    fun setHotspotOn(on: Boolean) {
        hotspotOn = on
        reconcile()
    }

    /** Records whether the user turned the connected-devices feature on. */
    @Synchronized
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        reconcile()
    }

    /**
     * Asks the Wi-Fi service, as the shell user Shizuku lends us, to stop the hotspot, and reports
     * whether it accepted. Blocks on a binder call, so run it off the main thread.
     *
     * ponytail: calls the hidden `IWifiManager.stopSoftAp()` by reflection; revisit if a release
     * renames it or the tethering service stops reacting to it.
     */
    fun stopHotspot(): Boolean = runCatching {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.WIFI_SERVICE))
        val wifi = Class.forName("android.net.wifi.IWifiManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
            ?: error("IWifiManager.asInterface returned null")
        Class.forName("android.net.wifi.IWifiManager").getMethod("stopSoftAp").invoke(wifi) as Boolean
    }.getOrElse { error ->
        Log.w(TAG, "Stopping the hotspot failed", error)
        false
    }

    /**
     * Sets [hotspotOn] from the live Wi-Fi service state, since the AP state broadcast is missed
     * when the hotspot was already on before we started. A failed read leaves it unchanged.
     */
    private suspend fun probeHotspotOn() {
        val output = withContext(Dispatchers.IO) { ShizukuState.runAsShellForOutput(DUMP_COMMAND) } ?: return
        setHotspotOn(liveBlock(output) != null)
    }

    /** Starts or stops the poll loop so it runs exactly when every precondition holds. */
    @Synchronized
    private fun reconcile() {
        val active = scope != null && hotspotOn && enabled &&
            ShizukuState.status.value == ShizukuStatus.READY
        if (!active) {
            pollJob?.cancel()
            pollJob = null
            _state.value = HotspotState()
        } else if (pollJob?.isActive != true) {
            pollJob = scope?.launch { pollLoop() }
        }
    }

    /** Re-reads the hotspot every [POLL_INTERVAL_MS] until cancelled. */
    private suspend fun pollLoop() {
        while (true) {
            val dump = withContext(Dispatchers.IO) { ShizukuState.runAsShellForOutput(DUMP_COMMAND) }
            if (dump != null) _state.value = parse(dump)
            delay(POLL_INTERVAL_MS)
        }
    }

    /**
     * Picks the live `SoftApManager` block out of the output of [DUMP_COMMAND], or null when no
     * hotspot is up. `dumpsys wifi` prints the live manager and then up to three dead ones under
     * `ActiveModeWarden.Graveyard` (`curState=<QUIT>`, empty `mCurrentSoftApInfoMap`, count 0), so
     * position says nothing: the live block is the one whose `mCurrentSoftApInfoMap` is not `{}`.
     */
    private fun liveBlock(dump: String): String? = dump.split(BLOCK_DELIMITER).drop(1).firstOrNull { block ->
        val map = AP_MAP_REGEX.find(block)?.groupValues?.get(1)?.trim()
        map != null && map != "{}"
    }

    /**
     * Reads a [HotspotState] from the output of [DUMP_COMMAND]. The name and the number of connected
     * devices (`getConnectedClientList().size()`) come from the live block only, see [liveBlock].
     * `dumpsys wifi` never gives MACs, so the devices themselves come from the neighbour table of the
     * AP interface, capped at that number because a device that just left lingers there for a while.
     *
     * Fails soft: an unrecognised dump gives an empty [HotspotState].
     */
    private fun parse(dump: String): HotspotState {
        val block = liveBlock(dump) ?: return HotspotState()
        val ssid = SSID_REGEX.find(block)?.groupValues?.get(1)
        val count = COUNT_REGEX.find(block)?.groupValues?.get(1)?.toInt()
        val clients = NEIGHBOUR_REGEX.findAll(dump)
            .filter { it.groupValues[3] !in DEAD_NEIGHBOUR_STATES }
            .map { it.groupValues[2].lowercase() }
            .distinct()
            .take(count ?: 0)
            .toList()
        if (BuildConfig.DEBUG && count != null && count > 0 && clients.isEmpty()) Log.d(TAG, "count=$count but no neighbour rows")
        return HotspotState(ssid, count, clients)
    }

    /** Crashes a debug build at start-up if [parse] stops choosing the live block regardless of order. */
    private fun selfCheck() {
        val live = parse(LIVE_FIRST_SAMPLE)
        check(live.count == 1 && live.clients == listOf("aa:bb:cc:dd:ee:01")) { "live-first sample: $live" }
        check(parse(ALL_GRAVEYARD_SAMPLE) == HotspotState()) { "all-graveyard sample" }
        val reversed = parse(GRAVEYARD_FIRST_SAMPLE)
        check(reversed.count == 1 && reversed.clients == listOf("aa:bb:cc:dd:ee:02")) { "graveyard-first sample: $reversed" }
    }

    private const val TAG = "HotspotClients"
    private const val POLL_INTERVAL_MS = 5000L
    private const val DUMP_COMMAND =
        "dumpsys wifi | grep -E 'Dump of SoftApManager|mCurrentSoftApConfiguration|mCurrentSoftApInfoMap|getConnectedClientList'; ip neigh show"
    private const val BLOCK_DELIMITER = "Dump of SoftApManager"
    private val DEAD_NEIGHBOUR_STATES = setOf("FAILED", "INCOMPLETE")
    private val SSID_REGEX = Regex("mCurrentSoftApConfiguration:\\s*ssid\\s*=\\s*\"([^\"]*)\"")
    private val AP_MAP_REGEX = Regex("mCurrentSoftApInfoMap(.*)")
    private val COUNT_REGEX = Regex("getConnectedClientList\\(\\)\\.size\\(\\):\\s*(\\d+)")
    private val NEIGHBOUR_REGEX = Regex(
        "^\\S+ dev (ap\\S*|swlan\\S*|wlan[1-9]\\S*) lladdr ((?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2})(?: router)? (\\w+)\\s*$",
        RegexOption.MULTILINE,
    )

    /** A dump with the live manager first, then the graveyard, as Android prints it. */
    private val LIVE_FIRST_SAMPLE = """
        Dump of SoftApManager id=1
        mCurrentSoftApConfiguration: ssid = "X" 
        getConnectedClientList().size(): 1
        mCurrentSoftApInfoMap {ap_br_wlan2=SoftApInfo{}}
         curState=StartedState
        Dump of ActiveModeWarden.Graveyard
        Dump of SoftApManager id=2
        mCurrentSoftApConfiguration: ssid = "X" 
        getConnectedClientList().size(): 0
        mCurrentSoftApInfoMap {}
         curState=<QUIT>
        10.94.183.5 dev ap_br_wlan2 lladdr aa:bb:cc:dd:ee:01 REACHABLE
        """.trimIndent()

    /** A dump of a device whose hotspot is off: only dead managers. */
    private val ALL_GRAVEYARD_SAMPLE = """
        Dump of ActiveModeWarden.Graveyard
        Dump of SoftApManager id=2
        mCurrentSoftApConfiguration: ssid = "X" 
        getConnectedClientList().size(): 0
        mCurrentSoftApInfoMap {}
         curState=<QUIT>
        Dump of SoftApManager id=3
        mCurrentSoftApConfiguration: ssid = "X" 
        getConnectedClientList().size(): 0
        mCurrentSoftApInfoMap {}
         curState=<QUIT>
        10.94.183.5 dev ap_br_wlan2 lladdr aa:bb:cc:dd:ee:01 REACHABLE
        """.trimIndent()

    /** A hypothetical dump with a dead manager before the live one, whose only neighbour row carries a flag. */
    private val GRAVEYARD_FIRST_SAMPLE = """
        Dump of SoftApManager id=2
        mCurrentSoftApConfiguration: ssid = "X" 
        getConnectedClientList().size(): 0
        mCurrentSoftApInfoMap {}
         curState=<QUIT>
        Dump of SoftApManager id=1
        mCurrentSoftApConfiguration: ssid = "X" 
        getConnectedClientList().size(): 1
        mCurrentSoftApInfoMap {ap_br_wlan2=SoftApInfo{}}
         curState=StartedState
        10.94.183.6 dev ap_br_wlan2 lladdr aa:bb:cc:dd:ee:02 router STALE
        """.trimIndent()
}
