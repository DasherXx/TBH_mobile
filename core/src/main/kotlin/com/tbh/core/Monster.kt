package com.tbh.core

import kotlinx.serialization.Serializable

@Serializable
data class Monster(
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val attack: Int
)
