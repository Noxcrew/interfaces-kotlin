package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/** A wrapper around a [PlayerInventory]. */
public class PlayerInterfacesInventory(
    player: Player,
) : CachedInterfacesInventory() {
    private val playerInventory = player.inventory

    override fun get(
        row: Int,
        column: Int,
    ): ItemStack? {
        val index = gridPointToBukkitIndex(row, column)
        return playerInventory.getItem(index)
    }

    override fun setInternal(
        row: Int,
        column: Int,
        item: ItemStack?,
    ) {
        val index = gridPointToBukkitIndex(row, column)
        return playerInventory.setItem(index, item)
    }

    override fun isPlayerInventory(
        row: Int,
        column: Int,
    ): Boolean = true
}
