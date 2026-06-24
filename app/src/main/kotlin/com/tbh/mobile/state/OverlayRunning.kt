package com.tbh.mobile.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reaktywny, procesowy sygnał: czy OverlayService aktualnie działa.
 * OverlayService ustawia go w onCreate/onDestroy; MainActivity obserwuje przez collectAsState,
 * dzięki czemu UI odzwierciedla rzeczywisty stan na żywo — także gdy nakładka zostanie
 * zatrzymana z powiadomienia, podczas gdy MainActivity pozostaje na wierzchu (brak „ducha").
 */
object OverlayRunning {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun set(running: Boolean) { _isRunning.value = running }
}
