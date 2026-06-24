package com.tbh.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    // ── Determinizm ────────────────────────────────────────────────────────────

    @Test
    fun `same initial state always produces identical result`() {
        val state = GameState.initial()
        val r1 = GameEngine.tick(state, 20)
        val r2 = GameEngine.tick(state, 20)
        assertEquals(r1, r2)
    }

    @Test
    fun `tick(state, n) equals applying tick(state,1) n times`() {
        val state = GameState.initial()
        var manual = state
        repeat(15) { manual = GameEngine.tick(manual, 1) }
        assertEquals(manual, GameEngine.tick(state, 15))
    }

    // ── Pokonanie potwora ──────────────────────────────────────────────────────

    @Test
    fun `defeating monster advances wave`() {
        val state = oneHitKillState(wave = 1)
        val result = GameEngine.tick(state, 1)
        assertEquals(2, result.wave)
    }

    @Test
    fun `defeating monster scales monster maxHp by 1_35`() {
        val monsterMaxHp = 100
        val state = GameState(
            heroes  = listOf(Hero(1, HeroClass.WARRIOR, 100, 100, attack = 99_999)),
            monster = Monster("Goblin", hp = 1, maxHp = monsterMaxHp, attack = 0),
            rngSeed = 42L
        )
        val result = GameEngine.tick(state, 1)
        assertEquals((monsterMaxHp * 1.35).toInt(), result.monster.maxHp)
    }

    @Test
    fun `defeating monster heals party to full hp`() {
        val state = GameState(
            heroes  = listOf(Hero(1, HeroClass.WARRIOR, hp = 30, maxHp = 100, attack = 99_999)),
            monster = Monster("Goblin", hp = 1, maxHp = 100, attack = 0),
            rngSeed = 42L
        )
        val result = GameEngine.tick(state, 1)
        assertEquals(100, result.heroes[0].hp)
    }

    @Test
    fun `monster name changes after wave 5`() {
        // Bohaterowie z atakiem 99999 → potwór umiera co tick → 1 tick = 1 fala
        var state = GameState.initial().copy(
            heroes = GameState.initial().heroes.map { it.copy(attack = 99_999) }
        )
        // Po 5 tickach jesteśmy na fali 6 (fale 1-5 = Goblin, fala 6+ = Ork)
        repeat(5) { state = GameEngine.tick(state, 1) }
        assertEquals(6, state.wave)
        assertEquals("Ork", state.monster.name)
    }

    // ── Drużyna pokonana ───────────────────────────────────────────────────────

    @Test
    fun `already dead party resets to wave 1 on next tick`() {
        val state = GameState.initial().copy(
            heroes = GameState.initial().heroes.map { it.copy(hp = 0) },
            wave   = 7,
            zone   = 1
        )
        val result = GameEngine.tick(state, 1)
        assertEquals(1, result.wave)
        assertTrue(result.heroes.all { it.hp > 0 })
    }

    @Test
    fun `reset restores full hero hp`() {
        val dead = GameState.initial().let { s ->
            s.copy(heroes = s.heroes.map { it.copy(hp = 0) })
        }
        val reset = GameEngine.tick(dead, 1)
        reset.heroes.forEach { hero ->
            assertEquals(hero.maxHp, hero.hp)
        }
    }

    // ── Obrażenia od potwora ───────────────────────────────────────────────────

    @Test
    fun `monster damages the targeted hero each tick`() {
        // 1 bohater, atak=0 (nie zabija potwora), potwór atak=10
        val state = GameState(
            heroes  = listOf(Hero(1, HeroClass.WARRIOR, hp = 100, maxHp = 100, attack = 0)),
            monster = Monster("Tank", hp = 999_999, maxHp = 999_999, attack = 10),
            rngSeed = 42L
        )
        val result = GameEngine.tick(state, 1)
        // Z 1 bohaterem nextInt(1)=0 zawsze — bohater dostaje trafienie
        assertEquals(90, result.heroes[0].hp)
    }

    // ── Pomocnicze ─────────────────────────────────────────────────────────────

    private fun oneHitKillState(wave: Int = 1) = GameState(
        heroes  = listOf(Hero(1, HeroClass.WARRIOR, 100, 100, attack = 99_999)),
        monster = Monster("Dummy", hp = 1, maxHp = 100, attack = 0),
        wave    = wave,
        rngSeed = 42L
    )
}
