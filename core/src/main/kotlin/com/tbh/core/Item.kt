package com.tbh.core

data class Item(
    val id: Int,
    val name: String,
    val rarity: Rarity,
    val attackBonus: Int = 0,
    val hpBonus: Int = 0
)
