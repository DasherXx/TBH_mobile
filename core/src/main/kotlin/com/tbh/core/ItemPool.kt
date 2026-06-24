package com.tbh.core

/**
 * Statyczna pula przedmiotów MVP.
 * Nowe przedmioty dodajemy tutaj — GameEngine wybiera przez itemIdx z RNG.
 */
object ItemPool {
    val all: List<Item> = listOf(
        // COMMON
        Item(1,  "Drewniany Miecz",  Rarity.COMMON, attackBonus = 5),
        Item(2,  "Skórzana Zbroja",  Rarity.COMMON, hpBonus = 20),
        Item(3,  "Prosty Łuk",       Rarity.COMMON, attackBonus = 4, hpBonus = 5),
        // RARE
        Item(4,  "Stalowy Miecz",    Rarity.RARE,   attackBonus = 12),
        Item(5,  "Kolczuga",         Rarity.RARE,   hpBonus = 40),
        Item(6,  "Elficka Różdżka",  Rarity.RARE,   attackBonus = 15, hpBonus = 10),
        // EPIC
        Item(7,  "Miecz Smoka",      Rarity.EPIC,   attackBonus = 30),
        Item(8,  "Zbroja Rycerza",   Rarity.EPIC,   hpBonus = 80),
        Item(9,  "Klejnot Mocy",     Rarity.EPIC,   attackBonus = 20, hpBonus = 30)
    )
}
