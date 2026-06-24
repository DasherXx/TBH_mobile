package com.tbh.core

import kotlinx.serialization.Serializable

@Serializable
data class Hero(
    val id: Int,
    val heroClass: HeroClass,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val level: Int = 1,
    val xp: Int = 0,
    val equippedItem: Item? = null
) {
    /** Efektywny atak w walce = bazowy + bonus z ekwipunku. */
    val effectiveAttack: Int get() = attack + (equippedItem?.attackBonus ?: 0)

    /** Efektywne maksymalne HP = bazowe + bonus z ekwipunku. */
    val effectiveMaxHp: Int get() = maxHp + (equippedItem?.hpBonus ?: 0)
}
