package com.tbh.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Krok6Test {

    private val tickMs = 1500L

    // ── Serializacja round-trip ─────────────────────────────────────────────────

    @Test
    fun `serialization round-trip preserves full GameState`() {
        val item = Item(7, "Miecz Smoka", Rarity.EPIC, attackBonus = 30)
        val state = GameState.initial().copy(
            heroes = GameState.initial().heroes.mapIndexed { i, h ->
                if (i == 0) h.copy(level = 4, xp = 37, equippedItem = item) else h
            },
            wave = 12,
            zone = 2,
            gold = 1234L,
            inventory = listOf(
                Item(1, "Drewniany Miecz", Rarity.COMMON, attackBonus = 5),
                item
            ),
            lastSeenTimestamp = 1_700_000_000_000L,
            rngSeed = 99L
        )

        val restored = GameSerializer.fromJson(GameSerializer.toJson(state))
        assertEquals(state, restored)
    }

    @Test
    fun `serialization round-trip on a ticked state`() {
        val state = GameEngine.tick(GameState.initial(), 25)
        val restored = GameSerializer.fromJson(GameSerializer.toJson(state))
        assertEquals(state, restored)
    }

    // ── Zgodność offline z determinizmem (Krok 4) ───────────────────────────────

    @Test
    fun `offline progress equals manual repeated single ticks`() {
        val n = 30
        val state = GameState.initial().copy(lastSeenTimestamp = 0L)

        // Naliczenie offline: now = n * tickMs → dokładnie n ticków
        val offline = OfflineProgress.apply(state, nowMillis = n * tickMs, tickMs = tickMs)

        var manual = state
        repeat(n) { manual = GameEngine.tick(manual, 1) }

        assertEquals(n, offline.ticksSimulated)
        assertEquals(manual, offline.state)
    }

    @Test
    fun `offline tick count is elapsed divided by tick interval`() {
        val state = GameState.initial().copy(lastSeenTimestamp = 1_000L)
        // 10 pełnych ticków + reszta (która się nie liczy)
        val now = 1_000L + 10 * tickMs + 700L
        val offline = OfflineProgress.apply(state, nowMillis = now, tickMs = tickMs)
        assertEquals(10, offline.ticksSimulated)
    }

    // ── Limit maksymalnego czasu offline ────────────────────────────────────────

    @Test
    fun `offline progress is capped at max offline time`() {
        val state = GameState.initial().copy(lastSeenTimestamp = 0L)
        val maxTicks = (OfflineProgress.MAX_OFFLINE_MS / tickMs).toInt()

        // 30 dni nieobecności — znacznie ponad limit 2h
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val offline = OfflineProgress.apply(state, nowMillis = thirtyDaysMs, tickMs = tickMs)

        assertEquals(maxTicks, offline.ticksSimulated)
        // Wynik identyczny jak ograniczone tick(state, maxTicks)
        assertEquals(GameEngine.tick(state, maxTicks), offline.state)
    }

    @Test
    fun `negative or zero elapsed produces no progress`() {
        val state = GameEngine.tick(GameState.initial(), 5).copy(lastSeenTimestamp = 5_000L)
        // now < lastSeenTimestamp (np. zmiana zegara) → 0 ticków, stan bez zmian
        val offline = OfflineProgress.apply(state, nowMillis = 4_000L, tickMs = tickMs)
        assertEquals(0, offline.ticksSimulated)
        assertEquals(state, offline.state)
    }

    // ── Podsumowanie (diff stanu) ───────────────────────────────────────────────

    @Test
    fun `offline summary matches state diff`() {
        val state = GameState.initial().copy(lastSeenTimestamp = 0L)
        val offline = OfflineProgress.apply(state, nowMillis = 60 * tickMs, tickMs = tickMs)

        assertEquals(offline.state.gold - state.gold, offline.goldGained)
        assertEquals(offline.state.wave - state.wave, offline.monstersDefeated)
        assertTrue("Po 60 tickach powinien być jakiś postęp", offline.goldGained > 0)
    }
}
