package com.tbh.mobile.overlay

import com.tbh.core.HeroClass
import com.tbh.core.Rarity

internal fun HeroClass.displayName() = when (this) {
    HeroClass.WARRIOR -> "Wojownik"
    HeroClass.MAGE    -> "Mag"
    HeroClass.ARCHER  -> "Łucznik"
}

internal fun Rarity.displayName() = when (this) {
    Rarity.COMMON -> "Zwykły"
    Rarity.RARE   -> "Rzadki"
    Rarity.EPIC   -> "Epicki"
}
