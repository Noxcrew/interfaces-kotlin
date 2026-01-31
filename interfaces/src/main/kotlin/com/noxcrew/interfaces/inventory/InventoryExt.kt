package com.noxcrew.interfaces.inventory

import org.bukkit.craftbukkit.entity.CraftHumanEntity
import org.bukkit.craftbukkit.inventory.CraftInventory
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

/** Fast version of [clear] that doesn't unnecessarily clear already empty slots. */
public fun Inventory.fastClear() {
    val handle = (this as CraftInventory).inventory
    for (i in 0 until handle.containerSize) {
        if (handle.getItem(i).isEmpty) continue
        handle.setItem(i, net.minecraft.world.item.ItemStack.EMPTY)
    }
}

/**
 * Removes all items from the player's extended inventory, including the crafting grid and the item
 * currently on their cursor.
 *
 * Optimized to only remove items instead of setting empty slots to empty again.
 */
public fun HumanEntity.clearInventory() {
    // clear the player's own inventory
    inventory.fastClear()

    // clear the inventory crafting grid
    if (openInventory.topInventory.type == InventoryType.CRAFTING) {
        openInventory.topInventory.fastClear()
    }

    // also clear the cursor item only if we're currently carrying one
    if (!(this as CraftHumanEntity).handle.containerMenu.carried.isEmpty) {
        openInventory.setCursor(null)
    }
}
