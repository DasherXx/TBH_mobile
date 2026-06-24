package com.tbh.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentTest {

    private val sword = Item(1, "Miecz", Rarity.COMMON, attackBonus = 5)
    private val armor = Item(2, "Zbroja", Rarity.RARE, hpBonus = 40)

    private fun stateWith(inventory: List<Item>, heroEquipped: Item? = null): GameState {
        val base = GameState.initial()
        val hero = base.heroes[0].copy(equippedItem = heroEquipped)
        return base.copy(
            heroes = listOf(hero) + base.heroes.drop(1),
            inventory = inventory
        )
    }

    @Test
    fun `equipping sets item and removes it from inventory`() {
        val state = stateWith(listOf(sword, armor))
        val result = Equipment.equip(state, heroId = 1, inventoryIndex = 0)

        assertEquals(sword, result.heroes[0].equippedItem)
        assertEquals(listOf(armor), result.inventory)         // miecz zdjęty z puli
    }

    @Test
    fun `swapping returns old item to inventory and keeps inventory size`() {
        val state = stateWith(listOf(armor), heroEquipped = sword)
        val result = Equipment.equip(state, heroId = 1, inventoryIndex = 0)

        assertEquals(armor, result.heroes[0].equippedItem)
        assertEquals(1, result.inventory.size)                // 1 wchodzi, 1 wychodzi
        assertTrue("stary przedmiot wraca do inventory", result.inventory.contains(sword))
    }

    @Test
    fun `equipping increases effective combat stats`() {
        val state = stateWith(listOf(sword))
        val baseAttack = state.heroes[0].effectiveAttack
        val result = Equipment.equip(state, heroId = 1, inventoryIndex = 0)
        assertEquals(baseAttack + sword.attackBonus, result.heroes[0].effectiveAttack)
    }

    @Test
    fun `invalid inventory index leaves state unchanged`() {
        val state = stateWith(listOf(sword))
        assertEquals(state, Equipment.equip(state, heroId = 1, inventoryIndex = 5))
        assertEquals(state, Equipment.equip(state, heroId = 1, inventoryIndex = -1))
    }

    @Test
    fun `unknown hero id leaves state unchanged`() {
        val state = stateWith(listOf(sword))
        assertEquals(state, Equipment.equip(state, heroId = 999, inventoryIndex = 0))
    }

    @Test
    fun `unequipping is not possible here but empty slot starts null`() {
        val state = stateWith(listOf(sword))
        assertNull(state.heroes[0].equippedItem)
    }
}
