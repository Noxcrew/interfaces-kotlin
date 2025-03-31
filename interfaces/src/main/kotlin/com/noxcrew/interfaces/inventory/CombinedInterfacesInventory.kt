package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.grid.mapping.CombinedGridMapper
import com.noxcrew.interfaces.utilities.createBukkitInventory
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/** A wrapper around an [Inventory] for a chest and a [PlayerInventory]. */
public class CombinedInterfacesInventory(
    holder: InventoryHolder,
    player: Player,
    title: Component?,
    private val rows: Int,
    private val mapper: CombinedGridMapper,
) : CachedInterfacesInventory() {

    private val playerInventory = player.inventory

    /** The [chestInventory] being used to place items in. */
    public val chestInventory: Inventory = createBukkitInventory(holder, rows, title)

    override fun get(row: Int, column: Int): ItemStack? {
        if (mapper.isPlayerInventory(row, column)) {
            return playerInventory.getItem(mapper.toPlayerInventorySlot(row, column))
        }

        return chestInventory.getItem(mapper.toTopInventorySlot(row, column))
    }

    override fun setInternal(row: Int, column: Int, item: ItemStack?) {
        if (mapper.isPlayerInventory(row, column)) {
            playerInventory.setItem(mapper.toPlayerInventorySlot(row, column), item)
            return
        }

        chestInventory.setItem(mapper.toTopInventorySlot(row, column), item)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = row >= rows
}
