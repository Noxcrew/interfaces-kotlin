package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.grid.mapping.ContainerGridMapper.Companion.COLUMNS_IN_CHEST
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/** Creates a new bukkit inventory. */
public fun createBukkitInventory(holder: InventoryHolder, rows: Int, title: Component?): Inventory {
    if (title == null) {
        return Bukkit.createInventory(holder, rows * COLUMNS_IN_CHEST)
    }

    return Bukkit.createInventory(holder, rows * COLUMNS_IN_CHEST, title)
}
