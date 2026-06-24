package com.tbh.core

/**
 * Wynik naliczenia postępu offline.
 * goldGained / monstersDefeated liczone z różnicy stanu przed/po — bez modyfikacji tick().
 */
data class OfflineResult(
    val state: GameState,
    val ticksSimulated: Int,
    val goldGained: Long,
    val monstersDefeated: Int
)

/**
 * Czyste naliczenie „umiarkowanego" postępu offline.
 * nowMillis i tickMs jako parametry — :core nie sięga po System.currentTimeMillis.
 */
object OfflineProgress {

    /** Limit „umiarkowanego" postępu offline: 2h (z założeń gry). */
    const val MAX_OFFLINE_MS = 2L * 60 * 60 * 1000

    fun apply(
        state: GameState,
        nowMillis: Long,
        tickMs: Long,
        maxOfflineMs: Long = MAX_OFFLINE_MS
    ): OfflineResult {
        require(tickMs > 0) { "tickMs musi być dodatnie" }

        val elapsed = (nowMillis - state.lastSeenTimestamp).coerceAtLeast(0L)
        val capped = elapsed.coerceAtMost(maxOfflineMs)
        val ticks = (capped / tickMs).toInt()

        // Reużycie kompozytywności z Kroku 4: tick(state, n) == n-krotne tick(state, 1).
        val newState = GameEngine.tick(state, ticks)

        return OfflineResult(
            state = newState,
            ticksSimulated = ticks,
            goldGained = newState.gold - state.gold,
            monstersDefeated = newState.wave - state.wave   // każde zwycięstwo to wave + 1
        )
    }
}
