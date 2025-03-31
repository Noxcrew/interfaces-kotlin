package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.grid.mapping.ChestGridMapper
import com.noxcrew.interfaces.utilities.createBukkitInventory
import net.kyori.adventure.text.Component
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/** A wrapper around an [Inventory] for a chest. */
public class ChestInterfacesInventory(
    holder: InventoryHolder,
    title: Component?,
    rows: Int,
    private val mapper: ChestGridMapper,
) : CachedInterfacesInventory() {

    /** The [chestInventory] being used to place items in. */
    public val chestInventory: Inventory = createBukkitInventory(holder, rows, title)

    override fun get(row: Int, column: Int): ItemStack? {
        return chestInventory.getItem(mapper.toTopInventorySlot(row, column))
    }

    override fun setInternal(row: Int, column: Int, item: ItemStack?) {
        chestInventory.setItem(mapper.toTopInventorySlot(row, column), item)
    }
}
