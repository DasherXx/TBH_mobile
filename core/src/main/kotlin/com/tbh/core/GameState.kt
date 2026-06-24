package com.tbh.core

/**
 * Placeholder — wypełniany w Kroku 4 (Rdzeń walki).
 * Cały rdzeń logiki żyje w module :core, zero importów Androida.
 */
data class GameState(
    val currentZone: Int = 1,
    val currentWave: Int = 1,
    val gold: Long = 0L,
    val lastSeenTimestamp: Long = 0L
)
