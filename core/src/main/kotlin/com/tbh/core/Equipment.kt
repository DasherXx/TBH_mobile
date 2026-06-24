package com.tbh.core

/**
 * Czysta logika zakładania ekwipunku (jeden slot na bohatera).
 * Brak zależności od Androida — testowalne w JUnit.
 */
object Equipment {

    /**
     * Zakłada przedmiot z [inventoryIndex] na bohatera o danym [heroId].
     * Stary equippedItem (jeśli był) wraca do inventory.
     * Niewłaściwy indeks lub nieznany bohater → stan zwracany bez zmian.
     */
    fun equip(state: GameState, heroId: Int, inventoryIndex: Int): GameState {
        if (inventoryIndex !in state.inventory.indices) return state
        val heroIdx = state.heroes.indexOfFirst { it.id == heroId }
        if (heroIdx < 0) return state

        val item = state.inventory[inventoryIndex]
        val hero = state.heroes[heroIdx]

        // Inventory bez zakładanego przedmiotu; stary ekwipunek wraca do puli.
        val newInventory = state.inventory.toMutableList().apply {
            removeAt(inventoryIndex)
            hero.equippedItem?.let { add(it) }
        }

        val newHeroes = state.heroes.toMutableList().apply {
            this[heroIdx] = hero.copy(equippedItem = item)
        }

        return state.copy(heroes = newHeroes, inventory = newInventory)
    }
}
