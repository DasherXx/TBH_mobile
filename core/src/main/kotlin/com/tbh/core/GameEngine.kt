package com.tbh.core

/**
 * Czysta, deterministyczna logika gry.
 * tick(state, n) == n-krotne zastosowanie tick(state, 1) — dzięki temu:
 *   - ten sam stan + ten sam seed zawsze daje ten sam wynik,
 *   - postęp offline = przewinięcie symulacji przez miniony czas.
 */
object GameEngine {

    private const val WAVES_PER_ZONE = 10

    fun tick(state: GameState, ticks: Int = 1): GameState {
        var current = state
        repeat(ticks) { current = singleTick(current) }
        return current
    }

    private fun singleTick(state: GameState): GameState {
        val rng = LcgRng(state.rngSeed)
        // Zawsze wykonaj jeden krok RNG — seed musi się zmieniać niezależnie od
        // stanu gry, żeby każdy tick był deterministycznie różny.
        val monsterTargetIdx = rng.nextInt(state.heroes.size.coerceAtLeast(1))
        val nextSeed = rng.state

        // Cała drużyna martwa → reset do fali 1
        if (state.heroes.all { it.hp <= 0 }) {
            return GameState.initial(rngSeed = nextSeed)
        }

        val heroes = state.heroes.toMutableList()
        var monster = state.monster

        // Wszyscy żywi bohaterowie atakują potwora jednocześnie
        val totalDmg = heroes.sumOf { if (it.hp > 0) it.attack else 0 }
        monster = monster.copy(hp = (monster.hp - totalDmg).coerceAtLeast(0))

        // Potwór atakuje bohatera o wylosowanym indeksie (jeśli żywy i sam żyje)
        if (monster.hp > 0 && heroes[monsterTargetIdx].hp > 0) {
            heroes[monsterTargetIdx] = heroes[monsterTargetIdx].copy(
                hp = (heroes[monsterTargetIdx].hp - monster.attack).coerceAtLeast(0)
            )
        }

        // Potwór pokonany → kolejna fala z silniejszym wrogiem, drużyna się leczy
        if (monster.hp <= 0) {
            return advanceWave(state, heroes, nextSeed)
        }

        return state.copy(heroes = heroes, monster = monster, rngSeed = nextSeed)
    }

    private fun advanceWave(state: GameState, heroes: List<Hero>, nextSeed: Long): GameState {
        val newWave = state.wave + 1
        val newZone = (newWave - 1) / WAVES_PER_ZONE + 1
        val newMaxHp = (state.monster.maxHp * 1.35).toInt().coerceAtMost(999_999)
        val newMonster = Monster(
            name = monsterName(newWave, newZone),
            hp = newMaxHp,
            maxHp = newMaxHp,
            attack = (state.monster.attack * 1.2).toInt().coerceAtMost(9_999)
        )
        return state.copy(
            heroes = heroes.map { it.copy(hp = it.maxHp) },
            monster = newMonster,
            wave = newWave,
            zone = newZone,
            rngSeed = nextSeed
        )
    }

    private fun monsterName(wave: Int, zone: Int) = when {
        zone == 1 -> when { wave <= 5 -> "Goblin"; wave <= 10 -> "Ork"; else -> "Troll" }
        zone == 2 -> when { wave <= 15 -> "Ogr"; else -> "Ogr Boss" }
        zone == 3 -> when { wave <= 25 -> "Troll"; else -> "Troll Szaman" }
        else      -> "Smok (Zona $zone)"
    }
}
