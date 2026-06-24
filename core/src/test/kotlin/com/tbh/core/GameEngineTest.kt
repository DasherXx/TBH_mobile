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

    @Test
    fun `reset returns equipped items to inventory instead of losing them`() {
        val sword = Item(1, "Miecz", Rarity.COMMON, attackBonus = 5)
        val armor = Item(2, "Zbroja", Rarity.RARE, hpBonus = 40)
        val dead = GameState.initial().let { s ->
            s.copy(
                heroes = s.heroes.mapIndexed { i, h ->
                    when (i) {
                        0 -> h.copy(hp = 0, equippedItem = sword)
                        1 -> h.copy(hp = 0, equippedItem = armor)
                        else -> h.copy(hp = 0)
                    }
                },
                inventory = emptyList()
            )
        }
        val reset = GameEngine.tick(dead, 1)

        assertEquals(1, reset.wave)
        assertTrue("miecz wraca do inventory", reset.inventory.contains(sword))
        assertTrue("zbroja wraca do inventory", reset.inventory.contains(armor))
        assertTrue("świeży bohaterowie bez ekwipunku", reset.heroes.all { it.equippedItem == null })
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

    // ── Loot: złoto ───────────────────────────────────────────────────────────

    @Test
    fun `defeating monster increases gold`() {
        val state = oneHitKillState()
        val result = GameEngine.tick(state, 1)
        assertTrue("Gold should increase after kill", result.gold > 0L)
    }

    // ── Loot: XP ──────────────────────────────────────────────────────────────

    @Test
    fun `defeating monster grants XP to alive heroes`() {
        val state = oneHitKillState()
        val result = GameEngine.tick(state, 1)
        val hero = result.heroes[0]
        // Bohater dostał XP (lub już awansował poziom)
        assertTrue("Alive hero should gain XP or level up", hero.xp > 0 || hero.level > 1)
    }

    @Test
    fun `dead hero does not gain XP after kill`() {
        val state = GameState(
            heroes = listOf(
                Hero(1, HeroClass.WARRIOR, hp = 100, maxHp = 100, attack = 99_999),
                Hero(2, HeroClass.MAGE,    hp = 0,   maxHp = 80,  attack = 0)       // martwy
            ),
            monster = Monster("Dummy", hp = 1, maxHp = 100, attack = 0),
            rngSeed = 42L
        )
        val result = GameEngine.tick(state, 1)
        assertEquals(0, result.heroes[1].xp)
        assertEquals(1, result.heroes[1].level)
    }

    // ── Levelowanie ───────────────────────────────────────────────────────────

    @Test
    fun `hero levels up when XP threshold is reached`() {
        // Próg dla poziom 1 = 100*1 = 100 XP
        // xpForKill(wave=1, zone=1) = 30+5+10 = 45
        // Startujemy z xp=90 → 90+45=135 ≥ 100 → awans
        val state = GameState(
            heroes  = listOf(Hero(1, HeroClass.WARRIOR, 100, 100, attack = 99_999, xp = 90, level = 1)),
            monster = Monster("Dummy", hp = 1, maxHp = 100, attack = 0),
            rngSeed = 42L
        )
        val result = GameEngine.tick(state, 1)
        val hero = result.heroes[0]
        assertEquals(2, hero.level)
        assertTrue("attack should increase on level up", hero.attack > 100)
        assertTrue("maxHp should increase on level up", hero.maxHp > 100)
    }

    @Test
    fun `XP overflow is preserved after level up`() {
        // xpForKill(wave=1, zone=1) = 45
        // xp=90 → 90+45=135 → awans (próg=100), nadwyżka = 35
        val state = GameState(
            heroes  = listOf(Hero(1, HeroClass.WARRIOR, 100, 100, attack = 99_999, xp = 90, level = 1)),
            monster = Monster("Dummy", hp = 1, maxHp = 100, attack = 0),
            rngSeed = 42L
        )
        val result = GameEngine.tick(state, 1)
        val xpGain = GameEngine.xpForKill(wave = 1, zone = 1)   // = 45
        val expectedOverflow = (90 + xpGain) - 100              // 135 - 100 = 35
        assertEquals(expectedOverflow, result.heroes[0].xp)
    }

    // ── Ekwipunek ─────────────────────────────────────────────────────────────

    @Test
    fun `equipped item with attackBonus increases damage dealt`() {
        val item = Item(id = 99, name = "Test Sword", rarity = Rarity.COMMON, attackBonus = 50)
        val baseHero     = Hero(1, HeroClass.WARRIOR, hp = 100, maxHp = 100, attack = 20)
        val equippedHero = baseHero.copy(equippedItem = item)

        // Potwór z dużą ilością HP (żaden nie ginie w jednym ticku)
        val baseState = GameState(
            heroes  = listOf(baseHero),
            monster = Monster("Dummy", hp = 999, maxHp = 999, attack = 0),
            rngSeed = 42L
        )
        val equippedState = baseState.copy(heroes = listOf(equippedHero))

        val baseResult     = GameEngine.tick(baseState, 1)
        val equippedResult = GameEngine.tick(equippedState, 1)

        // Bohater bez przedmiotu: 999 - 20 = 979 HP potworowi
        assertEquals(979, baseResult.monster.hp)
        // Bohater z mieczem: 999 - (20+50) = 929 HP potworowi
        assertEquals(929, equippedResult.monster.hp)
    }

    // ── Pomocnicze ─────────────────────────────────────────────────────────────

    private fun oneHitKillState(wave: Int = 1) = GameState(
        heroes  = listOf(Hero(1, HeroClass.WARRIOR, 100, 100, attack = 99_999)),
        monster = Monster("Dummy", hp = 1, maxHp = 100, attack = 0),
        wave    = wave,
        rngSeed = 42L
    )
}
