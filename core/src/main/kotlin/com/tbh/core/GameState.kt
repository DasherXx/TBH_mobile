package com.tbh.core

data class GameState(
    val heroes: List<Hero>,
    val monster: Monster,
    val zone: Int = 1,
    val wave: Int = 1,
    val gold: Long = 0L,
    val inventory: List<Item> = emptyList(),
    val lastSeenTimestamp: Long = 0L,
    val rngSeed: Long = DEFAULT_SEED
) {
    companion object {
        private const val DEFAULT_SEED = 42L

        fun initial(rngSeed: Long = DEFAULT_SEED) = GameState(
            heroes = listOf(
                Hero(id = 1, heroClass = HeroClass.WARRIOR, hp = 120, maxHp = 120, attack = 15),
                Hero(id = 2, heroClass = HeroClass.MAGE,    hp = 80,  maxHp = 80,  attack = 22),
                Hero(id = 3, heroClass = HeroClass.ARCHER,  hp = 100, maxHp = 100, attack = 12)
            ),
            monster = Monster(name = "Goblin", hp = 80, maxHp = 80, attack = 9),
            rngSeed = rngSeed
        )
    }
}
