package com.tbh.mobile.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sygnał na poziomie procesu: czy pełnoekranowe menu jest na wierzchu.
 * MenuActivity ustawia to w onStart/onStop, a OverlayService obserwuje, żeby schować/pokazać
 * nakładkę (okno TYPE_APPLICATION_OVERLAY renderuje się nad menu).
 */
object MenuVisibility {
    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    fun setOpen(open: Boolean) { _isOpen.value = open }
}
