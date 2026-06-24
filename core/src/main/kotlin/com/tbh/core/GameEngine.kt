package com.tbh.core

import kotlin.math.pow

object GameEngine {

    private const val WAVES_PER_ZONE = 10
    private const val ITEM_DROP_CHANCE = 40   // procent szans na drop (itemRoll < 40 → drop)
    private const val MAX_INVENTORY = 20

    fun tick(state: GameState, ticks: Int = 1): GameState {
        var current = state
        repeat(ticks) { current = singleTick(current) }
        return current
    }

    /** XP za zabicie potwora. Publiczne dla testów. */
    fun xpForKill(wave: Int, zone: Int): Int = 20 + wave * 4 + zone * 8

    /**
     * Próg XP do awansu z danego poziomu — wykładniczy, by spowolnić progresję w późniejszej grze.
     * L1→2: 100, L2→3: 160, L3→4: 256, L5: ~655, L10: ~6800. Publiczne dla testów.
     */
    fun xpThreshold(level: Int): Int = (100 * 1.6.pow(level - 1)).toInt()

    /**
     * Mnożnik HP potwora przy przejściu do danej fali — łagodny start, agresywne późne fale.
     * Publiczne dla testów.
     */
    fun monsterHpScale(forWave: Int): Double = when {
        forWave < 10 -> 1.18
        forWave < 25 -> 1.30
        else         -> 1.45
    }

    private fun singleTick(state: GameState): GameState {
        val rng = LcgRng(state.rngSeed)
        // Zawsze 4 rzuty na tick → ścisły determinizm niezależnie od stanu gry
        val monsterTargetIdx = rng.nextInt(state.heroes.size.coerceAtLeast(1))
        val goldRoll         = rng.nextInt(state.zone * 5 + 5)
        val itemRoll         = rng.nextInt(100)
        val itemIdx          = rng.nextInt(ItemPool.all.size)
        val nextSeed         = rng.state

        // Cała drużyna martwa → reset. Złoto, rekord fali i ekwipunek zostają;
        // założone przedmioty wracają do inventory (świeży bohaterowie mają equippedItem = null).
        if (state.heroes.all { it.hp <= 0 }) {
            val reset = GameState.initial(rngSeed = nextSeed)
            val recoveredGear = state.heroes.mapNotNull { it.equippedItem }
            return reset.copy(
                gold = state.gold,
                inventory = state.inventory + recoveredGear,
                highestWave = state.highestWave
            )
        }

        val heroes = state.heroes.toMutableList()
        var monster = state.monster

        // Bohaterowie atakują potwora — efektywny atak = bazowy + bonus przedmiotu
        val totalDmg = heroes.sumOf { if (it.hp > 0) it.effectiveAttack else 0 }
        monster = monster.copy(hp = (monster.hp - totalDmg).coerceAtLeast(0))

        // Potwór atakuje losowego bohatera (jeśli żyje)
        if (monster.hp > 0 && heroes[monsterTargetIdx].hp > 0) {
            heroes[monsterTargetIdx] = heroes[monsterTargetIdx].copy(
                hp = (heroes[monsterTargetIdx].hp - monster.attack).coerceAtLeast(0)
            )
        }

        // Potwór pokonany → loot, XP, nowa fala
        if (monster.hp <= 0) {
            return advanceWave(state, heroes, nextSeed, goldRoll, itemRoll, itemIdx)
        }

        return state.copy(heroes = heroes, monster = monster, rngSeed = nextSeed)
    }

    private fun advanceWave(
        state: GameState,
        heroesAfterCombat: List<Hero>,
        nextSeed: Long,
        goldRoll: Int,
        itemRoll: Int,
        itemIdx: Int
    ): GameState {
        val newWave = state.wave + 1
        val newZone = (newWave - 1) / WAVES_PER_ZONE + 1

        // Złoto
        val newGold = state.gold + 5L + state.zone * 2 + goldRoll

        // Drop przedmiotu (deterministyczny — RNG wylosowano zawsze, niezależnie od wyniku)
        val droppedItem = if (itemRoll < ITEM_DROP_CHANCE && state.inventory.size < MAX_INVENTORY) {
            ItemPool.all[itemIdx]
        } else null
        val newInventory = if (droppedItem != null) state.inventory + droppedItem else state.inventory

        // XP dla żywych bohaterów + levelowanie
        val xpGain = xpForKill(state.wave, state.zone)
        val updatedHeroes = heroesAfterCombat.map { hero ->
            val withXp = if (hero.hp > 0) applyXp(hero, xpGain) else hero
            withXp.copy(hp = withXp.effectiveMaxHp)
        }

        // Kolejny potwór
        val newMaxHp = (state.monster.maxHp * monsterHpScale(newWave)).toInt().coerceAtMost(999_999)
        val newMonster = Monster(
            name = monsterName(newWave, newZone),
            hp = newMaxHp,
            maxHp = newMaxHp,
            attack = (state.monster.attack * 1.2).toInt().coerceAtMost(9_999)
        )

        return state.copy(
            heroes = updatedHeroes,
            monster = newMonster,
            wave = newWave,
            zone = newZone,
            gold = newGold,
            inventory = newInventory,
            highestWave = maxOf(state.highestWave, newWave),
            rngSeed = nextSeed
        )
    }

    private fun applyXp(hero: Hero, xpGain: Int): Hero {
        var h = hero.copy(xp = hero.xp + xpGain)
        while (true) {
            val threshold = xpThreshold(h.level)
            if (h.xp < threshold) break
            h = h.copy(
                level  = h.level + 1,
                xp     = h.xp - threshold,
                maxHp  = (h.maxHp * 1.10).toInt().coerceAtLeast(h.maxHp + 1),
                attack = (h.attack * 1.10).toInt().coerceAtLeast(h.attack + 1)
            )
        }
        return h
    }

    private fun monsterName(wave: Int, zone: Int) = when {
        zone == 1 -> when { wave <= 5 -> "Goblin"; wave <= 10 -> "Ork"; else -> "Troll" }
        zone == 2 -> when { wave <= 15 -> "Ogr"; else -> "Ogr Boss" }
        zone == 3 -> when { wave <= 25 -> "Troll"; else -> "Troll Szaman" }
        else      -> "Smok (Zona $zone)"
    }
}
