package com.tbh.mobile.battle

import androidx.annotation.ColorInt

data class Fighter(
    val name: String,
    val hp: Int,
    val maxHp: Int,
    @ColorInt val color: Int,
    val damage: Int
)
