package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.utilities.createBukkitInventory
import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex
import net.kyori.adventure.text.Component
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/** A wrapper around an [Inventory] for a chest. */
public class ChestInterfacesInventory(
    holder: InventoryHolder,
    title: Component?,
    rows: Int
) : CachedInterfacesInventory() {

    /** The [chestInventory] being used to place items in. */
    public val chestInventory: Inventory = createBukkitInventory(holder, rows, title)

    override fun get(row: Int, column: Int): ItemStack? {
        val index = gridPointToBukkitIndex(row, column)
        return chestInventory.getItem(index)
    }

    override fun setInternal(row: Int, column: Int, item: ItemStack?) {
        val index = gridPointToBukkitIndex(row, column)
        chestInventory.setItem(index, item)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = false
}
