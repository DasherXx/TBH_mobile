package com.tbh.mobile.battle

import android.graphics.Color

data class BattleState(
    val heroes: List<Fighter>,
    val monster: Fighter,
    val wave: Int = 1
) {
    companion object {
        fun initial() = BattleState(
            heroes = listOf(
                Fighter("Wojownik", 120, 120, Color.parseColor("#4FC3F7"), damage = 15),
                Fighter("Mag",       80,  80, Color.parseColor("#CE93D8"), damage = 22),
                Fighter("Łucznik",  100, 100, Color.parseColor("#A5D6A7"), damage = 12)
            ),
            monster = Fighter("Goblin", 80, 80, Color.parseColor("#EF9A9A"), damage = 9)
        )
    }
}
