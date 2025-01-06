package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.utilities.createBukkitInventory
import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex
import com.noxcrew.interfaces.view.AbstractInterfaceView.Companion.COLUMNS_IN_CHEST
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
) : CachedInterfacesInventory() {

    private val chestSlots = rows * COLUMNS_IN_CHEST
    private val playerInventory = player.inventory

    /** The [chestInventory] being used to place items in. */
    public val chestInventory: Inventory = createBukkitInventory(holder, rows, title)

    override fun get(row: Int, column: Int): ItemStack? {
        val bukkitIndex = gridPointToBukkitIndex(row, column)

        if (row >= rows) {
            val adjustedIndex = bukkitIndex - chestSlots
            return playerInventory.getItem(adjustedIndex)
        }

        return chestInventory.getItem(bukkitIndex)
    }

    override fun setInternal(row: Int, column: Int, item: ItemStack?) {
        val bukkitIndex = gridPointToBukkitIndex(row, column)

        if (row >= rows) {
            val adjustedIndex = bukkitIndex - chestSlots
            playerInventory.setItem(adjustedIndex, item)
            return
        }

        chestInventory.setItem(bukkitIndex, item)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = row >= rows
}
