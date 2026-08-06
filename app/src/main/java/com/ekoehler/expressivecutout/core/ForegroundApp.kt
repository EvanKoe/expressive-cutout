package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The package name of the app currently in the foreground, kept up to date by the accessibility
 * service from window-state-changed events and read by the overlay. Only the package name is
 * surfaced — never any window content — so it stays within the service's
 * `canRetrieveWindowContent="false"` contract. Null until the first window change is observed.
 */
object ForegroundAppBus {

    private val _packageName = MutableStateFlow<String?>(null)
    val packageName: StateFlow<String?> = _packageName.asStateFlow()

    fun update(packageName: String?) {
        _packageName.value = packageName
    }
}
