package com.tbh.mobile.battle

import android.graphics.Color
import kotlin.math.roundToInt

object BattleEngine {

    fun tick(state: BattleState): BattleState {
        if (state.heroes.all { it.hp <= 0 }) return BattleState.initial()

        val heroes = state.heroes.toMutableList()
        var monster = state.monster

        // Alive heroes deal combined damage to monster
        if (monster.hp > 0) {
            val dmg = heroes.sumOf { if (it.hp > 0) it.damage else 0 }
            monster = monster.copy(hp = (monster.hp - dmg).coerceAtLeast(0))
        }

        // Monster hits a random alive hero
        if (monster.hp > 0) {
            val alive = heroes.indices.filter { heroes[it].hp > 0 }
            if (alive.isNotEmpty()) {
                val t = alive.random()
                heroes[t] = heroes[t].copy(hp = (heroes[t].hp - monster.damage).coerceAtLeast(0))
            }
        }

        // Monster defeated → advance wave, heal party
        if (monster.hp <= 0) {
            val nextWave = state.wave + 1
            val newMaxHp = (state.monster.maxHp * 1.35).roundToInt().coerceAtMost(9999)
            val newMonster = state.monster.copy(
                name = monsterName(nextWave),
                hp = newMaxHp,
                maxHp = newMaxHp,
                color = monsterColor(nextWave),
                damage = (state.monster.damage * 1.2).roundToInt()
            )
            return BattleState(heroes.map { it.copy(hp = it.maxHp) }, newMonster, nextWave)
        }

        return BattleState(heroes, monster, state.wave)
    }

    private fun monsterName(wave: Int) = when {
        wave <= 3  -> "Goblin"
        wave <= 6  -> "Ork"
        wave <= 10 -> "Troll"
        wave <= 15 -> "Ogr"
        else       -> "Smok"
    }

    private fun monsterColor(wave: Int) = when {
        wave <= 3  -> Color.parseColor("#EF9A9A")  // jasny czerwony
        wave <= 6  -> Color.parseColor("#FFCC80")  // pomarańczowy
        wave <= 10 -> Color.parseColor("#80CBC4")  // morski
        wave <= 15 -> Color.parseColor("#B0BEC5")  // niebieskoszary
        else       -> Color.parseColor("#FF8A65")  // ognisty
    }
}
